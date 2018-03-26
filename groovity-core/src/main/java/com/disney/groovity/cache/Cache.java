/*******************************************************************************
 * © 2018 Disney | ABC Television Group
 *
 * Licensed under the Apache License, Version 2.0 (the "Apache License")
 * with the following modification; you may not use this file except in
 * compliance with the Apache License and the following modification to it:
 * Section 6. Trademarks. is deleted and replaced with:
 *
 * 6. Trademarks. This License does not grant permission to use the trade
 *     names, trademarks, service marks, or product names of the Licensor
 *     and its affiliates, except as required to comply with Section 4(c) of
 *     the License and to reproduce the content of the NOTICE file.
 *
 * You may obtain a copy of the Apache License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License with the above modification is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the Apache License for the specific
 * language governing permissions and limitations under the Apache License.
 *******************************************************************************/
package com.disney.groovity.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.disney.groovity.BindingDecorator;
import com.disney.groovity.util.ScriptHelper;

import groovy.lang.Binding;
import groovy.lang.Closure;
/**
 * Represents an individual cache as declared in a groovity script; provides bulk retrieval methods, single object put/remove,
 * and handles background batch refreshing of stale entries.  Relies on a CacheLoader closure to perform the loading logic and
 * a ValueStore for managing CacheValues.
 *
 * @author Alex Vigdor
 */
public class Cache {
	public static final String MAP = "map";
	private static Logger log = Logger.getLogger(Cache.class.getName());
	private CacheValueStore valueStore;
	private ScriptHelper scriptHelper;
	private BlockingQueue<Object> refreshQueue = new LinkedBlockingQueue<>();
	private ScheduledFuture<?> refreshFuture;
	private int batchSize = 100;
	@SuppressWarnings("rawtypes")
	private Closure cacheLoader;
	private BindingDecorator bindingDecorator;
	private CacheManager manager;
	private ScheduledExecutorService scheduler;
	private ConcurrentHashMap<Object, CompletableFuture<CacheValue>> syncLoadMap = new ConcurrentHashMap<>();
	
	public void init(){
		manager = new CacheManager(this);
		valueStore.init();
		refreshFuture = scheduler.scheduleWithFixedDelay(new Runnable() {
			public void run() {
				try{
					if(!refreshQueue.isEmpty()){
						final HashSet<Object> refreshSet = new HashSet<>(getBatchSize()*2);
						//draining to a set should weed out the duplicates
						refreshQueue.drainTo(refreshSet);
						if(!refreshSet.isEmpty()){
							scheduler.execute(new Runnable(){
								@SuppressWarnings("rawtypes")
								public void run() {
									List<Object> keys = new ArrayList<>(refreshSet);
									Map<Object,Object> valueMap = new HashMap<Object,Object>(keys.size()*2);
									LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
									Binding safeBinding = new Binding(variables);
									if(bindingDecorator!=null){
										bindingDecorator.decorateRecursive(variables);
									}
									safeBinding.setVariable(MAP, valueMap);
									final Binding oldBinding = ScriptHelper.THREAD_BINDING.get();
									ScriptHelper.THREAD_BINDING.set(safeBinding);
									try {
										Closure closure = getCacheLoader().rehydrate(getScriptHelper(), safeBinding, getScriptHelper());
										//split up the work into batches
										int numBatches = (int)Math.ceil(keys.size()/(double)getBatchSize());
										for(int b=0;b<numBatches;b++){
											valueMap.clear();
											for(int i=(b*getBatchSize());i<(b+1)*getBatchSize() && i<keys.size();i++){
												valueMap.put(keys.get(i), null);
											}
											closure.call(valueMap);
											for(Entry<Object, Object> lentry: valueMap.entrySet()){
												Object val = lentry.getValue();
												if(val instanceof Future){
													//auto-resolve futures
													val = ((Future)lentry.getValue()).get();
												}
												//instantiate CacheValue
												CacheValue cv = new CacheValue(val);
												getValueStore().put(lentry.getKey(), cv);
											}
										}
										//log.info("Cache async refreshed "+valueMap.size());
									} catch (Exception e) {
										log.log(Level.SEVERE,"Cache refresh error with "+valueMap,e);
									} 
									finally{
										if(oldBinding==null){
											ScriptHelper.THREAD_BINDING.remove();
										}
										else{
											ScriptHelper.THREAD_BINDING.set(oldBinding);
										}
									}
								}
								
							});
						}
					}
				}
				catch(Throwable th){
					log.log(Level.SEVERE,"Error refreshing caches", th);
				}
			}}, 1000, 1000, TimeUnit.MILLISECONDS);
	}
	
	
	public void destroy(){
		refreshFuture.cancel(true);
		refreshQueue.clear();
		valueStore.destroy();
	}
	
	public void refresh(Object key) {
		CacheValue cv = getValueStore().get(key);
		if(cv!=null) {
			if(cv.pendingRefresh.compareAndSet(false, true)){
				refreshQueue.offer(key);
			}
		}
	}
	
	public Map<Object,Object> get(Iterable<Object> keys, int refresh, int ttl){
		Map<Object,CacheValue> found = new LinkedHashMap<Object,CacheValue>();
		Map<Object,CompletableFuture<CacheValue>> loadKeys = null;
		for(Object key: keys){
			found.put(key, null);
		}
		long tooOld = ttl>0 ? Caches.currentCacheTime-ttl :0;
		long toRefresh = refresh > 0 ? Caches.currentCacheTime-refresh : 0;
		//step 1, load from valueStore
		getValueStore().getAll(found);
		int hits = 0, misses = 0;
		//step 2, expire TTL
		for(Entry<Object, CacheValue> entry: found.entrySet()){
			CacheValue cv = entry.getValue();
			if(cv!=null){
				if(cv.getCreated()<tooOld){
					//expired, force sync load
					entry.setValue(null);
					getValueStore().remove(entry.getKey(), cv);
				}
				else{
					if(cv.getCreated()<toRefresh && cv.pendingRefresh.compareAndSet(false, true)){
						//past refresh date, offer for async refresh
						refreshQueue.offer(entry.getKey());
					}
					hits++;
					continue;
				}
			}
			//no value, load it
			if(loadKeys==null) {
				loadKeys = new LinkedHashMap<>();
			}
			loadKeys.put(entry.getKey(), null);
			misses++;
		}
		//step 3, synchronous loading
		if(loadKeys!=null){
			Map<Object,Object> myLoadKeys = null;
			Map<Object,CompletableFuture<CacheValue>> myLoadFutures = null;
			try{
				for(Entry<Object, CompletableFuture<CacheValue>> entry:loadKeys.entrySet()){
					final Object key = entry.getKey();
					CompletableFuture<CacheValue> keyFuture = syncLoadMap.get(key);
					if(keyFuture==null){
						keyFuture = new CompletableFuture<>();
						CompletableFuture<CacheValue> oldFuture = syncLoadMap.putIfAbsent(key, keyFuture);
						if(oldFuture==null){
							if(myLoadKeys==null) {
								myLoadKeys = new LinkedHashMap<>();
								myLoadFutures = new LinkedHashMap<>();
							}
							//we are good to load on this thread
							myLoadKeys.put(key, null);
							myLoadFutures.put(key, keyFuture);
							//System.out.println("Will load key "+key+" on thread "+Thread.currentThread().getName());
							entry.setValue(keyFuture);
						}
						else{
							//another thread beat us
							//System.out.println("Will belatedly await "+key+" on thread "+Thread.currentThread().getName());
							entry.setValue(oldFuture);
						}
					}else{
						//another thread is loading
						//System.out.println("Will await key "+key+" on thread "+Thread.currentThread().getName());
						entry.setValue(keyFuture);
					}
				}
				if(myLoadKeys != null){
					LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
					Binding safeBinding = new Binding(variables);
					if(bindingDecorator!=null){
						bindingDecorator.decorateRecursive(variables);
					}
					safeBinding.setVariable(MAP, myLoadKeys);
					final Binding oldBinding = ScriptHelper.THREAD_BINDING.get();
					ScriptHelper.THREAD_BINDING.set(safeBinding);
					try {
						@SuppressWarnings("rawtypes")
						Closure closure = getCacheLoader().rehydrate(getScriptHelper(), safeBinding, getScriptHelper());
						closure.call(myLoadKeys);
						//System.out.println("Cache sync loaded "+myLoadKeys.size());
						for(Entry<Object, Object> lentry: myLoadKeys.entrySet()){
							Object val = lentry.getValue();
							if(val instanceof Future){
								//auto-resolve futures
								@SuppressWarnings("unchecked")
								Future<Object> fv = ((Future<Object>)lentry.getValue());
								val = fv.get();
							}
							//instantiate CacheValue
							CacheValue cv = new CacheValue(val);
							getValueStore().put(lentry.getKey(), cv);
							myLoadFutures.get(lentry.getKey()).complete(cv);
							//found.put(lentry.getKey(),cv);
						}
					} catch (Exception e) {
						for(CompletableFuture<CacheValue> future: myLoadFutures.values()){
							future.completeExceptionally(e);
						}
						throw new RuntimeException("Could not load value for "+loadKeys+": "+e.getClass().getName()+": "+e.getMessage(),e);
					} 
					finally{
						if(oldBinding==null){
							ScriptHelper.THREAD_BINDING.remove();
						}
						else{
							ScriptHelper.THREAD_BINDING.set(oldBinding);
						}
					}
				}
				for(Entry<Object, CompletableFuture<CacheValue>> entry:loadKeys.entrySet()){
					found.put(entry.getKey(),entry.getValue().get());
				}
			}
			catch(InterruptedException e){
				throw new RuntimeException(e);
			}
			catch(ExecutionException e){
				throw new RuntimeException(e);
			}
			finally{
				if(myLoadFutures!=null) {
					for(Entry<Object, CompletableFuture<CacheValue>> entry: myLoadFutures.entrySet()){
						syncLoadMap.remove(entry.getKey(), entry.getValue());
					}
				}
			}
		}
		//step 4, produce return map with clones as needed and nulls ommitted
		Map<Object,Object> rval = new LinkedHashMap<>();
		for(Entry<Object, CacheValue> entry: found.entrySet()){
			CacheValue cv;
			Object v;
			if(((cv =entry.getValue())!= null) && ((v=cv.getValue())!=null)){
				rval.put(entry.getKey(),v);
			};
		}
		manager.update(hits, misses);
		return rval;
	}
	
	public void put(Object key, Object value){
		if(value!=null){
			valueStore.put(key, new CacheValue(value));
		}
		else{
			valueStore.remove(key);
		}
	}
	
	public void remove(Object key){
		valueStore.remove(key);
	}
	
	public void remove(Object key, Object value){
		CacheValue cv = valueStore.get(key);
		if(cv!=null){
			if(value!=null){
				if(value.equals(cv.getValue())){
					valueStore.remove(key, cv);
				}
			}
			else if(cv.getValue()==null){
				valueStore.remove(key, cv);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public Closure getCacheLoader() {
		return cacheLoader;
	}

	@SuppressWarnings("rawtypes")
	public void setCacheLoader(Closure cacheLoader) {
		this.cacheLoader = cacheLoader;
	}


	public int getBatchSize() {
		return batchSize;
	}


	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}


	public CacheValueStore getValueStore() {
		return valueStore;
	}


	public void setValueStore(CacheValueStore valueStore) {
		this.valueStore = valueStore;
	}


	public ScriptHelper getScriptHelper() {
		return scriptHelper;
	}


	public void setScriptHelper(ScriptHelper scriptHelper) {
		this.scriptHelper = scriptHelper;
	}


	public BindingDecorator getBindingDecorator() {
		return bindingDecorator;
	}


	public void setBindingDecorator(BindingDecorator bindingDecorator) {
		this.bindingDecorator = bindingDecorator;
	}
	
	public CacheManager getCacheManager(){
		return manager;
	}


	public ScheduledExecutorService getScheduler() {
		return scheduler;
	}


	public void setScheduler(ScheduledExecutorService scheduler) {
		this.scheduler = scheduler;
	}
	
}

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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
/**
 * Default cache value store implementation; uses soft references along with a hard limit to provide forgiving memory management
 *
 * @author Alex Vigdor
 */
public class SoftCacheValueStore implements CacheValueStore {
	private ConcurrentHashMap<Object, SoftCacheReference> backingMap = new ConcurrentHashMap<Object, SoftCacheReference>();
	private int ttl =-1;
	private int max = -1;
	private ReferenceQueue<CacheValue> queue;
	@SuppressWarnings("rawtypes")
	private Future pruneFuture;
	private ScheduledExecutorService scheduler;
	
	public SoftCacheValueStore(ScheduledExecutorService scheduler){
		this.scheduler=scheduler;
		this.queue = new ReferenceQueue<CacheValue>();
	}
	
	public void init(){
		pruneFuture = scheduler.scheduleWithFixedDelay(new Runnable(){
			public void run() {
				SoftCacheReference sv;
				while ((sv = (SoftCacheReference)queue.poll()) != null) {
					backingMap.remove(sv.key, sv);
					//System.out.println("Removing garbage collected "+sv.key);
				}
				if(getTtl()>0){
					long cutoff = Caches.currentCacheTime - getTtl();
					Iterator<SoftCacheReference> scrs = backingMap.values().iterator();
					while(scrs.hasNext()){
						SoftCacheReference ref = scrs.next();
						CacheValue cv = ref.get();
						if(cv==null ||  cv.getCreated()<cutoff){
							scrs.remove();
							//System.out.println("Removing expired "+ref.key);
						}
					}
				}
				if(max>0){
					List<SoftCacheReference> values = new ArrayList<SoftCacheReference>(backingMap.values());
					if(values.size()>max){
						//System.out.println("Pruning soft cache from "+values.size()+" to "+max);
						Collections.sort(values);
						int toDelete = values.size()-max;
						for(int i=0;i<toDelete;i++){
							SoftCacheReference ref = values.get(i);
							backingMap.remove(ref.key);
							//System.out.println("Removing least recently used "+ref.key+" last accessed "+ref.lastAccess);
						}
					}
				}
			}		
		}, 2000, 2000, TimeUnit.MILLISECONDS);
	}
	
	public void destroy(){
		pruneFuture.cancel(true);
		backingMap.clear();
	}

	public CacheValue get(Object key) {
		SoftCacheReference scr = backingMap.get(key);
		return scr!=null?scr.get():null;
	}

	public void getAll(Map<Object, CacheValue> values) {
		for(Entry<Object, CacheValue> entry: values.entrySet()){
			SoftCacheReference scr = backingMap.get(entry.getKey());
			if(scr!=null){
				entry.setValue(scr.get());
			}
		}
	}

	public void put(Object key, CacheValue value) {
		backingMap.put(key, new SoftCacheReference(key, value, queue));
	}

	public void remove(Object key, CacheValue value) {
		if(value!=null){
			SoftCacheReference scr = backingMap.get(key);
			if(scr!=null && value.equals(scr.get())){
				backingMap.remove(key,scr);
			}
		}
	}
	
	public void remove(Object key) {
		backingMap.remove(key);
	}

	public void clear() {
		backingMap.clear();
	}
	
	public int size(){
		return backingMap.size();
	}
	
	public int getTtl() {
		return ttl;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	private class SoftCacheReference extends SoftReference<CacheValue> implements Comparable<SoftCacheReference>{
		protected final Object key;
		protected long lastAccess;
		
		public SoftCacheReference(Object key, CacheValue value, ReferenceQueue<CacheValue> queue){
			super(value,queue);
			this.key=key;
			lastAccess = Caches.currentCacheTime;
		}
		
		public CacheValue get(){
			lastAccess = Caches.currentCacheTime;
			return super.get();
		}

		public int compareTo(SoftCacheReference o) {
			return (int)(o.lastAccess-lastAccess);
		}
	}
}

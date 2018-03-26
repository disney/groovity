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
package com.disney.uriparcel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maintains a local representation of state loaded from some underlying URI that might change
 * over time, but that is meant to be consumed in a consistent fashion.  
 * <p>
 * A synchronous load from the origin is performed on first call(), subsequently only asynchronous 
 * loads will be performed if a refresh interval has been specified and has passed.
 * <p>
 * Having no refresh is essentially a read-once and forget parsing of the remote source, whereas 
 * a refresh keeps the object up-to-date over time in case it changes on the origin
 * <p>
 * URIParcel relies on ContentStreamStores to provide underlying storage and retrieval of binary streams,
 * and ContentValueHandlers to provide serialization and deserialization of those streams to specific java classes. 
 * Both of these components are discovered using the Java ServiceLoader mechanism to support custom plugins.
 * 
 * @author Alex Vigdor
 *
 */
public class URIParcel<V> implements Callable<V> {
	private static ExecutorService backgroundRefresh = Executors.newSingleThreadExecutor(new ThreadFactory() {
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName("URI Parcel Refresh "+t.getName());
			return t;
		}
	});
	private static ServiceLoader<ContentStreamStore> streamStores = ServiceLoader.load(ContentStreamStore.class);
	private static ServiceLoader<ContentValueHandler> valueHandlers = ServiceLoader.load(ContentValueHandler.class);
	private static List<ContentStreamStore> sortedStreamLoaders;
	private static List<ContentValueHandler> sortedValueLoaders;
	private static Logger log = Logger.getLogger(URIParcel.class.getName());
	
	private Class<V> type;
	private Callable<URI> source;
	volatile V value;
	boolean loaded = false;
	volatile long loadtime = 0;
	volatile boolean refreshing=false;
	private Callable<Long> refresh = null;
	private Callable<Map<String,Object>> config;
	
	public URIParcel(Class<V> type){
		this.type=type;
	}
	
	public URIParcel(Class<V> type, Callable<URI> source){
		this.type=type;
		this.source=source;
	}
	
	public URIParcel(Class<V> type, Callable<URI> source, Map<String,Object> config){
		this.type=type;
		this.source=source;
		this.config = new SimpleCallable<Map<String,Object>>(config);
	}
	
	public URIParcel(Class<V> type, Callable<URI> source, Callable<Map<String,Object>> config){
		this.type=type;
		this.source=source;
		this.config = config;
	}
	
	public URIParcel(Class<V> type, Callable<URI> source, long refresh){
		this.type=type;
		this.source=source;
		this.refresh=new SimpleCallable<Long>(refresh);
	}
	
	public URIParcel(Class<V> type, Callable<URI> source, Callable<Long> refresh, Callable<Map<String,Object>> config){
		this.type=type;
		this.source=source;
		this.refresh=refresh;
		this.config=config;
	}
	
	public URIParcel(Class<V> type, Callable<URI> source, long refresh, Map<String,Object> config){
		this.type=type;
		this.source=source;
		this.refresh=new SimpleCallable<Long>(refresh);
		this.config = new SimpleCallable<Map<String,Object>>(config);
	}
	
	public URIParcel(Class<V> type, URI source){
		this.type=type;
		setSource(source);
	}
	
	public URIParcel(Class<V> type, URI source, Map<String,Object> config){
		this.type=type;
		setSource(source);
		this.config = new SimpleCallable<Map<String,Object>>(config);
	}
	
	public URIParcel(Class<V> type, URI source, long refresh){
		this.type=type;
		setSource(source);
		this.refresh=new SimpleCallable<Long>(refresh);;
	}
	
	public URIParcel(Class<V> type, URI source, long refresh, Map<String,Object> config){
		this.type=type;
		setSource(source);
		this.refresh=new SimpleCallable<Long>(refresh);
		this.config = new SimpleCallable<Map<String,Object>>(config);
	}
	
	public static <T> T get(URI uri, Class<T> type) throws Exception{
		return new URIParcel<T>(type,uri).call();
	}
	
	public static void put(URI uri, Object obj) throws Exception{
		put(uri,obj,null);
	}

	public static <T> void put(URI uri, T obj, String contentType) throws Exception{
		@SuppressWarnings("unchecked")
		Class<T> tC = (Class<T>) obj.getClass();
		new URIParcel<T>(tC, uri).put(obj,contentType);
	}
	
	protected V doLoad() throws Exception {
		try{
			ContentStream stream = getContentStream(source.call(),config!=null?config.call():null);
			if(stream==null){
				throw new FileNotFoundException("No stream found for "+source.call());
			}
			value = getContentValue(stream);
			loaded = true;
			
		}
		finally{
			loadtime = System.currentTimeMillis();
			refreshing=false;
		}
		return value;
	}

	public Callable<URI> getSource() {
		return source;
	}
	
	public V call() throws Exception{
		if(!loaded){
			//first-time synchronous load
			return doLoad();
		}
		else if(refresh!=null){
			long rf = refresh.call();
			if(rf>0 && !refreshing){
				long curTime = System.currentTimeMillis();
				if(curTime > (loadtime+rf)){
					loadtime=curTime;
					refreshing=true;
					//perform async load
					backgroundRefresh.submit(new Runnable() {
						public void run() {
							try{
								doLoad();
							}
							catch(Exception e){
								log.log(Level.SEVERE, "Error refreshing URIParcel "+source, e);
							}
						}
					});
				}
			}
		}
		return value;
	}

	public void setSource(Callable<URI> source) {
		this.source = source;
	}
	
	public void setSource(final URI source) {
		this.source = new Callable<URI>() {
			public URI call() throws Exception {
				return source;
			}
		};;
	}

	public Callable<Long> getRefresh() {
		return refresh;
	}

	public void setRefresh(Callable<Long> refresh) {
		this.refresh = refresh;
	}
	
	public void setRefresh(long refresh){
		this.refresh=new SimpleCallable<Long>(refresh);
	}
	
	public Class<V> getType(){
		return type;
	}
	
	protected static List<ContentStreamStore> getStreamLoaders(){
		if(sortedStreamLoaders==null){
			List<ContentStreamStore> csl = new ArrayList<ContentStreamStore>();
			for(ContentStreamStore loader: streamStores){
				csl.add(loader);
			}
			Collections.sort(csl);
			sortedStreamLoaders = csl;
		}
		return sortedStreamLoaders;
	}
	
	protected static List<ContentValueHandler> getValueLoaders(){
		if(sortedValueLoaders==null){
			List<ContentValueHandler> csl = new ArrayList<ContentValueHandler>();
			for(ContentValueHandler loader: valueHandlers){
				csl.add(loader);
			}
			Collections.sort(csl);
			sortedValueLoaders = csl;
		}
		return sortedValueLoaders;
	}

	protected static ContentStream getContentStream(URI uri, Map<?,?> config) throws IOException{
		ContentStream content = new ContentStream();
		content.setUri(uri);
		for(ContentStreamStore loader: getStreamLoaders()){
			if(log.isLoggable(Level.FINE)){
				log.fine("Trying stream loader "+loader);
			}
			if(loader.load(content,config)){
				return content;
			}
		}
		throw new IllegalArgumentException("No ContentStreamLoader found for "+uri);
	}
	
	protected V getContentValue(ContentStream stream) throws Exception{
		Map<?,?> conf = config!=null?config.call():null;
		for(ContentValueHandler loader: getValueLoaders()){
			if(log.isLoggable(Level.FINE)){
				log.fine("Trying value loader "+loader);
			}
			try{
				if(loader.isSupported(type, stream.getContentType())){
					InputStream is = stream.getContent().call();
					try{
						V val = (V) loader.load(is, stream.getContentType(), type, conf);
						if(val!=null){
							return val;
						}
					}
					finally{
						is.close();
					}
				}
			}
			catch(Exception e){
				StringBuilder msgBuilder = new StringBuilder(e.getClass().getName()).append("; ");
				if(e.getMessage()!=null){
					msgBuilder.append(e.getMessage());
				}
				for(StackTraceElement elem: e.getStackTrace()){
					if(elem.getLineNumber()>=0 && elem.getClassName().equals(loader.getClass().getName())){
						msgBuilder.append("; ").append(elem.toString());
					}
				}
				String msg = msgBuilder.toString();
				if(log.isLoggable(Level.FINE)){
					log.log(Level.FINE, "Skipping value handler for GET "+loader+" due to error "+msg, e);
				}
				else{
					log.log(Level.WARNING, "Skipping value loader for GET "+loader+" due to error "+msg);
				}
			}
		}
		throw new IllegalArgumentException("No ContentValueLoader found for "+type.getName());
	}

	public Callable<Map<String,Object>> getConfig() {
		return config;
	}

	public void setConfig(Callable<Map<String,Object>> config) {
		this.config = config;
	}
	
	public void setConfig(Map<String,Object> config) {
		this.config = new SimpleCallable<Map<String,Object>>(config);
	}

	public void put(V value) throws Exception{
		put(value,null);
	}
	
	public void put(V value, String contentType) throws Exception{
		//TODO overflow to local disk if size is too big
		Map<?,?> conf = config!=null?config.call():null;
		boolean foundhandler = false;
		ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
		for(ContentValueHandler loader: getValueLoaders()){
			if(log.isLoggable(Level.FINE)){
				log.fine("Trying value loader "+loader);
			}
			try{
				if(loader.isSupported(type, contentType)){
					if(loader.store(tempOut, contentType, value, conf)){
						foundhandler=true;
						break;
					}
					tempOut.reset();
				}
			}
			catch(Exception e){
				StringBuilder msgBuilder = new StringBuilder(e.getClass().getName()).append("; ");
				if(e.getMessage()!=null){
					msgBuilder.append(e.getMessage());
				}
				for(StackTraceElement elem: e.getStackTrace()){
					if(elem.getLineNumber()>=0 && elem.getClassName().equals(loader.getClass().getName())){
						msgBuilder.append("; ").append(elem.toString());
					}
				}
				String msg = msgBuilder.toString();
				if(log.isLoggable(Level.FINE)){
					log.log(Level.FINE, "Skipping value handler for PUT "+loader+" due to error "+msg, e);
				}
				else{
					log.log(Level.WARNING, "Skipping value handler for PUT "+loader+" due to error "+msg);
				}
			}
		}
		if(!foundhandler){
			throw new Exception("No handler found for "+value.getClass().getName()+" / "+contentType);
		}
		final byte[] bytes = tempOut.toByteArray();
		ContentStream toStore = new ContentStream(this.source.call());
		toStore.setContent(new Callable<InputStream>() {
			public InputStream call() throws Exception {
				return new ByteArrayInputStream(bytes);
			}
		});
		toStore.setContentLength(bytes.length);
		toStore.setContentType(contentType);
		toStore.setLastModified(System.currentTimeMillis());
		for(ContentStreamStore loader: getStreamLoaders()){
			if(log.isLoggable(Level.FINE)){
				log.fine("Trying stream loader "+loader);
			}
			if(loader.store(toStore,conf)){
				return;
			}
		}
		throw new Exception("No store found for "+toStore.getUri());
	}

	private static class SimpleCallable<V> implements Callable<V>{
		final V val;
		public SimpleCallable(V val){
			this.val=val;
		}

		public V call() throws Exception {
			return val;
		}
	}
}

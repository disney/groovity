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
package com.disney.groovity.source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.disney.groovity.GroovityConstants;
/**
 * Abstract base class for source locators; provides scheduled polling, tracks source state and handles sending events to listeners.
 *
 * @author Alex Vigdor
 */
public abstract class AbstractGroovitySourceLocator implements GroovitySourceLocator, GroovityConstants{
	private static final Log log = LogFactory.getLog(AbstractGroovitySourceLocator.class);
	private ArrayList<GroovitySourceListener> sourceListeners;
	private HashMap<String,GroovitySource> cachedSources;
	private ScheduledExecutorService ex;
	private int interval = 30;

	public abstract Iterator<GroovitySource> iterator();
	
	public void init(){
		cachedSources = new HashMap<String, GroovitySource>();
		if(interval > 0){
			ex = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r);
					t.setName("Groovity Source Locator "+t.getName());
					return t;
				}
			});
			ex.scheduleWithFixedDelay(new Runnable() {
				public void run() {
					try{
						poll();
					}
					catch(Exception e){
						log.error("Error checking for source updates",e);
					}
				}
			}, interval, interval, TimeUnit.SECONDS);
		}
	}
	
	public void destroy(){
		if(ex!=null){
			ex.shutdown();
			try {
				if(!ex.awaitTermination(30, TimeUnit.SECONDS)) {
					ex.shutdownNow();
					ex.awaitTermination(10, TimeUnit.SECONDS);
				}
			} catch (InterruptedException e) {
				ex.shutdownNow();
			}
			ex=null;
		}
	}
	
	/**
	 * Poll for changes and notify listeners
	 */
	public synchronized void poll(){
		if(log.isDebugEnabled()){
			log.debug("Polling for source changes");
		}
		HashSet<GroovitySource> changeSources = new HashSet<GroovitySource>();
		HashSet<String> oldSources = new HashSet<String>(cachedSources.keySet());
		for(GroovitySource source: this){
			//log.info("Validating source "+source.getPath());
			oldSources.remove(source.getPath());
			GroovitySource oldSource = cachedSources.put(source.getPath(),source);
			if(oldSource==null || oldSource.getLastModified()!=source.getLastModified()){
				changeSources.add(source);
			}
		}
		//remaining oldSources should be deleted
		for(String p: oldSources){
			GroovitySource oldSource = cachedSources.remove(p);
			changeSources.add(oldSource);
		}
		if(changeSources.size()>0){
			fireSourceChanged(changeSources.toArray(new GroovitySource[0]));
		}
	}
	
	protected void fireSourceChanged(GroovitySource... sources){
		if(sourceListeners!=null){
			for(GroovitySourceListener listener: sourceListeners){
				listener.sourcesChanged(sources);
			}
		}
	}

	public void addSourceListener(GroovitySourceListener listener) {
		if(sourceListeners==null){
			sourceListeners = new ArrayList<GroovitySourceListener>();
		}
		sourceListeners.add(listener);
	}

	public void removeSourceListener(GroovitySourceListener listener) {
		if(sourceListeners!=null){
			sourceListeners.remove(listener);
		}
	}

	public ArrayList<GroovitySourceListener> getSourceListeners() {
		return sourceListeners;
	}

	public void setSourceListeners(ArrayList<GroovitySourceListener> sourceListeners) {
		this.sourceListeners = sourceListeners;
	}

	public int getInterval() {
		return interval;
	}

	/**
	 * Control the refresh rate in seconds 
	 * 
	 * @param interval
	 */
	public void setInterval(int interval) {
		this.interval = interval;
	}

}

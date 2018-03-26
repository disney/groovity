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
package com.disney.groovity.conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.disney.groovity.GroovityConstants;

/**
 * Abstract base class for configurators; this holds all configuration data in memory, and refreshes configuration values from subclasses every 
 * 10 seconds to allow runtime configuration changes to flow through.
 *
 * @author Alex Vigdor
 */
public abstract class AbstractConfigurator implements Configurator, GroovityConstants {
	private static final Logger log = Logger.getLogger(Configurator.class.getName());
	private ScheduledExecutorService configLoader ;
	private ConcurrentHashMap<ConfigurationKey,String> loadedConfiguration;
	private List<ConfigurationKey> loadedKeys;
	private boolean initialized = false;

	public void configure(String sourcePath, Set<String> propertyNames, BiConsumer<String, String> propertySetter){
		if(sourcePath.endsWith(GROOVITY_SOURCE_EXTENSION)){
			sourcePath = sourcePath.substring(0, sourcePath.length()-5);
		}
		if(sourcePath.startsWith("/")){
			sourcePath = sourcePath.substring(1);
		}
		String[] sourceElems = sourcePath.split("/");
		if(loadedKeys!=null){
			for(String propertyName : propertyNames){
				loadedKeysLoop:
				for(ConfigurationKey key: loadedKeys){
					String[] kp = key.getPath();
					if(kp.length<=sourceElems.length){
						if(key.getProperty().equals(propertyName)){
							for(int i=0;i<kp.length;i++){
								if(!kp[i].equals(sourceElems[i])){
									continue loadedKeysLoop;
								}
							}
							//we got this far, we have a match
							propertySetter.accept(propertyName, loadedConfiguration.get(key));
							break loadedKeysLoop;
						}
					}
				}
			}
		}
	}
	
	public void init(){
		if(!initialized){
			initialized = true;
			configLoader = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r);
					t.setName("Groovity Config Loader "+t.getName());
					return t;
				}
			});
			configLoader.scheduleWithFixedDelay(new Runnable(){public void run() {
				doSetup();
			}}, 10, 10, TimeUnit.SECONDS);
		}
		doSetup();
	}
	public void destroy(){
		if(initialized){
			initialized = false;
			configLoader.shutdownNow();
			try {
				configLoader.awaitTermination(30, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				log.warning("Configurator destroy was interrupted");
			}
		}
	}
	
	protected void doSetup(){
		try{
			Map<ConfigurationKey,String> newConf = loadConfiguration();
			List<ConfigurationKey> newKeys = new ArrayList<ConfigurationKey>(newConf.keySet());
			Collections.sort(newKeys, new Comparator<ConfigurationKey>() {
				public int compare(ConfigurationKey o1, ConfigurationKey o2) {
					return o2.getPath().length-o1.getPath().length;
				}
			});
			loadedConfiguration = new ConcurrentHashMap<ConfigurationKey, String>(newConf);
			loadedKeys = newKeys;
		}
		catch(Exception e){
			log.log(Level.SEVERE, "Error setting up configurator", e);
		}
	}
	
	protected abstract Map<ConfigurationKey,String> loadConfiguration() throws Exception;

}

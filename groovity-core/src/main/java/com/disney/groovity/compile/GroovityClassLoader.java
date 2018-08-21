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
package com.disney.groovity.compile;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;

import org.codehaus.groovy.control.CompilerConfiguration;

import com.disney.groovity.ArgsResolver;
import com.disney.groovity.BindingDecorator;
import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityConstants;
import com.disney.groovity.GroovityObjectConverter;
import com.disney.groovity.cache.Cache;
import com.disney.groovity.cache.SoftCacheValueStore;
import com.disney.groovity.conf.Configurator;
import com.disney.groovity.util.AsyncChannel;
import com.disney.groovity.util.AsyncChannelObserver;
import com.disney.groovity.util.ScriptHelper;

import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

/**
 * Custom classloader for Groovity; a single class loader is generated per groovity script.  This class loader implements protection domain, 
 * provides access to caches and args resolver as well as the ability to discover all other compiled groovity scripts in separate classloaders.
 *
 * @author Alex Vigdor
 */
public class GroovityClassLoader extends GroovyClassLoader implements GroovityConstants, AsyncChannelObserver{
	private static final Logger log = Logger.getLogger(GroovityClassLoader.class.getName());
	private ProtectionDomain protectionDomain;
	private ScriptHelper helper;
	private Groovity groovity;
	private String sourcePath;
	private String scriptName;
	private ConcurrentHashMap<String,Cache> caches;
	private BindingDecorator bindingDecorator;
	private ArgsResolver argsResolver;
	private List<ObjectName> mbeanNames = new ArrayList<ObjectName>();
	private ScheduledExecutorService scheduler;
	private Logger scriptLogger;
	private Class<Script> scriptClass;
	private java.lang.reflect.Field confField;
	private Map<String, Object> readOnlyConfiguration;
	private Map<String, Object> originalConfiguration;
	private Set<String> configurationKeys;
	@SuppressWarnings("rawtypes")
	private ConcurrentHashMap<String, Class> traits;
	private Set<AsyncChannel> channels = ConcurrentHashMap.newKeySet();
	
	public GroovityClassLoader(String sourcePath, ClassLoader contextClassLoader,
			CompilerConfiguration compilerConfiguration, Groovity groovity, ScheduledExecutorService scheduler, 
			@SuppressWarnings("rawtypes") ConcurrentHashMap<String, Class> traits) throws MalformedURLException {
		super(contextClassLoader,compilerConfiguration);
		this.protectionDomain=new ProtectionDomain(new CodeSource(new URL("http","groovity",sourcePath), (Certificate[]) null), null,this,null);
		this.groovity=groovity;
		this.helper=new ScriptHelper(groovity, this);
		this.sourcePath=sourcePath;
		this.scriptName = sourcePath.substring(0,sourcePath.length()-5);
		this.caches = new ConcurrentHashMap<String, Cache>();
		this.bindingDecorator=groovity.getBindingDecorator();
		this.scheduler=scheduler;
		this.scriptLogger = Logger.getLogger(sourcePath);
		this.traits = traits;
	}
	
	@SuppressWarnings("rawtypes")
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		String traitName = className;
		int p = className.indexOf("$");
		if(p>0) {
			traitName = className.substring(0, p);
		}
		Class trait = traits.get(traitName);
		if(trait!=null && (trait.getClassLoader()!=this)) {
			Class traitClass = trait.getClassLoader().loadClass(className);
			return traitClass;
		}
		throw new ClassNotFoundException(className);
	}

	/**
	 * add special protectiondomain to support java security policy
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Class defineClass(String name, byte[] bytes){
		Class cls = super.defineClass(name, bytes, 0, bytes.length, protectionDomain); 
		if(this.scriptClass==null && Script.class.isAssignableFrom(cls)){
			this.scriptClass=cls;
		}
		setClassCacheEntry(cls);
		return cls;
	}

	@SuppressWarnings("unchecked")
	public void init(@SuppressWarnings("rawtypes") ConcurrentHashMap<String, Class> finalTraits){
		finalTraits.forEach((k,v)->{
			if(!traits.containsKey(k)) {
				traits.put(k, v);
			}
		});
		if(this.readOnlyConfiguration==null && this.scriptClass!=null){
			try {
				java.lang.reflect.Field confField = scriptClass.getDeclaredField(CONF);
				if(Modifier.isStatic(confField.getModifiers())){
					confField.setAccessible(true);
					Object confValue = confField.get(null);
					if(confValue !=null && confValue instanceof Map){
						this.confField=confField;
						this.originalConfiguration =  (Map<String, Object>) confValue;
						this.configurationKeys = Collections.unmodifiableSet(new ConcurrentSkipListSet<>(originalConfiguration.keySet()));
						ConcurrentHashMap<String,Object> configuration = prepareConfiguration();
						setConfiguration(configuration);
					}
				}
			} 
			catch(Exception e){}
		}
	}
	
	public Cache getCache(String name, Closure<?> loader, int ttl, int max){
		Cache cache = caches.get(name);
		if(cache==null && loader!=null){
			//TODO support other caching backends
			cache = new Cache();
			cache.setScriptHelper(helper);
			cache.setCacheLoader(loader.dehydrate());
			cache.setBindingDecorator(bindingDecorator);
			SoftCacheValueStore scvs = new SoftCacheValueStore(scheduler);
			scvs.setTtl(ttl);
			scvs.setMax(max);
			cache.setValueStore(scvs);
			cache.setScheduler(scheduler);
			cache.init();
			Cache oc = caches.putIfAbsent(name, cache);
			if(oc!=null){
				cache.destroy();
				cache = oc;
			}
			else{
				try {
					ObjectName mbeanName = new ObjectName("com.disney.groovity:type=CacheManager,script="+sourcePath+",name="+name);
					ManagementFactory.getPlatformMBeanServer().registerMBean(cache.getCacheManager(), mbeanName);
					mbeanNames.add(mbeanName);
				} catch (Exception e) {
					log.log(Level.SEVERE, "Error registering Cache MBean for "+name+" in "+sourcePath, e);
				} 
			}
		}
		return cache;
	}
	
	private void setConfiguration(ConcurrentHashMap<String, Object> config) throws IllegalArgumentException, IllegalAccessException {
		//put unmodifiable view on script to prevent attempts to mutate from scripts
		//Only configurators (e.g. system properties) should be used to modify conf
		readOnlyConfiguration = Collections.unmodifiableMap(config);
		confField.set(null, readOnlyConfiguration);
	}
	
	private synchronized ConcurrentHashMap<String, Object> prepareConfiguration(){
		ConcurrentHashMap<String,Object> configuration = new ConcurrentHashMap<String, Object>();
		//sanity check conf values
		for (Entry<String, Object> entry: originalConfiguration.entrySet()){
			Object val = entry.getValue();
			if(val == null){
				scriptLogger.warning("Converting null default conf value to empty string for property '"+entry.getKey()+"' in "+sourcePath);
				configuration.put(entry.getKey(), "");
			}
			else if(val instanceof Boolean 
					|| val instanceof Number 
					|| val instanceof String 
					|| val instanceof Class<?> 
					|| (!val.getClass().isArray() && val.getClass().isPrimitive())){
				configuration.put(entry.getKey(), val);
			}
			else if(val instanceof GString){
				configuration.put(entry.getKey(), val.toString());
			}
			else{
				scriptLogger.severe("Configuration should contain String, Boolean or Number default values or type class, unsupported type "+val.getClass().getName()+" found for property '"+entry.getKey()+"' in "+sourcePath);
			}
		}
		return configuration;
	}
	
	public void configure(Configurator configurator){
		if(configurator!=null && confField!=null){
			try{
				if(log.isLoggable(Level.FINE)){
					log.log(Level.FINE,"Configuring "+sourcePath);
				}
				ConcurrentHashMap<String, Object> newConf = prepareConfiguration();
				configurator.configure(sourcePath, newConf.keySet(), (k, v)->{
					if(v==null){
						scriptLogger.warning("Converting null configuration value to empty string for property '"+k+"' in "+sourcePath);
						newConf.put(k, "");
					}
					else{
						Object defaultValue = newConf.get(k);
						Object coercedVal = null;
						if(defaultValue!=null){
							//coerce string values into numbers/boolean as declared in the source
							Class<?> coerceTo;
							if(defaultValue instanceof Class) {
								coerceTo = (Class<?>) defaultValue;
							}
							else {
								coerceTo = defaultValue.getClass();
							}
							try {
								coercedVal = GroovityObjectConverter.convert(v, coerceTo);
							}
							catch(Exception e) {
								scriptLogger.severe("Error converting conf value '"+v+"' to "+coerceTo+" for property '"+k+"' in "+sourcePath+"; leaving default value '"+defaultValue+"' intact.  Caused by "+e.getClass().getName()+": "+e.getMessage());
								return;
							}
						}
						else{
							coercedVal = v;
						}
						newConf.put(k, coercedVal);
					}
				});
				//now remove leftover classes, as they define a target type with no default and no value has been applied
				for(Iterator<Entry<String, Object>> iter = newConf.entrySet().iterator(); iter.hasNext();) {
					Entry<String, Object> entry = iter.next();
					if(entry.getValue() instanceof Class) {
						iter.remove();
					}
				}
				setConfiguration(newConf);
			}
			catch(Exception e){
				log.log(Level.SEVERE, "Error configuring "+sourcePath, e);
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void destroy(){
		channels.forEach(AsyncChannel::close);
		Class[] cls = getLoadedClasses();
		//log.info("WIll check "+cls.length+" classes for destroy from "+this.getTheClass());
		for(Class cl: cls){
			Method m = null;
			try {
				m = cl.getDeclaredMethod(DESTROY);
			} catch (Throwable e) {
				//ignore
			}
			if(m!=null) {
				try {
					if(Modifier.isStatic(m.getModifiers())){
						m.invoke(null);
					}
				} catch (Exception e) {
					log.log(Level.SEVERE,"Error destroying "+cl,e);
				} 
			}/*//let's not - a fresh compile will replace and a restart will clear, but aggressive clearing could lead to issues with dependent scripts
			try {
				Annotation traitAnnotation = cl.getAnnotation(Trait.class);
				if(traitAnnotation!=null) {
					if(log.isLoggable(Level.FINE)) {
						log.fine("Removing trait "+cl.getName());
					}
					groovity.getTraits().remove(cl.getName(), cl);
				}
			} catch (Throwable e) {
			} */
		}	
		for(ObjectName mbeanName: mbeanNames){
			try {
				ManagementFactory.getPlatformMBeanServer().unregisterMBean(mbeanName);
			} catch (Exception e) {
				log.log(Level.FINE, "Error unregistering Cache MBean for "+mbeanName, e);
			} 
		}
		for(Cache cache: caches.values()){
			cache.destroy();
		}
		caches.clear();
	}
	
	public Class<Script> getScriptClass(){
		return scriptClass;
	}
	
	public Collection<Class<Script>> getScriptClasses(){
		return groovity.getGroovityScriptClasses();
	}
	
	public String getSourcePath(){
		return sourcePath;
	}
	
	public String getScriptName(){
		return scriptName;
	}

	public ArgsResolver getArgsResolver() {
		return argsResolver;
	}

	public void setArgsResolver(ArgsResolver argsResolver) {
		this.argsResolver = argsResolver;
	}
	
	public void addConfigurator(Configurator configurator){
		groovity.addConfigurator(configurator);
	}
	
	public Logger getLogger(){
		return scriptLogger;
	}
	
	public ScriptHelper getScriptHelper(){
		return helper;
	}
	
	public Set<String> getConfigurationKeys(){
		return configurationKeys;
	}
	
	public Map<String, Object> getConfiguration(){
		return readOnlyConfiguration;
	}

	@Override
	public void opened(AsyncChannel channel) {
		channels.add(channel);
	}

	@Override
	public void closed(AsyncChannel channel) {
		channels.remove(channel);
	}

	public Collection<AsyncChannel> getChannels(){
		return Collections.unmodifiableCollection(channels);
	}
}

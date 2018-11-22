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
package com.disney.groovity;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import groovy.transform.Trait;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.Janitor;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.tools.GroovyClass;
import org.codehaus.groovy.tools.Utilities;

import com.disney.groovity.cache.Caches;
import com.disney.groovity.compile.CompilerConfigurationDecorator;
import com.disney.groovity.compile.GroovityASTTransformation;
import com.disney.groovity.compile.GroovityClassLoader;
import com.disney.groovity.compile.GroovityCompilerEvent;
import com.disney.groovity.compile.GroovityCompilerEvent.Change;
import com.disney.groovity.compile.GroovitySourceTransformer;
import com.disney.groovity.compile.StatsASTTransformation;
import com.disney.groovity.compile.GroovitySourceTransformer.TransformedSource;
import com.disney.groovity.compile.SkipStatistics;
import com.disney.groovity.conf.Configurator;
import com.disney.groovity.conf.MultiConfigurator;
import com.disney.groovity.doc.Arg;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.ClassDescriptor;
import com.disney.groovity.doc.Function;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.doc.ClassDescriptor.TypedName;
import com.disney.groovity.source.GroovitySource;
import com.disney.groovity.source.GroovitySourceListener;
import com.disney.groovity.source.GroovitySourceLocator;
import com.disney.groovity.stats.GroovityStatistics;
import com.disney.groovity.util.AsyncChannel;
import com.disney.groovity.util.ClosureWritable;
import com.disney.groovity.util.InterruptFactory;
import com.disney.groovity.util.ScriptHelper;
import com.disney.groovity.util.TypeLabel;


/**
 * Groovity is the runtime engine for compiling, loading, running and visiting scripts.  A Groovity is acquired using 
 * a GroovityBuilder which is used to define initialization parameters - Groovity itself only exposes runtime capabilities.
 * <p>
 * Groovity handles compilation of sources from the configured source locators, the reading and writing of JAR files if so configured, 
 * the configuration, binding, argument resolution, loading and running of scripts, as well as visitation and notification of script 
 * changes for external frameworks
 * <p>
 * Groovity is safe for multithreaded access, however it enforces single-threaded compilation, so calls to compile or compileAll
 * should be prepared to handle an exception if concurrent compilation is attempted.
 * 
 * @author Alex Vigdor
 *
 */
public class Groovity implements GroovityConstants{
	private static final Logger log = Logger.getLogger(Groovity.class.getName());
	private static final String GROOVITY_SCRIPT_BINDING_PREFIX = INTERNAL_BINDING_PREFIX.concat("Groovity$script:");
	private static final String GROOVITY_BINDING_DECORATED =INTERNAL_BINDING_PREFIX.concat("Groovity$bndDcr");
	private static final List<String> internalMethodNames = Arrays.asList(RUN,LOAD,TAG,STREAM,"methodMissing","propertyMissing","$static_propertyMissing","$static_methodMissing");
	
	private static final Pattern sourcePattern = Pattern.compile("(?i)(/.*)\\".concat(GROOVITY_SOURCE_EXTENSION));
	private static final Pattern traitPattern = Pattern.compile("\\btrait\\b");
	private static final Script PLACEHOLDER_SCRIPT = new Script() {	
		public Object run() {
			return null;
		}
	};
	
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	private final ConcurrentHashMap<String, Class<Script>> scripts = new ConcurrentHashMap<String, Class<Script>>();
	private final ConcurrentHashMap<String, Long> scriptDates = new ConcurrentHashMap<String, Long>();
	private final ConcurrentHashMap<String, GroovityCompilerEvent> compileEvents = new ConcurrentHashMap<String, GroovityCompilerEvent>();
	private final ConcurrentHashMap<String, Class<Script>> embeddedScripts = new ConcurrentHashMap<String, Class<Script>>();
	private File jarDirectory = null;
	private ClassLoader parentLoader;
	private List<GroovityObserver> observers = new ArrayList<GroovityObserver>();
	private GroovitySourceLocator[] sourceLocators;
	private EnumSet<GroovityPhase> sourcePhases;
	private EnumSet<GroovityPhase> jarPhases;
	private String scriptBaseClass;
	private Taggables tagLib;
	private HttpClient httpClient;
	private AtomicBoolean inCompile = new AtomicBoolean();
	private int asyncThreads = Runtime.getRuntime().availableProcessors()*16;
	private ExecutorService asyncExecutor;
	private ScheduledExecutorService configExecutor;
	private ScheduledExecutorService cacheRefreshExecutor;
	private ScheduledExecutorService cacheTimeExecutor;
	private InterruptFactory interruptFactory;
	private boolean caseSensitive = true;
	private BindingDecorator bindingDecorator;
	private ArgsLookup argsLookup = null;
	private GroovitySourceListener groovitySourceListener = new GroovitySourceListener() {
		
		public void sourcesChanged(GroovitySource... sources) {
			if(sourcePhases!=null && sourcePhases.contains(GroovityPhase.RUNTIME)){
				try{
					compile(false,true,sources);
				}
				catch(Error e){
					log.log(Level.SEVERE,"Automatic compilation threw error",e);
				}
			}
		}
	};
	private Configurator configurator;
	@SuppressWarnings("rawtypes")
	private ConcurrentHashMap<String, Class> traits = new ConcurrentHashMap<>();
	@SuppressWarnings("rawtypes")
	private ConcurrentHashMap<String, Class> inherentTraits = new ConcurrentHashMap<>();
	private List<CompilerConfigurationDecorator> compilerConfigurationDecorators;
	private AtomicBoolean started = new AtomicBoolean(false);
	
	//encourage use of the builder
	protected Groovity(){
		
	}
	
	protected Script createScript(final String scriptName) throws InstantiationException, IllegalAccessException{
		final Class<Script> gsc = getScriptClass(scriptName);
		if(gsc!=null){
			Script gs = gsc.newInstance();
			return gs;
		}
		return null;
	}
	
	protected Class<Script> getScriptClass(final String scriptName){
		final String fixedName = fixCase(scriptName);
		Class<Script> scriptClass = scripts.get(fixedName);
		if(scriptClass==null){
			//fall back on embedded scripts loaded from classpath
			scriptClass = embeddedScripts.get(fixedName);
		}
		return scriptClass;
	}
	
	public Collection<String> getGroovityScriptNames(){
		final HashSet<String> scriptNames = new HashSet<>();
		scriptNames.addAll(scripts.keySet());
		scriptNames.addAll(embeddedScripts.keySet());
		return scriptNames;
	}
	
	public Collection<Class<Script>> getGroovityScriptClasses(){
		return getGroovityScriptNames().stream().map(name -> getScriptClass(name)).collect(Collectors.toSet());
	}
	
	public Map<String, Long> getCompiledStatus()
	{
		Map<String,Long> status = new HashMap<String, Long>();
		for(Entry<String,Long> entry:scriptDates.entrySet()){
			Class<Script> scriptClass = scripts.get(entry.getKey());
			if(scriptClass!=null){
				status.put(getSourcePath(scriptClass), entry.getValue());
			}
		}
		return status;
	}
	public Map<String, GroovityCompilerEvent> getCompilerEvents()
	{
		return this.compileEvents;
	}
	/**
	 * This is the primary method of interest - execute an individual groovity script by path using a given binding.
	 * If the script returns a Writable (streaming template or GString) and a Writer is bound to "out", the value will be streamed to out,
	 * otherwise the return value is passed through to the caller.
	 * 
	 * @param scriptName the path of the script to execute, e.g. /myFolder/myScript
	 * @param binding the groovy binding to use as global variable scope
	 * @return the return value of the script, if any
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public Object run(final String scriptName, final Binding binding) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException{
		Script script = load(scriptName,binding);
		if(script!=null){
			Object rval = script.run();
			ScriptHelper scriptHelper = ((GroovityClassLoader)script.getClass().getClassLoader()).getScriptHelper();
			if(!scriptHelper.processReturn(script, rval)){
				return rval;
			}
		}
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	public Object tag(final String tagName, final Map attributes, final groovy.lang.Closure body) throws Exception{
		return tagLib.tag(tagName,attributes, body);
	}
	
	public boolean hasTag(final String tagName){
		return tagLib.hasTag(tagName);
	}
	
	public boolean hasScript(final String scriptName){
		final String fixedName = fixCase(scriptName);
		return scripts.containsKey(fixedName) || embeddedScripts.containsKey(fixedName);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected final void decorate(final Map vars){
		final Object done = vars.get(GROOVITY_BINDING_DECORATED);
		if(done==null){
			vars.put(GROOVITY_BINDING_DECORATED, GROOVITY_BINDING_DECORATED);
			if(bindingDecorator!=null){
				bindingDecorator.decorateRecursive(vars);
			}
		}
	}
	/**
	 * This method is used to load an instance of a script without executing it, for example in order to use it as a library, 
	 * or to defer running it until a later time; instances are always Singletons within a binding scope, so repeated
	 * calls to load the same script name with the same binding will return the same instance of the script.
	 * 
	 * @param scriptName the path of the script to execute, e.g. /myFolder/myScript
	 * @param binding the groovy binding to use as global variable scope
	 * @return an instance of a script tied to the provided binding
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public Script load(final String scriptName, final Binding binding) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		final String varName = GROOVITY_SCRIPT_BINDING_PREFIX.concat(fixCase(scriptName));
		@SuppressWarnings("rawtypes")
		final Map variables = binding.getVariables();
		Script script = (Script) variables.get(varName);
		if(script==null){
			decorate(variables);
			//register place holder to catch circular dependencies
			variables.put(varName, PLACEHOLDER_SCRIPT);
			try{
				final Binding oldThreadBinding = ScriptHelper.THREAD_BINDING.get();
				if(oldThreadBinding!=binding){
					//register our binding during field init so it propgates
					ScriptHelper.THREAD_BINDING.set(binding);
				}
				try{
					final Class<Script> gsc = getScriptClass(scriptName);
					if(gsc!=null){
						//before we create a new instance, let's look for args
						ArgsResolver abd = ((GroovityClassLoader)gsc.getClassLoader()).getArgsResolver();
						if(abd!=null){
							//now is the time to enforce any defaults, coercion, validation, BEFORE we create a new instance
							//whose field loading might depend on the arg binding decorator
							abd.resolve(variables,argsLookup);
						}
						script = gsc.newInstance();
						if(script!=null){
							script.setBinding(binding);
							variables.put(varName, script);
							if(script instanceof Loadable){
								((Loadable)script).load();
							}
						}
					}
				}
				finally{
					if(oldThreadBinding==null){
						ScriptHelper.THREAD_BINDING.remove();
					}
					else if(oldThreadBinding != binding){
						ScriptHelper.THREAD_BINDING.set(oldThreadBinding);
					}
				}
			}
			finally{
				if(script==null){
					variables.remove(varName);
				}
			}
		}
		if(script==null){
			throw new ClassNotFoundException("No grvt found for "+scriptName);
		}
		if(script==PLACEHOLDER_SCRIPT){
			throw new InstantiationException("Circular load dependency found leading back to "+scriptName);
		}
		return script;
	}
	public File getJarDirectory() {
		return jarDirectory;
	}

	protected void setJarDirectory(File jarDirectory) {
		this.jarDirectory = jarDirectory;
	}
	
	protected void init(boolean init) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		if(tagLib==null){
			tagLib = new Taggables();
		}
		tagLib.init(this);
		compilerConfigurationDecorators = new ArrayList<>();
		ClassLoader loader = parentLoader!=null?parentLoader:Thread.currentThread().getContextClassLoader();
		Iterator<CompilerConfigurationDecorator> decs = ServiceLoader.load(CompilerConfigurationDecorator.class,loader).iterator();
		decs.forEachRemaining(compilerConfigurationDecorators::add);
		if(log.isLoggable(Level.FINE)){
			log.fine("Got compiler decorators from service loader "+compilerConfigurationDecorators);
		}
		Iterator<GroovityObserver> obs = ServiceLoader.load(GroovityObserver.class, loader).iterator();
		obs.forEachRemaining(this::addObserver);
		if(log.isLoggable(Level.FINE)){
			log.fine("Got observers from service loader "+observers);
		}
		loadClasses(init);
		if(sourcePhases!=null && sourcePhases.contains(GroovityPhase.STARTUP) && sourceLocators!=null && sourceLocators.length>0){
			compileAll(false, init);
		}
	}
	
	protected void start() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException{
		log.info("Initializing Groovity");
		if(httpClient==null){
			httpClient = HttpClients.createDefault();
		}
		
		if(argsLookup==null){
			argsLookup = new ArgsLookup();
		}
		asyncExecutor = Executors.newFixedThreadPool(asyncThreads, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("Groovity Async "+t.getName());
				return t;
			}
		});
		cacheRefreshExecutor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors()*4,new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("Groovity Cache "+t.getName());
				return t;
			}
		});
		cacheRefreshExecutor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				GroovityStatistics.warnStuckThreads();
			}
		}, 1, 1, TimeUnit.MINUTES);
		cacheTimeExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("Groovity Cache Time "+t.getName());
				return t;
			}
		});
		cacheTimeExecutor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				Caches.currentCacheTime = (System.currentTimeMillis()/1000);
			}
		}, 1, 1, TimeUnit.SECONDS);
		interruptFactory = new InterruptFactory();
		//intialize configurator first so class inits can pick up system properties
		if(configurator!=null){
			configurator.init();
		}
		init(true);
		if(configurator!=null){
			//initialize configurator again to pick up any changes to system properties that happened during class init
			configurator.init();
			//schedule 10-second configuration refresh 
			configExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r);
					t.setName("Groovity Config Applier "+t.getName());
					return t;
				}
			});
			configExecutor.scheduleWithFixedDelay(new Runnable() {
				public void run() {
					try{
						configureAll();
					}
					catch(Exception e){
						log.log(Level.SEVERE, "Error updating configuration ",e);
					}
				}
			}, 10, 10, TimeUnit.SECONDS);
			configureAll();
		}
		//now start all scripts
		for(Class<Script> c: getGroovityScriptClasses()){
			startClass(c);
		}
		//init locators AFTER compile, to avoid scheduled compilation starting before startup is complete
		if(sourceLocators!=null && sourceLocators.length>0){
			for(GroovitySourceLocator locator:sourceLocators){
				locator.addSourceListener(this.groovitySourceListener);
				locator.init();
			}
		}
		started.set(true);
	}
	
	void shutdownAndAwaitTermination(ExecutorService pool) {
		 pool.shutdown(); // Disable new tasks from being submitted
		 try {
		     // Wait a while for existing tasks to terminate
			 if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
				 pool.shutdownNow(); // Cancel currently executing tasks
				 // Wait a while for tasks to respond to being cancelled
				 if (!pool.awaitTermination(60, TimeUnit.SECONDS)){
					 log.severe("Async ThreadPool did not terminate");
				 }
			 }
		} catch (InterruptedException ie) {
			 // (Re-)Cancel if current thread also interrupted
			 pool.shutdownNow();
			 // Preserve interrupt status
		     Thread.currentThread().interrupt();
		}
	}
	
	public void destroy(){
		if(started.compareAndSet(true, false)) {
			observers.forEach(o->{o.destroy(this);});
			if(configExecutor!=null){
				configExecutor.shutdownNow();
				configurator.destroy();
			}
			cacheRefreshExecutor.shutdownNow();
			cacheTimeExecutor.shutdownNow();
			
			if(sourceLocators!=null){
				for(int i=0;i<sourceLocators.length;i++){
					sourceLocators[i].destroy();
				}
			}
			for(Class<?> c: getGroovityScriptClasses()){
				((GroovityClassLoader)c.getClassLoader()).destroy();
			}
			shutdownAndAwaitTermination(asyncExecutor);
			interruptFactory.destroy();
			if(httpClient instanceof CloseableHttpClient){
				try {
					((CloseableHttpClient)httpClient).close();
				} catch (IOException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			try {
				cacheTimeExecutor.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException e1) {
			}
			if(configExecutor!=null){
				try {
					configExecutor.awaitTermination(20, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
				}
			}
			try {
				cacheRefreshExecutor.awaitTermination(30, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
			}
		}
		tagLib.destroy();
	}
	
	public void visit(GroovityObserver visitor){
		for(Class<Script> sc: getGroovityScriptClasses()){
			GroovityClassLoader gcl = (GroovityClassLoader) sc.getClassLoader();
			visitor.scriptStart(this, gcl.getScriptName(), sc);
		}
	}
	/**
	 * Compile all scripts known to the configured source locators
	 * 
	 * @param force if true, recompile all sources, if false recompile only changed sources
	 */
	public void compileAll(boolean force, boolean init){
		if(inCompile.compareAndSet(false, true)){
			try{
				compileEvents.clear();
				List<GroovitySource> sources = new ArrayList<GroovitySource>();
				//track existing views so we can keep track of files that may have since been deleted
				HashSet<String> oldViews = new HashSet<String>(((Map<String, Class<Script>>)scripts).keySet());
				for(GroovitySourceLocator sourceLocator: sourceLocators){
					for(GroovitySource source: sourceLocator){
						try {
							Matcher matcher = sourcePattern.matcher(source.getPath());
							if(matcher.matches()){
								String name = matcher.group(1);
								oldViews.remove(fixCase(name));
								sources.add(source);
							}
						} catch (Exception e) {
							log.log(Level.SEVERE,"Unable to load source "+source.getPath(),e);
						}
					}
				}
				//remaining values in oldViews correspond to deleted files IF they are not embedded
				for(String del: oldViews){
					Class<Script> cgs = scripts.get(del);
					final String path = getSourcePath(cgs);
					try {
						sources.add(new GroovitySource() {
							
							public String getSourceCode() throws IOException {
								return "";
							}
							
							public String getPath() {
								return path;
							}
							
							public long getLastModified() {
								return System.currentTimeMillis();
							}
							
							public boolean exists() {
								return false;
							}
						});
					} catch (Exception e) {
						log.log(Level.SEVERE,"Error deleting grvt "+del,e);
					}
				}
				compile(force, init, sources.toArray(new GroovitySource[0]));
			}finally{
				inCompile.set(false);
			}
		}
		else{
			throw new RuntimeException("Groovy compiler is already busy, please retry later");
		}
	}
	
	public String getSourcePath(Class<?> scriptClass){
		return getSourcePath(scriptClass.getName());
	}
	
	protected static String getSourcePath(String scriptClassName){
		StringBuilder builder = new StringBuilder();
		String[] parts = scriptClassName.split("___",-1);
		for(int i=0;i<parts.length;i++){
			if(i>0){
				builder.append("_");
			}
			builder.append(parts[i].replaceAll("_", "/"));
		}
		builder.append(GROOVITY_SOURCE_EXTENSION);
		return builder.toString();
	}
	
	protected List<String> getDependencies(Class<?> clz){
		try {
			//load known script dependencies so we can control load order
			java.lang.reflect.Field factoryField = clz.getDeclaredField(INIT_DEPENDENCIES);
			@SuppressWarnings("unchecked")
			List<String> deps = (List<String>) factoryField.get(clz);
			return deps;
		} catch (Exception e) {
		} 
		return new ArrayList<String>();
	}
	
	protected void configureAll(){
		if(configurator!=null) {
			for(Class<Script> scriptClass: getGroovityScriptClasses()){
				((GroovityClassLoader)scriptClass.getClassLoader()).configure(configurator);
			}
		}
	}
	
	protected void initClass(Class<?> clz){
		if(clz!=null){
			final GroovityClassLoader gcl = (GroovityClassLoader)clz.getClassLoader();
			gcl.init(traits);
			//here is where we call upon our Configurator
			if(configurator!=null) {
				try {
					gcl.configure(configurator);
				}
				catch (Exception e) {
					log.log(Level.SEVERE, "Error configuring "+clz.getName(), e);
				}
			}
			//Now let's register the ArgsBindingVisitor with the classloader
			try {
				java.lang.reflect.Field argsField = clz.getDeclaredField(ARGS);
				if(Modifier.isStatic(argsField.getModifiers())){
					argsField.setAccessible(true);
					Object argsValue = argsField.get(null);
					if(argsValue !=null && argsValue instanceof Map){
						@SuppressWarnings("unchecked")
						Map<String,Object> av = (Map<String,Object>)argsValue;
						gcl.setArgsResolver(new ArgsResolver(av,getSourcePath(clz)));
					}
				}
			}
			catch(NoSuchFieldException e){}
			catch (Exception e) {
				log.log(Level.SEVERE, "Error intializing script arguments for "+clz.getName(), e);
			} 
			Class<?>[] cls = gcl.getLoadedClasses();
			for(Class<?> cl: cls){
				try {
					//inject GroovityScriptFactory
					java.lang.reflect.Field factoryField = cl.getDeclaredField(GROOVITY_SCRIPT_HELPER_FIELD);
					factoryField.setAccessible(true);
					factoryField.set(null,gcl.getScriptHelper());
				} catch (Throwable e) {
				} 
				Method m = null;
				try {
					m = cl.getDeclaredMethod(INIT);
				} catch (Throwable e) {
				}
				if(m!=null) {
					try {
						if(Modifier.isStatic(m.getModifiers()) && m.getParameterTypes().length==0){
							LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
							Binding binding = new Binding(variables);
							binding.setVariable(AsyncChannel.ASYNC_CHANNEL_OBSERVER_KEY, gcl);
							if(bindingDecorator!=null){
								bindingDecorator.decorateRecursive(variables);
							}
							ScriptHelper.THREAD_BINDING.set(binding);
							try{
								m.invoke(null);
							}
							finally{
								ScriptHelper.THREAD_BINDING.remove();
							}
							log.info("Initialized "+cl);
						}
					} catch(InvocationTargetException e){
						Throwable t = e.getCause();
						if(t instanceof Error){
							throw (Error)t;
						}
						log.log(Level.SEVERE,"Error initing "+cl,t);
					}
					catch (Exception e) {
						log.log(Level.SEVERE,"Error initing "+cl,e);
					} 
				}
			}
		}
	}
	
	protected void startClass(Class<Script> clz){
		if(clz!=null){
			final GroovityClassLoader gcl = (GroovityClassLoader)clz.getClassLoader();
			Class<?>[] cls = gcl.getLoadedClasses();
			for(Class<?> cl: cls){
				Method m = null;
				try {
					m = cl.getDeclaredMethod(START);
				} catch (Throwable e) {
				}
				if(m!=null) {
					try {
						if(Modifier.isStatic(m.getModifiers()) && m.getParameterTypes().length==0){
							LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
							Binding binding = new Binding(variables);
							binding.setVariable(AsyncChannel.ASYNC_CHANNEL_OBSERVER_KEY, gcl);
							if(bindingDecorator!=null){
								bindingDecorator.decorateRecursive(variables);
							}
							ScriptHelper.THREAD_BINDING.set(binding);
							try{
								m.invoke(null);
							}
							finally{
								ScriptHelper.THREAD_BINDING.remove();
							}
							log.info("Started "+cl);
						}
					} catch(InvocationTargetException e){
						Throwable t = e.getCause();
						if(t instanceof Error){
							throw (Error)t;
						}
						log.log(Level.SEVERE,"Error starting "+cl,t);
					}
					catch (Exception e) {
						log.log(Level.SEVERE,"Error starting "+cl,e);
					} 
				}
			}
			if(observers!=null){
				for(GroovityObserver listener:observers){
					try {
						listener.scriptStart(this, gcl.getScriptName(), clz);
					}
					catch(Throwable th) {
						log.log(Level.WARNING, "Error in GroovityObserver.scriptStart() for "+gcl.getScriptName(), th);
					}
				}
			}
		}
	}
	
	protected boolean hasInit(GroovityClassLoader cll){
		Class<?>[] cls = cll.getLoadedClasses();
		for(Class<?> cl: cls){
			try {
				Method m = cl.getDeclaredMethod(INIT);
				if(Modifier.isStatic(m.getModifiers()) && m.getParameterTypes().length==0){
					return true;
				}
			} catch (Throwable e) {
			} 
		}
		return false;
	}
	//for first-time load ONLY
	protected void loadClasses(boolean init) throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		HashMap<String,Collection<String>> newScriptDependencies = new HashMap<String, Collection<String>>();
		Map<String,Boolean> newScriptInits = new HashMap<String, Boolean>();
		//first, look on classpath for packaged groovity scripts
		ClassLoader loader = parentLoader;
		if(loader==null){
			loader = Thread.currentThread().getContextClassLoader();
		}
		long time1=System.currentTimeMillis();
		int manifestCount = 0;
		List<String[]> manifestLines = new ArrayList<>();
		Enumeration<URL> classpathJarManifests = loader.getResources(GROOVITY_JAR_MANIFEST);
		while(classpathJarManifests.hasMoreElements()){
			URL manifest = classpathJarManifests.nextElement();
			if(log.isLoggable(Level.FINEST)){
				log.finest("Found groovity manifest "+manifest);
			}
			String baseUrl = manifest.toString();
			baseUrl = baseUrl.substring(0,baseUrl.length()-8);
			String jarName = baseUrl;
			int bang = jarName.indexOf("!");
			if(bang > 0){
				jarName = jarName.substring(0, bang);
			}
			int slash = jarName.lastIndexOf("/");
			if(slash>0){
				jarName = jarName.substring(slash+1);
			}
			try{
				BufferedReader reader = new BufferedReader(new InputStreamReader(manifest.openStream(), "UTF-8"));
				try{
					String line;
					while((line=reader.readLine())!=null){
						if(line!=null && !line.isEmpty()){
							manifestLines.add(new String[] {baseUrl,line});
						}
					}
				}
				finally{
					reader.close();
				}
			}
			catch(Exception e){
				log.log(Level.SEVERE, "Error loading groovity from mainfest", e);
			}
			manifestCount++;
		}
		int lastLineCount = manifestLines.size()+1;
		Map<String,Throwable> loadErrors = new HashMap<>();
		int count = 0;
		while(!manifestLines.isEmpty() && (manifestLines.size() < lastLineCount)) {
			lastLineCount = manifestLines.size();
			Iterator<String[]> manifestIterator = manifestLines.iterator();
			while(manifestIterator.hasNext()) {
				String[] manifestLine = manifestIterator.next();
				String baseUrl = manifestLine[0];
				String line = manifestLine[1];
				String sourcePath =  "/".concat(line.substring(0, line.length()-4));
				try {
					URL grvtJar = new URL(baseUrl.concat(line));
					URLConnection conn = grvtJar.openConnection();
					long modTime = conn.getLastModified();
					InputStream jarStream = conn.getInputStream();
					try{
						loadClasses(sourcePath, jarStream, modTime, true, newScriptDependencies, newScriptInits, false);
						count++;
					}
					finally{
						jarStream.close();
					}
					if(log.isLoggable(Level.FINEST)){
						log.finest("Loaded "+sourcePath+" from "+baseUrl);
					}
					manifestIterator.remove();
					loadErrors.remove(sourcePath);
				}
				catch(Throwable e) {
					loadErrors.put(sourcePath, e);
					//we will retry in the loop to recover from dependency ordering issues
				}
			}
		}
		if(count>0){
			log.info("Loaded "+count+" embedded scripts from "+manifestCount+" manifest files in "+(System.currentTimeMillis()-time1));
		}
		if(!loadErrors.isEmpty()) {
			log.severe("There were "+loadErrors.size()+" errors loading manifest scripts ");
			loadErrors.entrySet().stream().forEach(entry -> {
				log.log(Level.SEVERE, entry.getKey(), entry.getValue());
			});
		}
		//next, load from app jar directory
		if(jarPhases!=null && jarPhases.contains(GroovityPhase.STARTUP) && jarDirectory!=null && jarDirectory.isDirectory()){
			List<File> jarList = new ArrayList<>();
			findJarFiles(jarDirectory, jarList);
			if(!jarList.isEmpty()){
				long jarStart = System.currentTimeMillis();
				Map<String,Throwable> jarErrors = new HashMap<>();
				int loadedJars = 0;
				int lastJarCount = jarList.size()+1;
				while(!jarList.isEmpty() && (jarList.size() < lastJarCount)) {
					lastJarCount = jarList.size();
					Iterator<File> jarIter = jarList.iterator();
					while(jarIter.hasNext()) {
						File j = jarIter.next();
						String path = j.getPath();
						try {
							loadClasses(j, newScriptDependencies,newScriptInits);
							jarIter.remove();
							jarErrors.remove(path);
							loadedJars++;
						}
						catch(Throwable e) {
							jarErrors.put(path, e);
						}
					}
				}
				if(!jarErrors.isEmpty()) {
					log.severe("There were "+jarErrors.size()+" errors loading jar files ");
					jarErrors.entrySet().stream().forEach(entry -> {
						log.log(Level.SEVERE, entry.getKey(), entry.getValue());
					});
				}
				if(loadedJars>0) {
					log.info("Loaded "+count+" jar files in "+(System.currentTimeMillis()-jarStart));
				}
			}
		}
		List<String> sortedScripts = sortDependencies(newScriptDependencies,newScriptInits);
		for(String scriptName:sortedScripts){
			Class<Script> theClass = getScriptClass(scriptName);
			if(init) {
				initClass(theClass);
			}
		}
	}
	
	protected void findJarFiles(File file, Collection<File> accumulator) {
		if(file.isDirectory()){
			File[] files = file.listFiles();
			if(files!=null){
				for(File f: files){
					findJarFiles(f,accumulator);
				}
			}
		}
		else if(file.getName().endsWith(".jar")){
			accumulator.add(file);
		}
	}
	
	protected void loadClasses(File file, HashMap<String,Collection<String>> newScriptDependencies, Map<String,Boolean> newScriptInits) throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		String sourcePath = jarDirectory.toURI().relativize(file.toURI()).getPath();
		sourcePath = "/".concat(sourcePath.substring(0, sourcePath.length()-4));
		long time1=System.currentTimeMillis();
		FileInputStream jarStream = new FileInputStream(file);
		try{
			loadClasses(sourcePath, jarStream, file.lastModified(), false, newScriptDependencies, newScriptInits, true);
		}
		finally{
			jarStream.close();
		}
		if(log.isLoggable(Level.FINE)) {
			long time2=System.currentTimeMillis();
			log.fine("Loaded Groovy Script from disk: ".concat(file.getPath())+" in "+(time2-time1));
		}
	}
	
	@SafeVarargs
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected final Class<Script> loadGroovyClassesAndFindScript(GroovityClassLoader loader, GroovyClass[] classes, Map<String, Class>... loadTraits) throws IllegalAccessException {
		Class<Script> scriptClass = null;
		List<Class> defined = new ArrayList<>();
		for(GroovyClass cl:classes){
			defined.add(loader.defineClass(cl.getName(), cl.getBytes()));
		}
		for(Class c: defined){
			if(Script.class.isAssignableFrom(c)){
				scriptClass = c;
				//log.info("Loaded groovy script from disk "+c.getName());
			}
			else if (Taggable.class.isAssignableFrom(c)){
				if(tagLib!=null){
					try {
						tagLib.add((Taggable)c.newInstance());
					} catch (InstantiationException e) {
						log.log(Level.SEVERE,"Could not register GroovyTag "+c.getName(),e);
					}
				}
				//log.info("Loaded supporting class from disk "+c.getName());
			}
			try {
				Annotation traitAnnotation = c.getAnnotation(Trait.class);
				if(traitAnnotation!=null) {
					if(log.isLoggable(Level.FINE)) {
						log.fine("INITING TRAIT "+c.getName());
					}
					for(Map<String,Class> traitMap: loadTraits) {
						traitMap.put(c.getName(), c);
					}
				}
			} catch (Throwable e) {
			} 
		}
		return scriptClass;
	}
	
	protected void loadClasses(String sourcePath, InputStream jarStream, long modTime, boolean embedded, HashMap<String,Collection<String>> newScriptDependencies, Map<String,Boolean> newScriptInits, boolean projectJar) throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		ArrayList<String> dependencies = new ArrayList<String>();
		CompilerConfiguration compilerConfiguration = createCompilerConfiguration(null,dependencies);
		GroovyClass[] classes = loadGroovyClasses(jarStream);
		//a classloader only gets the traits that are available when it is created, so we make a copy
		@SuppressWarnings("rawtypes")
		ConcurrentHashMap<String, Class> traitsCopy = new ConcurrentHashMap<>(traits);
		GroovityClassLoader loader = parentLoader!=null? new GroovityClassLoader(sourcePath,parentLoader,compilerConfiguration,this,cacheRefreshExecutor,traitsCopy) : new GroovityClassLoader(sourcePath,Thread.currentThread().getContextClassLoader(),compilerConfiguration,this,cacheRefreshExecutor,traitsCopy);
		if(classes!=null){
			Class<Script> scriptClass;
			if(projectJar) {
				scriptClass = loadGroovyClassesAndFindScript(loader, classes,traits,traitsCopy);
			}
			else {
				scriptClass = loadGroovyClassesAndFindScript(loader, classes,traits,traitsCopy, inherentTraits);
			}
			if(scriptClass!=null){
				//this seemingly useless call will force failure if there are referenced traits missing so we can retry ...
				for(@SuppressWarnings("rawtypes") Class c: loader.getLoadedClasses()) {
					Field[] fields = c.getDeclaredFields();
					for(Field f: fields) {
						f.getGenericType();
					}
					Method[]  dm = c.getDeclaredMethods();
					for(Method m: dm) {
						m.getGenericParameterTypes();
						m.getGenericReturnType();
						m.getGenericExceptionTypes();
					}
				}
				String path = getSourcePath(scriptClass);
				String script = getScriptName(path);
				String fixed = fixCase(script);
				//System.out.println("Loaded script "+script+" for name "+path+" "+scriptClass);
				newScriptDependencies.put(script,getDependencies(scriptClass));
				newScriptInits.put(script, hasInit((GroovityClassLoader)scriptClass.getClassLoader()));
				if(embedded){
					embeddedScripts.put(fixed,scriptClass);
				}
				else{
					scripts.put(fixed,scriptClass);
					scriptDates.put(fixed, modTime);
				}
			}
			else{
				log.severe("NO SCRIPT CLASS FOUND!! "+sourcePath);
			}
		}
	}
	
	/**
	 * Compile a named set of source paths; compilation also handles static init and destroy lifecycle 
	 * management and jar file creation
	 * 
	 * @param force if true, recompile all sources, if false recompile only changed sources
	 * @param sourcePaths a list of source paths to compile
	 * @throws Exception
	 */
	public void compile(boolean force, boolean init, List<String> sourcePaths) throws Exception{
		if(inCompile.compareAndSet(false, true)){
			try{
				compileEvents.clear();
				List<GroovitySource> sources = new ArrayList<GroovitySource>();
				for(int i=0;i<sourcePaths.size();i++)
				{
					String path = sourcePaths.get(i);
					try{						
						GroovitySource source = null;
						Exception error=null;
						for(GroovitySourceLocator sourceLocator:sourceLocators){
							try{
								source = sourceLocator.getGroovityScriptSource(path);
								if(source!=null && source.exists()){
									break;
								}
							}
							catch(Exception e){
								error  = e;
							}							
						}
						if(source!=null){
							sources.add(source);
						}
						else if(error!=null){
							throw error;
						}
						else{
							throw new FileNotFoundException(path);
						}
					}
					catch(Exception e)
					{
						log.log(Level.SEVERE,"Unable to load source "+path,e);
					}
				}	
				compile(force, init, sources.toArray(new GroovitySource[0]));
			}finally{
				inCompile.set(false);
			}
		}
		else{
			throw new RuntimeException("Groovy compiler is already busy, please retry later");
		}
	}
	
	public boolean isCompiling(){
		return inCompile.get();
	}
	
	private String getScriptName(String sourcePath){
		Matcher matcher = sourcePattern.matcher(sourcePath);
		if(!matcher.matches()){
			throw new RuntimeException("Source file name does not match required pattern: "+sourcePath);
		}
		String name = matcher.group(1);
		return name;
	}
	
	private List<String> sortDependencies(final Map<String,Collection<String>> scriptDependencies,  Map<String,Boolean> scriptInits){
		ArrayList<String> scriptNames = new ArrayList<String>();
		scriptNames.addAll(scriptDependencies.keySet());
		ArrayList<String> rval = new ArrayList<String>();
		Iterator<String> scriptNameIterator = scriptNames.iterator();
		//first load up all scripts with no dependencies
		while(scriptNameIterator.hasNext()){
			String scriptName = scriptNameIterator.next();
			Collection<String> deps = scriptDependencies.get(scriptName);
			if(deps==null || deps.isEmpty()){
				rval.add(scriptName);
				scriptNameIterator.remove();
			}
		}
		int sizeRemaining = scriptNames.size();
		while(sizeRemaining > 0){
			scriptNameIterator = scriptNames.iterator();
			//next try to wait until dependencies are satisfied
			scriptLoop:
			while(scriptNameIterator.hasNext()){
				String scriptName = scriptNameIterator.next();
				Collection<String> deps = scriptDependencies.get(scriptName);
				for(String dep: deps){
					if(scriptDependencies.containsKey(dep) && !rval.contains(dep)){
						//dependency is not ready yet
						continue scriptLoop;
					}
				}
				rval.add(scriptName);
				scriptNameIterator.remove();
			}
			if(sizeRemaining==scriptNames.size()){
				//we have run out of paths ... let us now process any scripts with no init methods
				scriptNameIterator = scriptNames.iterator();
				while(scriptNameIterator.hasNext()){
					String scriptName = scriptNameIterator.next();
					Boolean hasInit = scriptInits.get(scriptName);
					if(!hasInit.booleanValue()){
						rval.add(scriptName);
						scriptNameIterator.remove();
					}
				}
			}
			if(sizeRemaining == scriptNames.size()){
				//we failed to load any more
				throw new IllegalArgumentException("Unable to determine appropriate load order due to cylic dependencies "+scriptNames);
			}
			sizeRemaining = scriptNames.size();
		}
		return rval;
	}

	protected void compile(boolean force, boolean init, GroovitySource... sources){
		compile(force, init, new ConcurrentHashMap<>(inherentTraits), sources);
	}
	/**
	 * Compare the timestamp of the source against an existing compiled version, only recompiling
	 * if they do not match.  The force parameter is used to force a recompile regardless of dates.
	 * 
	 * @param sources
	 * @param force
	 */
	@SuppressWarnings("rawtypes") 
	protected void compile(boolean force, boolean init, ConcurrentHashMap<String, Class> compilerTraits, GroovitySource... sources){
		//take multiple sources and compile as a set, only initing() classes once all are loaded
		HashMap<String, Class<Script>> newScripts = new HashMap<String, Class<Script>>();
		HashMap<String, Long> newScriptDates = new HashMap<String, Long>();
		HashMap<String, File> deletedScripts = new HashMap<String, File>();
		HashMap<String, Collection<String>> scriptDependencies = new HashMap<String, Collection<String>>();
		HashMap<String, Boolean> scriptInits = new HashMap<String, Boolean>();
		List<GroovitySource> traitSources = new ArrayList<>();
		List<GroovitySource> plainSources = new ArrayList<>();
		for(GroovitySource source: sources) {
			try {
				if(traitPattern.matcher(source.getSourceCode()).find()) {
					traitSources.add(source);
					continue;
				}
			} catch (IOException e) {
			}
			plainSources.add(source);
		}
		if(!traitSources.isEmpty()) {
			compileLoop(newScripts, newScriptDates, deletedScripts, scriptDependencies, scriptInits, force, init, 0, compilerTraits, traitSources.toArray(new GroovitySource[0]));
		}
		if(!plainSources.isEmpty()) {
			compileLoop(newScripts, newScriptDates, deletedScripts, scriptDependencies, scriptInits, force, init, 0, compilerTraits, plainSources.toArray(new GroovitySource[0]));
		}
		List<Class<Script>> toDestroy = new ArrayList<Class<Script>>();
		HashSet<String> sourceNames = new HashSet<>();
		for(GroovitySource source: sources) {
			sourceNames.add(getScriptName(source.getPath()));
		}
		List<String> sortedScripts = sortDependencies(scriptDependencies,scriptInits);
		//Now cycle through loaded classes for initialization, etc.
		// load order for dependencies!
		for(String scriptName:sortedScripts){
			String scriptNameFixed= fixCase(scriptName);
			Class<Script> theClass = newScripts.get(scriptName);
			if(init) {
				initClass(theClass);
			}
			scriptDates.put(scriptNameFixed,newScriptDates.get(scriptName));
			Class<Script> oldClass = scripts.put(scriptNameFixed,theClass);
			if(oldClass!=null){
				toDestroy.add(oldClass);
			}
			else if(embeddedScripts.containsKey(scriptNameFixed)) {
				toDestroy.add(embeddedScripts.get(scriptNameFixed));
			}
		}
		for(Map.Entry<String, File> deleted: deletedScripts.entrySet()){
			String name = deleted.getKey();
			String nameCaseFixed = fixCase(name);
			Class<Script> oldClass = scripts.remove(nameCaseFixed);
			scriptDates.remove(nameCaseFixed);
			if(oldClass!=null){
				log.info("Deleting removed source "+name+" / class "+oldClass.getName());
				toDestroy.add(oldClass);
				Class<Script> embeddedClass= embeddedScripts.get(nameCaseFixed);
				if(embeddedClass !=null && init) {
					initClass(embeddedClass);
					newScripts.put(nameCaseFixed, embeddedClass);
				}
			}
			if(deleted.getValue()!=null){
				deleted.getValue().delete();
			}
		}
		if(init) {
			//now destroy
			for(Class<Script> del: toDestroy){
				GroovityClassLoader gcl = ((GroovityClassLoader)del.getClassLoader());
				// notify listeners
				if(observers!=null){
					for(GroovityObserver listener:observers){
						try {
							listener.scriptDestroy(this,gcl.getScriptName(),del);
						}
						catch(Throwable th) {
							log.log(Level.WARNING, "Error in GroovityObserver.scriptDestroy() for "+gcl.getScriptName(), th);
						}
					}
				}
				gcl.destroy();
			}
			if(started.get()) {
				//now start new scripts
				newScripts.values().forEach(cls ->{
					startClass(cls);
				});
			}
		}
		Map<String,Throwable> errors = new LinkedHashMap<String, Throwable>();
		for(Entry<String, GroovityCompilerEvent> entry: getCompilerEvents().entrySet()){
			if(entry.getValue().getError()!=null){
				if(sourceNames.contains(entry.getKey())) {
					errors.put(entry.getKey(), entry.getValue().getError());
				}
			}
		}
		if(errors.size()>0){
			StringBuilder messageBuilder = new StringBuilder(String.valueOf(errors.size())).append(" script");
			if(errors.size()>1) {
				messageBuilder.append("s");
			}
			messageBuilder.append(" failed to compile");
			for(Entry<String, Throwable> entry: errors.entrySet()){
				messageBuilder.append(LINE_SEPARATOR);
				messageBuilder.append(LINE_SEPARATOR);
				messageBuilder.append("Error compiling ").append(entry.getKey()).append(".grvt :");
				messageBuilder.append(LINE_SEPARATOR);
				messageBuilder.append(LINE_SEPARATOR);
				messageBuilder.append(entry.getValue().getMessage());
			}
			log.severe(messageBuilder.toString());
		}
	}
	@SuppressWarnings("rawtypes")
	private void compileLoop(Map<String, Class<Script>> newScripts, 
			Map<String, Long> newScriptDates, 
			Map<String, File> deletedScripts, 
			Map<String, Collection<String>> scriptDependencies,
			Map<String, Boolean> scriptInits,
			boolean force, 
			boolean init, 
			int numErrors, 
			ConcurrentHashMap<String, Class> compilerTraits, 
			GroovitySource... sources){
		LinkedHashMap<String,GroovitySource> errorSources = new LinkedHashMap<>();
		for(GroovitySource source: sources){
			try{
			GroovityCompilerEvent event = new GroovityCompilerEvent();
			event.setPath(source.getPath());
			event.setTime(System.currentTimeMillis());
			String name = getScriptName(source.getPath());
			String nameCaseFixed = fixCase(name);
			String className = name.replaceAll("_", "___").replaceAll("/", "_");
			//System.out.println("Compiling source "+source.getPath()+" "+name+" "+className);
			if(source.exists()){
				if(scripts.containsKey(nameCaseFixed)){
					event.setChange(Change.update);
				}
				else{
					event.setChange(Change.add);
				}
				if(!force){
					Long cur = scriptDates.get(nameCaseFixed);
					//System.out.println("comparing "+cur+" to "+source.getLastModified()+" for "+name);
					if(cur!=null && cur.longValue()==source.getLastModified()){
						continue;
					}
				}
				String sourceCode = null;
				try{	
					sourceCode = source.getSourceCode();
					//code transformations pre-compile
					TransformedSource transformed = GroovitySourceTransformer.transformSource(sourceCode);
					sourceCode=transformed.source;

					//groovy script compiler
					long time1=System.currentTimeMillis();
					ArrayList<String> dependencies = new ArrayList<String>();
					CompilerConfiguration compilerConfiguration = createCompilerConfiguration(transformed!=null?transformed.sourceLineNumbers:null,dependencies);
					//a classloader only gets the traits that are available when it is created, so we make a copy
					ConcurrentHashMap<String, Class> traitsCopy = new ConcurrentHashMap<>(compilerTraits);
					GroovityClassLoader loader = getParentLoader()!=null? new GroovityClassLoader(source.getPath(),getParentLoader(),compilerConfiguration,this,cacheRefreshExecutor,traitsCopy) : new GroovityClassLoader(source.getPath(),Thread.currentThread().getContextClassLoader(),compilerConfiguration,this,cacheRefreshExecutor,traitsCopy);
					CompilationUnit cu = new CompilationUnit(compilerConfiguration,null,loader);
					SourceUnit su =  new TransformedSourceUnit(className.concat(GROOVITY_SOURCE_EXTENSION), transformed, compilerConfiguration, loader, new ErrorCollector(compilerConfiguration));
					//errorCollector.sourceUnit = su;
					cu.addSource(su);
		
					//Don't compile all or extra class files get generated!
					cu.compile(Phases.CLASS_GENERATION);
					
					@SuppressWarnings("unchecked")
					GroovyClass[] gcs = (GroovyClass[]) cu.getClasses().toArray(new GroovyClass[0]);
					Class<Script> scriptClass=loadGroovyClassesAndFindScript(loader, gcs, compilerTraits, traitsCopy, traits);
					if(scriptClass!=null){
						long time2=System.currentTimeMillis();
						log.info("Compiled Groovy Script: ".concat(source.getPath())+" in "+(time2-time1));
					}
					else{
						log.severe("UHOH!!  Unable to find main class for "+source.getPath());
					}
					if(jarPhases!=null && jarPhases.contains(GroovityPhase.RUNTIME)){
						storeClasses(source, gcs);
					}
					if(log.isLoggable(Level.FINE)){
						log.fine("Registering script class "+scriptClass.getName()+" for name "+name);
					}
					newScripts.put(name,scriptClass);
					newScriptDates.put(name,source.getLastModified());
					scriptDependencies.put(name, dependencies);
					scriptInits.put(name, hasInit((GroovityClassLoader)scriptClass.getClassLoader()));
					if(log.isLoggable(Level.FINE)){
						log.fine("Found dependencies "+dependencies+" for script "+name);
					}
				}
				catch(Throwable th){
					errorSources.put(nameCaseFixed,source);
					log.log(Level.FINE,"Error compiling "+source.getPath(),th);
					if(sourceCode!=null && log.isLoggable(Level.FINE)){
						log.fine("Source code in trouble: \n"+sourceCode);
					}
					event.setError(th);
				}
				finally{
					compileEvents.put(nameCaseFixed, event);
				}
			}
			else{
				//remove from memory and disk
				if(jarDirectory!=null){
					deletedScripts.put(name, getClassesFile(source));
				}
				else{
					deletedScripts.put(name, null);
				}
				if(scripts.containsKey(nameCaseFixed)){
					event.setChange(Change.remove);
					compileEvents.put(fixCase(name), event);
				}
			}
			}
			catch(Exception e){
				log.log(Level.SEVERE,"Error compiling groovy "+source.getPath(),e);
			}
		}
		
		if(!errorSources.isEmpty()) {
			boolean retry = (numErrors==0 || errorSources.size()<numErrors);
			if(!retry) {
				//let's check if the compiling set of traits is missing anything
				for(Map.Entry<String, Class> entry: traits.entrySet()) {
					if(!compilerTraits.containsKey(entry.getKey())) {
						retry=true;
						compilerTraits.put(entry.getKey(), entry.getValue());
					}
				}
			}
			if(retry) {
				if(log.isLoggable(Level.FINE)) {
					log.fine("Retrying error compile on "+errorSources.size());
				}
				//retry failed compiles after all the rest in case they just need to pick up traits
				compileLoop(newScripts, newScriptDates, deletedScripts, scriptDependencies, scriptInits, force,init, errorSources.size(),compilerTraits,errorSources.values().toArray(new GroovitySource[0]));
			}
		}
	}
	
	protected CompilerConfiguration createCompilerConfiguration(Map<Integer,Integer> sourceLineNumbers, Collection<String> initDependencies) {
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		if(scriptBaseClass!=null){
			compilerConfiguration.setScriptBaseClass(scriptBaseClass);
			compilerConfiguration.addCompilationCustomizers(new ImportCustomizer().addImports(scriptBaseClass));
		}
		compilerConfiguration.addCompilationCustomizers(new ImportCustomizer().addStarImports("java.util.concurrent","groovy.transform","java.util.function","com.disney.groovity.model").addImports(ClosureWritable.class.getName(),AsyncChannel.class.getName(),GroovityStatistics.class.getName(),Taggable.class.getName(), Tag.class.getName(), Attr.class.getName(), Function.class.getName(), Arg.class.getName(), SkipStatistics.class.getName()));
		compilerConfiguration.addCompilationCustomizers(new ASTTransformationCustomizer(new GroovityASTTransformation(this,sourceLineNumbers,initDependencies)));
		compilerConfiguration.addCompilationCustomizers(new ASTTransformationCustomizer(new StatsASTTransformation()));
		Map<String,Boolean> options = compilerConfiguration.getOptimizationOptions();
		options.put(CompilerConfiguration.JDK8, true);
		compilerConfigurationDecorators.forEach(decorator -> { decorator.decorate(compilerConfiguration); });
		return compilerConfiguration;
	}

	protected void storeClasses(GroovitySource sourceFile, GroovyClass[] classes) throws IOException{
		if(jarDirectory!=null){
			File f = getClassesFile(sourceFile);
			f.mkdirs();
			f.delete();
			HashSet<String> directories = new HashSet<String>();
			FileOutputStream fos = new FileOutputStream(f);
			try{
				Manifest manifest = new Manifest();
				manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
				JarOutputStream target = new JarOutputStream(fos, manifest);
				JarEntry root = new JarEntry("/");
				target.putNextEntry(root);
				target.closeEntry();
				for(GroovyClass gc: classes){
					//System.out.println("Storing clas "+gc.getName());
					String[] segments = gc.getName().split("\\.");
					if(segments.length>0){
						String dir = "/";
						for(int i=0; i< (segments.length-1);i++){
							dir = dir.concat(segments[i]).concat("/");
							if(directories.add(dir)){
								JarEntry entry = new JarEntry(dir);
								//entry.setTime(sourceFile.getLastModified());
								target.putNextEntry(entry);
								target.closeEntry();
								//System.out.println("Created zip directory entry "+dir);
							}
						}
					}
					JarEntry entry = new JarEntry(gc.getName().replaceAll("\\.", "/").concat(".class"));
					target.putNextEntry(entry);
					target.write(gc.getBytes());
					target.closeEntry();
				}
	
				target.close();
			}
			finally{
				fos.close();
			}
			f.setLastModified(sourceFile.getLastModified());
		}

	}
	protected GroovyClass[] loadGroovyClasses(InputStream jarStream) throws IOException{
		//System.out.println("Loading classes from "+f);
		ArrayList<GroovyClass> out = new ArrayList<GroovyClass>();
		byte[] buf = new byte[8192];
		JarInputStream jar = new JarInputStream(jarStream);
		try{
			JarEntry entry = null;//Enumeration<JarEntry> entries = jar.entries();
			while((entry=jar.getNextJarEntry())!=null){
				if(!entry.isDirectory()){
					//System.out.println("Reading from jar file: "+entry.getName());
					int index = entry.getName().indexOf(".class");
					if(index > 0){
						String name = entry.getName().substring(0,index).replaceAll("/", ".");
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						int c;
						while((c=jar.read(buf))!=-1){
							baos.write(buf, 0, c);
						}
						out.add(new GroovyClass(name,baos.toByteArray()));
						//System.out.println("Loaded groovy class "+name);
					}
				}
			}
		}
		finally{
			jar.close();
		}
		return out.toArray(new GroovyClass[0]);
	}
	
	private File getClassesFile(GroovitySource sourceFile){
		return new File(jarDirectory,sourceFile.getPath()+".jar");
	}

	public ClassLoader getParentLoader() {
		return parentLoader;
	}

	protected void setParentLoader(ClassLoader parentLoader) {
		this.parentLoader = parentLoader;
	}

	public List<GroovityObserver> getObservers() {
		return observers;
	}

	protected void setObservers(List<GroovityObserver> observers) {
		this.observers = observers;
	}
	
	public void addObserver(GroovityObserver observer){
		observers.add(observer);
		visit(observer);
	}

	public GroovitySourceLocator[] getSourceLocators() {
		return sourceLocators;
	}

	protected void setSourceLocators(GroovitySourceLocator... sourceLocators) {
		this.sourceLocators = sourceLocators;
	}

	public EnumSet<GroovityPhase> getSourcePhases() {
		return sourcePhases;
	}

	protected void setSourcePhases(EnumSet<GroovityPhase> sourcePhases) {
		this.sourcePhases = sourcePhases;
	}

	public EnumSet<GroovityPhase> getJarPhases() {
		return jarPhases;
	}

	protected void setJarPhases(EnumSet<GroovityPhase> jarPhases) {
		this.jarPhases = jarPhases;
	}

	public Taggables getTaggables() {
		return tagLib;
	}

	protected void setTaggables(Taggables tagLib) {
		this.tagLib = tagLib;
	}

	public HttpClient getHttpClient() {
		return httpClient;
	}

	protected void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}
	
	public List<GroovitySource> getChangedSources(){
		ArrayList<GroovitySource> changedSources = new ArrayList<GroovitySource>();
		if(sourceLocators!=null){
			ArrayList<String> currentScripts = new ArrayList<String>(((Map<String, Long>)scriptDates).keySet());
			for(GroovitySourceLocator sourceLocator:sourceLocators){
				for(GroovitySource source: sourceLocator){
					String key = fixCase(source.getPath());
					if(key.endsWith(GROOVITY_SOURCE_EXTENSION)){
						key = key.substring(0,key.length()-5);
					}
					if(scriptDates.containsKey(key)){
						currentScripts.remove(key);
						if(!source.exists() || source.getLastModified()!=scriptDates.get(key)){
							changedSources.add(source);
						}
					}
					else if(source.exists()){
						changedSources.add(source);
					}
				}
			}
			for(final String script:currentScripts){
				//these have been deleted
				Class<Script> delClass = scripts.get(script);
				final String path = getSourcePath(delClass);
				changedSources.add(new GroovitySource() {
					
					public String getSourceCode() throws IOException {
						return "";
					}
					
					public String getPath() {
						return path;
					}
					
					public long getLastModified() {
						return System.currentTimeMillis();
					}
					
					public boolean exists() {
						return false;
					}
				});
			}
		}
		return changedSources;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List getTraitDocs() {
		ArrayList docs = new ArrayList();
		ArrayList<Map.Entry<String,Class>> entries = new ArrayList<>();
		entries.addAll(traits.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String,Class>>() {
			public int compare(Entry<String, Class> o1,
					Entry<String, Class> o2) {
				return o1.getKey().compareTo(o2.getKey());
			}
		});
		for(Map.Entry<String,Class> entry: entries){
			Class traitClass = entry.getValue();
			docs.add(makeClassDoc(traitClass));
		}
		return docs;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Map makeClassDoc(Class clazz) {
		ClassDescriptor descriptor = new ClassDescriptor(clazz);
		Map cmodel = new LinkedHashMap();
		cmodel.put(NAME, clazz.getName());
		if(descriptor.getSuperType()!=null) {
			cmodel.put(EXTENDS, TypeLabel.get(descriptor.getSuperType()));
		}
		if(descriptor.getInterfaces()!=null && descriptor.getInterfaces().length>0) {
			List<String> interfaces = new ArrayList<>();
			for(Type t: descriptor.getInterfaces()) {
				interfaces.add(TypeLabel.get(t));
			}
			cmodel.put(IMPLEMENTS, interfaces);
		}
		List<TypedName> props = descriptor.getProperties();
		if(props!=null){
			List cfields = new ArrayList();
			for(TypedName prop: props){
				if(!BINDING.equals(prop.name)){
					Map pmap = new LinkedHashMap();
					pmap.put(NAME, prop.name);
					pmap.put(TYPE, TypeLabel.get(prop.type));
					cfields.add(pmap);
				}
			}
			if(cfields.size()>0){
				cmodel.put(PROPERTIES, cfields);
			}
		}
		List<Method> mtds = descriptor.getMethods();
		if(mtds!=null){
			List mlist = new ArrayList();
			for(Method desc:mtds){
				if(!internalMethodNames.contains(desc.getName()) && !desc.getName().contains("__")){
					Map mmap = new HashMap();
					mmap.put(NAME, desc.getName());
					mmap.put(MODIFIERS,Modifier.toString(desc.getModifiers()));
					Function func = desc.getAnnotation(Function.class);
					if(func!=null){
						mmap.put(INFO, func.info());
					}
					Type[] pts = desc.getGenericParameterTypes();
					Annotation[][] anns = desc.getParameterAnnotations();
					if(pts!=null && pts.length>0){
						List parms = new ArrayList();
						for(int i=0;i<pts.length;i++){
							Map pm = new LinkedHashMap();
							Annotation[] aa = anns[i];
							String name = "arg"+(i+1);
							for(Annotation a:aa){
								if(a instanceof Arg){
									name = ((Arg)a).name();
									pm.put(INFO, ((Arg)a).info());
									break;
								}
							}
							pm.put(NAME,name);
							pm.put(TYPE, TypeLabel.get(pts[i]));
							parms.add(pm);
						}
						mmap.put(PARAMETERS, parms);
					}
					Type returnType = desc.getGenericReturnType();
					if(returnType!=null){
						mmap.put(RETURNS, TypeLabel.get(returnType));
					}
					mlist.add(mmap);
				}
			}
			cmodel.put(METHODS, mlist);
		}
		return cmodel;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List getLibraryDocs(){
		ArrayList docs = new ArrayList();
		ArrayList<Map.Entry<String,Class<Script>>> entries = new ArrayList<Map.Entry<String,Class<Script>>>();
		entries.addAll(embeddedScripts.entrySet());
		entries.addAll(scripts.entrySet());
		//alphabetize for consistency
		Collections.sort(entries, new Comparator<Map.Entry<String,Class<Script>>>() {
			public int compare(Entry<String, Class<Script>> o1,
					Entry<String, Class<Script>> o2) {
				return o1.getKey().compareTo(o2.getKey());
			}
		});
		for(Map.Entry<String,Class<Script>> entry: entries){
			Boolean isLibrary = Boolean.FALSE;
			Class scriptClass = entry.getValue();
			try {
				isLibrary = (Boolean) scriptClass.getField(IS_GROOVITY_LIBRARY).get(scriptClass);
			} catch (Exception e) {
			}
			if(isLibrary.booleanValue()){
				String sourcePath = getSourcePath(scriptClass);
				String className = scriptClass.getName();
				if(className.startsWith("_")){
					className = className.substring(1);
				}
				HashSet<Class> publicClasses = new HashSet<Class>();
				LinkedHashMap libModel = new LinkedHashMap();
				libModel.put(NAME, className);
				libModel.put(PATH,sourcePath);
				List functions = new ArrayList();
				libModel.put(FUNCTIONS, functions);
				Method[] methods = entry.getValue().getDeclaredMethods();
				Arrays.sort(methods,new Comparator<Method>() {
					public int compare(Method o1, Method o2) {
						int o = o1.getName().compareTo(o2.getName());
						if(o==0){
							o = o1.getParameterTypes().length-o2.getParameterTypes().length;
						}
						return o;
					}
				});
				for(Method method: methods){
					Function func = method.getAnnotation(Function.class);
					if(func!=null && !internalMethodNames.contains(method.getName())){
						LinkedHashMap funcModel = new LinkedHashMap();
						funcModel.put(NAME, method.getName());
						funcModel.put(INFO, func.info());
						functions.add(funcModel);
						//add argument info
						Type[] pts = method.getGenericParameterTypes();
						Annotation[][] panns = method.getParameterAnnotations();
						if(pts!=null && pts.length>0 && pts.length==panns.length){
							List params = new ArrayList();
							funcModel.put(ARGS, params);
							for(int i=0;i<pts.length;i++){
								Type type = pts[i];
								LinkedHashMap argModel = new LinkedHashMap();
								params.add(argModel);
								Annotation[] arganns = panns[i];
								for(Annotation a: arganns){
									if(a.annotationType().equals(Arg.class)){
										Arg arg = (Arg) a;
										argModel.put(NAME, arg.name());
										argModel.put(INFO,arg.info());
										argModel.put(NULLABLE,arg.nullable());
									}
								}
								collectClasses(type, publicClasses, scriptClass.getClassLoader());
								argModel.put(TYPE, TypeLabel.get(type));
							}
						}
						//add return type
						Type returnType = method.getGenericReturnType();
						if(returnType!=null){
							collectClasses(returnType, publicClasses, scriptClass.getClassLoader());
							funcModel.put(RETURNS, TypeLabel.get(returnType));
						}
					}					
				}
				if(publicClasses.size()>0){
					List classDocs = new ArrayList();
					libModel.put(CLASSES, classDocs);
					for(Class c: publicClasses){
						classDocs.add(makeClassDoc(c));
					}
				}
				docs.add(libModel);
			}
		}
		return docs;
	}

	private void collectClasses(Type type, @SuppressWarnings("rawtypes") Collection<Class> collection, ClassLoader classLoader){
		if(type instanceof ParameterizedType){
			ParameterizedType pType = ((ParameterizedType)type);
			collectClasses(pType.getRawType(), collection, classLoader);
			Type[] args = pType.getActualTypeArguments();
			for(int i=0;i<args.length;i++){
				collectClasses(args[i], collection, classLoader);
			}
		}
		else if(type instanceof Class){
			@SuppressWarnings("rawtypes")
			Class cType = ((Class)type);
			if(cType.isArray()){
				cType = cType.getComponentType();
			}
			if(cType.getClassLoader()==classLoader){
				if(collection.add(cType)) {
					Method[] cm = cType.getDeclaredMethods();
					for(Method method: cm){
						Type[] pts = method.getGenericParameterTypes();
						if(pts!=null){
							for(Type pt: pts){
								collectClasses(pt, collection, classLoader);
							}
						}
						Type returnType = method.getGenericReturnType();
						if(returnType!=null){
							collectClasses(returnType, collection, classLoader);
						}
					}
				}
			}
		}
	}
	
	public int getAsyncThreads() {
		return asyncThreads;
	}

	protected void setAsyncThreads(int asyncThreads) {
		this.asyncThreads = asyncThreads;
	}
	
	public ExecutorService getAsyncExecutor(){
		return asyncExecutor;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	protected void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}
	
	protected String fixCase(String in){
		return caseSensitive ? in : in.toLowerCase();
	}
	
	public BindingDecorator getBindingDecorator() {
		return bindingDecorator;
	}

	protected void setBindingDecorator(BindingDecorator bindingDecorator) {
		this.bindingDecorator = bindingDecorator;
	}

	public String getScriptBaseClass() {
		return scriptBaseClass;
	}

	protected void setScriptBaseClass(String scriptBaseClass) {
		this.scriptBaseClass = scriptBaseClass;
	}

	private static class TransformedSourceUnit extends SourceUnit{
		final TransformedSource transformedSource;

		public TransformedSourceUnit(String name, TransformedSource source,
				CompilerConfiguration configuration, GroovyClassLoader loader,
				ErrorCollector er) {
			super(name, source.source, configuration, loader, er);
			this.transformedSource=source;
		}
		public String getSample(int line, int column, Janitor janitor){
			if(line < 0){
				return null;
			}
			String sample = null;
			BufferedReader reader = new BufferedReader(new StringReader(transformedSource.originalSource));
			int c = 0;
			String text="";
			while(c!=line){
				try {
					text = reader.readLine();
				} catch (IOException e) {
				}
				c++;
			}
	        if (text != null) {
	        		text = text.replaceAll("\\t", " ");
	            if (column > 0) {
	                String marker = Utilities.repeatString(" ", column - 1) + "^";

	                if (column > 40 && column <=text.length()) {
	                    int start = column - 30 - 1;
	                    int end = (column + 10 > text.length() ? text.length() : column + 10 - 1);
	                    sample = "   " + text.substring(start, end) + Utilities.eol() + "   " +
	                            marker.substring(start, marker.length());
	                } else {
	                    sample = "   " + text + Utilities.eol() + "   " + marker;
	                }
	            } else {
	                sample = text;
	            }
	        }

	        return sample;
		}
	}

	public Configurator getConfigurator() {
		return configurator;
	}

	protected void setConfigurator(Configurator configurator) {
		this.configurator = configurator;
	}
	
	public void addConfigurator(Configurator toAdd){
		if(this.configurator==null){
			this.configurator = new MultiConfigurator(new CopyOnWriteArrayList<Configurator>());
		}
		if(!(configurator instanceof MultiConfigurator)){
			CopyOnWriteArrayList<Configurator> nc = new CopyOnWriteArrayList<Configurator>();
			nc.add(configurator);
			this.configurator = new MultiConfigurator(nc);
		}
		toAdd.init();
		((MultiConfigurator)configurator).getConfigurators().add(toAdd);
		configureAll();
	}

	public ArgsLookup getArgsLookup() {
		return argsLookup;
	}

	protected void setArgsLookup(ArgsLookup argsLookup) {
		this.argsLookup = argsLookup;
	}

	public InterruptFactory getInterruptFactory() {
		return interruptFactory;
	}

}

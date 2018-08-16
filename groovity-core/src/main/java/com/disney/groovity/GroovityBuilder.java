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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.impl.client.HttpClientBuilder;

import com.disney.groovity.conf.Configurator;
import com.disney.groovity.conf.EnvConfigurator;
import com.disney.groovity.conf.MultiConfigurator;
import com.disney.groovity.conf.PropertiesFileConfigurator;
import com.disney.groovity.conf.PropertiesResourceConfigurator;
import com.disney.groovity.conf.PropertiesURLConfigurator;
import com.disney.groovity.conf.SystemConfigurator;
import com.disney.groovity.source.AbstractGroovitySourceLocator;
import com.disney.groovity.source.ClasspathGroovitySourceLocator;
import com.disney.groovity.source.FileGroovitySourceLocator;
import com.disney.groovity.source.GroovitySourceLocator;
import com.disney.groovity.source.HttpGroovitySourceLocator;

/**
 * A fluent builder API to configure and instantiate a Groovity.  Each of the setter
 * methods has comments describing the purpose of that option.
 * 
 * @author Alex Vigdor
 *
 */
public class GroovityBuilder {
	private ArgsLookup argsLookup = null;
	private int asyncThreads = 128;
	private Map<String,Object> defaultBinding;
	private BindingDecorator bindingDecorator;
	private boolean caseSensitive = true;
	private int maxHttpConnPerRoute = 128;
	private int maxHttpConnTotal = 512;
	private File jarDirectory = null;
	private EnumSet<GroovityPhase> sourcePhases = EnumSet.of(GroovityPhase.STARTUP);
	private EnumSet<GroovityPhase> jarPhases = EnumSet.of(GroovityPhase.STARTUP);
	private Collection<URI> sourceLocations = null;
	private Collection<GroovitySourceLocator> sourceLocators = null;
	private int sourcePollSeconds = -1;
	private String scriptBaseClass = "com.disney.groovity.GroovityScript";
	private HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().useSystemProperties();
	private ClassLoader parentClassLoader;
	private Configurator configurator;
	private String propsResource;
	private File propsFile;
	private URL propsURL;
	
	public Map<String,Object> getDefaultBinding() {
		return defaultBinding;
	}
	/**
	 * Add a map of default binding values to be provided to all scripts in this groovity
	 * 
	 * @param defaultBinding
	 * @return
	 */
	public GroovityBuilder setDefaultBinding(Map<String,Object> defaultBinding) {
		this.defaultBinding = defaultBinding;
		return this;
	}
	public int getAsyncThreads() {
		return asyncThreads;
	}
	/**
	 * Set the maximum number of threads available for async HTTP client calls
	 * 
	 * @param asyncThreads
	 * @return
	 */
	public GroovityBuilder setAsyncThreads(int asyncThreads) {
		this.asyncThreads = asyncThreads;
		return this;
	}
	public boolean isCaseSensitive() {
		return caseSensitive;
	}
	/**
	 * Set to false to enable case insensitive paths for loading and running scripts
	 * @param caseSensitive
	 * @return
	 */
	public GroovityBuilder setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
		return this;
	}
	public int getMaxHttpConnPerRoute() {
		return maxHttpConnPerRoute;
	}
	/**
	 * Set the maximum number of http client connections per route
	 * 
	 * @param maxHttpConnPerRoute
	 * @return
	 */
	public GroovityBuilder setMaxHttpConnPerRoute(int maxHttpConnPerRoute) {
		this.maxHttpConnPerRoute = maxHttpConnPerRoute;
		return this;
	}
	public int getMaxHttpConnTotal() {
		return maxHttpConnTotal;
	}
	/**
	 * Set max total number of simultaneous http client connections
	 * @param maxHttpConnTotal
	 * @return
	 */
	public GroovityBuilder setMaxHttpConnTotal(int maxHttpConnTotal) {
		this.maxHttpConnTotal = maxHttpConnTotal;
		return this;
	}
	public File getJarDirectory() {
		return jarDirectory;
	}
	/**
	 * Define the directory where JAR files are read/written (according to JarPhases)
	 * @param jarDirectory
	 * @return
	 */
	public GroovityBuilder setJarDirectory(File jarDirectory) {
		this.jarDirectory = jarDirectory;
		return this;
	}
	public EnumSet<GroovityPhase> getSourcePhases() {
		return sourcePhases;
	}
	/**
	 * Set source phases (STARTUP = compile from source at startup, RUNTIME = automatically compile changes at runtime)
	 * @param sourcePhases
	 * @return
	 */
	public GroovityBuilder setSourcePhases(EnumSet<GroovityPhase> sourcePhases) {
		this.sourcePhases = sourcePhases;
		return this;
	}
	public EnumSet<GroovityPhase> getJarPhases() {
		return jarPhases;
	}
	/**
	 * Set jar phases (STARTUP = read from JAR files at startup, RUNTIME = write out JAR files during compilation)
	 * @param jarPhases
	 * @return
	 */
	public GroovityBuilder setJarPhases(EnumSet<GroovityPhase> jarPhases) {
		this.jarPhases = jarPhases;
		return this;
	}
	/**
	 * Convenience method for setting source phases as a String, full support = "STARTUP,RUNTIME"
	 * @param phases
	 * @return
	 */
	public GroovityBuilder setSourcePhase(String phases){
		String[] ph = phases.split("\\s*,\\s*");
		EnumSet<GroovityPhase> es = EnumSet.noneOf(GroovityPhase.class);
		if(ph[0].length()>0){
			for(String p: ph){
				es.add(GroovityPhase.valueOf(p));
			}
		}
		sourcePhases=es;
		return this;
	}
	/**
	 * Convenience method for setting jar phases as a string, full JAR support = "STARTUP,RUNTIME"
	 * @param phases
	 * @return
	 */
	public GroovityBuilder setJarPhase(String phases){
		String[] ph = phases.split("\\s*,\\s*");
		EnumSet<GroovityPhase> es = EnumSet.noneOf(GroovityPhase.class);
		if(ph[0].length()>0){
			for(String p: ph){
				es.add(GroovityPhase.valueOf(p));
			}
		}
		jarPhases=es;
		return this;
	}
	public Collection<URI> getSourceLocations() {
		return sourceLocations;
	}
	/**
	 * Set locations where source files can be listed and retrieved
	 * 
	 * @param sourceLocations
	 * @return
	 */
	public GroovityBuilder setSourceLocations(Collection<URI> sourceLocations) {
		this.sourceLocations = sourceLocations;
		return this;
	}
	/**
	 * Set file or HTTP locations where source files can be listed and retrieved
	 * 
	 * @param sourceLocations
	 * @return
	 */
	public GroovityBuilder setSourceLocations(URI... sourceLocations) {
		this.sourceLocations = Arrays.asList(sourceLocations);
		return this;
	}
	/**
	 * After setting all initialization parameters, call build() to construct, initialize and return the fully started Groovity
	 * 
	 * @return an initialized Groovity
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public Groovity build() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, URISyntaxException{
		return build(true);
	}
	/**
	 * After setting all initialization parameters, call build(true) to construct, initialize and return the fully started Groovity,
	 * or build(false) to initialize the Groovity without starting (e.g. to just build jar files without initing and starting classes)
	 * 
	 * @return an initialized Groovity
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public Groovity build(boolean start) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, URISyntaxException{
		CopyOnWriteArrayList<Configurator> configurators = new CopyOnWriteArrayList<Configurator>();
		if(propsResource!=null){
			configurators.add(new PropertiesResourceConfigurator(propsResource));
		}
		if(propsFile!=null){
			configurators.add(new PropertiesFileConfigurator(propsFile));
		}
		if(propsURL!=null){
			configurators.add(new PropertiesURLConfigurator(propsURL));
		}
		configurators.add(new EnvConfigurator());
		configurators.add(new SystemConfigurator());
		if(configurator!=null){
			configurators.add(configurator);
		}
		Groovity groovity = new Groovity();
		groovity.setJarDirectory(jarDirectory);
		groovity.setJarPhases(jarPhases);
		groovity.setSourcePhases(sourcePhases);
		groovity.setCaseSensitive(caseSensitive);
		groovity.setArgsLookup(argsLookup);
		groovity.setAsyncThreads(asyncThreads);
		groovity.setScriptBaseClass(scriptBaseClass);
		groovity.setParentLoader(parentClassLoader);
		groovity.setConfigurator(new MultiConfigurator(configurators));
		final AtomicReference<BindingDecorator> bindingDecoratorRef = new AtomicReference<BindingDecorator>(bindingDecorator);
		if(defaultBinding!=null){
			bindingDecoratorRef.set(new BindingMapDecorator(new ConcurrentHashMap<String,Object>(defaultBinding),bindingDecoratorRef.get()));
		}
		ServiceLoader.load(BindingDecorator.class).forEach(decorator ->{
			decorator.setChainedDecorator(bindingDecoratorRef.get());
			bindingDecoratorRef.set(decorator);
		});
		groovity.setBindingDecorator(bindingDecoratorRef.get());
		if(httpClientBuilder==null){
			httpClientBuilder = HttpClientBuilder.create().useSystemProperties();
		}
		if(maxHttpConnPerRoute>0){
			httpClientBuilder.setMaxConnPerRoute(maxHttpConnPerRoute);
		}
		if(maxHttpConnTotal>0){
			httpClientBuilder.setMaxConnTotal(maxHttpConnTotal);
		}
		groovity.setHttpClient(httpClientBuilder.build());
		List<GroovitySourceLocator> locators = new ArrayList<GroovitySourceLocator>();
		if(sourceLocators!=null){
			for(GroovitySourceLocator locator: sourceLocators){
				locators.add(locator);
			}
		}
		if(sourceLocations!=null){
			for(URI location: sourceLocations){
				AbstractGroovitySourceLocator sourceLocator = null;
				String scheme = location.getScheme();
				if(scheme!=null){
					scheme = scheme.toLowerCase();
					if(scheme.startsWith("http")){
						sourceLocator = new HttpGroovitySourceLocator(location);
					}
					else if(scheme.equals("classpath")){
						sourceLocator = new ClasspathGroovitySourceLocator(location.getPath());
					}
				}
				if(sourceLocator==null){
					//file locator
					if(location.isAbsolute()){
						sourceLocator = new FileGroovitySourceLocator(new File(location));
					}
					else{
						sourceLocator = new FileGroovitySourceLocator(new File(location.getPath()));
					}
				}
				sourceLocator.setInterval(sourcePollSeconds);
				locators.add(sourceLocator);
			}
		}
		groovity.setSourceLocators(locators.toArray(new GroovitySourceLocator[0]));
		if(start) {
			groovity.start();
		}
		else {
			groovity.init(false);
		}
		return groovity;
	}
	public int getSourcePollSeconds() {
		return sourcePollSeconds;
	}
	/**
	 * If RUNTIME source phase is enabled, controls how frequently this Groovity will poll locators for source changes
	 * @param sourcePollSeconds
	 * @return
	 */
	public GroovityBuilder setSourcePollSeconds(int sourcePollSeconds) {
		this.sourcePollSeconds = sourcePollSeconds;
		return this;
	}
	public String getScriptBaseClass() {
		return scriptBaseClass;
	}
	/**
	 * Specify a base class for groovity scripts, defaults to core groovy Script
	 * 
	 * @param scriptBaseClass
	 * @return
	 */
	public GroovityBuilder setScriptBaseClass(String scriptBaseClass) {
		this.scriptBaseClass = scriptBaseClass;
		return this;
	}
	public BindingDecorator getBindingDecorator() {
		return bindingDecorator;
	}
	/**
	 * Define a binding decorator to use with scripts compiled in this Groovity
	 * 
	 * @param bindingDecorator
	 * @return
	 */
	public GroovityBuilder setBindingDecorator(BindingDecorator bindingDecorator) {
		this.bindingDecorator = bindingDecorator;
		return this;
	}
	public HttpClientBuilder getHttpClientBuilder() {
		return httpClientBuilder;
	}
	/**
	 * Pass in a custom HTTPClientBuilder in order to tailor HTTP client behavior, e.g. to add ehcache or custom interceptors
	 * 
	 * @param httpClientBuilder
	 * @return
	 */
	public GroovityBuilder setHttpClientBuilder(HttpClientBuilder httpClientBuilder) {
		this.httpClientBuilder = httpClientBuilder;
		return this;
	}
	public ClassLoader getParentClassLoader() {
		return parentClassLoader;
	}
	/**
	 * Provide a custom parent classloader that all groovity script loaders will inherit from.
	 * 
	 * @param parentClassLoader
	 * @return
	 */
	public GroovityBuilder setParentClassLoader(ClassLoader parentClassLoader) {
		this.parentClassLoader = parentClassLoader;
		return this;
	}
	public Collection<GroovitySourceLocator> getSourceLocators() {
		return sourceLocators;
	}
	/**
	 * Set the GroovitySourceLocators used by this groovity, a lower-level alternative to providing source URIs
	 * 
	 * @param sourceLocators
	 * @return
	 */
	public GroovityBuilder setSourceLocators(Collection<GroovitySourceLocator> sourceLocators) {
		this.sourceLocators = sourceLocators;
		return this;
	}
	public Configurator getConfigurator() {
		return configurator;
	}
	/**
	 * Attach a configurator to this Groovity to provide configuration data to scripts
	 * 
	 * @param configurator
	 * @return
	 */
	public GroovityBuilder setConfigurator(Configurator configurator) {
		this.configurator = configurator;
		return this;
	}
	public String getPropsResource() {
		return propsResource;
	}
	/**
	 * Set a classpath properties file location that acts a source of configuration values for groovity conf blocks
	 * @param propsResource
	 * @return
	 */
	public GroovityBuilder setPropsResource(String propsResource) {
		this.propsResource = propsResource;
		return this;
	}
	public File getPropsFile() {
		return propsFile;
	}
	/**
	 * Set a properties file that acts a source of configuration values for groovity conf blocks
	 * 
	 * @param propsFile
	 * @return
	 */
	public GroovityBuilder setPropsFile(File propsFile) {
		this.propsFile = propsFile;
		return this;
	}
	/**
	 * Set an arbitrary URL for a properties file that acts a source of configuration values for groovity conf blocks
	 * 
	 * @param propsUrl
	 * @return
	 */
	public GroovityBuilder setPropsUrl(URL propsUrl) {
		this.propsURL=propsUrl;
		return this;
	}
	public ArgsLookup getArgsLookup() {
		return argsLookup;
	}
	/**
	 * Define the ArgsLookup implementation to use for this groovity
	 * 
	 * @param argsLookup
	 * @return
	 */
	public GroovityBuilder setArgsLookup(ArgsLookup argsLookup) {
		this.argsLookup = argsLookup;
		return this;
	}
	
}

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
package com.disney.groovity.maven;

import java.io.File;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

import com.disney.groovity.Groovity;
import com.disney.groovity.compile.GroovityCompilerEvent;

import groovy.json.JsonSlurper;

/**
 * Abstract base class for the different Groovity mojos; defines common properties including
 * <ul>
 * <li><b>groovitySourceDirectory</b> - defines the home folder for groovity sources, defaults to src/main/groovity</li>
 * <li><b>failOnError</b> - determines whether groovity compile errors should break the build, defaults to true</li>
 * <li><b>systemProperties</b> - Used to set system properties, for example to satisfy conf blocks for testing.</li>
 * <li><b>applicationContext</b> - Specify path to a spring application context XML file that defines a BindingDecorator if needed for compilation/testing, defaults to none</li>
 * </ul>
 * A magic system property groovity.defaultBinding can also be used to pass in a JSON blob of literals to populate the default binding.
 * 
 * @author Alex Vigdor
 */
public abstract class AbstractGroovityMojo extends AbstractMojo {

	@Parameter( defaultValue = "${project}", readonly = true, required=true )
	protected MavenProject project;
	
	@Parameter( defaultValue = "${session}", readonly = true, required=true )
	protected MavenSession session;

	@Parameter(defaultValue="src/main/groovity")
	protected File groovitySourceDirectory;
	
	@Parameter(defaultValue="true")
	protected boolean failOnError;

	@Parameter
	protected Map<String, String> systemProperties;
	
	protected Map<String, Object> defaultBinding;
	
	protected void validateFactory(Groovity factory) throws MojoFailureException{
		Map<String, GroovityCompilerEvent> events = factory.getCompilerEvents();
		Map<String,Throwable> errors = new LinkedHashMap<String, Throwable>();
		for(Entry<String, GroovityCompilerEvent> entry: events.entrySet()){
			if(entry.getValue().getError()!=null){
				errors.put(entry.getKey(), entry.getValue().getError());
			}
		}
		if(errors.size()>0){
			StringBuilder messageBuilder = new StringBuilder("There were ");
			messageBuilder.append(String.valueOf(errors.size())).append(" compiler errors");
			//for(Entry<String, Throwable> entry: errors.entrySet()){
			//	messageBuilder.append("\n\t").append(entry.getKey()).append(": ").append(entry.getValue().getMessage());
			//}
			throw new MojoFailureException(messageBuilder.toString());
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void populateSystemProperties() throws ExpressionEvaluationException{
		PluginParameterExpressionEvaluator evaluator = new PluginParameterExpressionEvaluator(session);
		if(systemProperties!=null){
			for(String key: systemProperties.keySet()){
				Object val = evaluator.evaluate(systemProperties.get(key));
				if(val!=null){
					System.setProperty(key, val.toString());
					if(getLog().isDebugEnabled()){
						getLog().debug("Setting system property "+key+" = "+val);
					}
					if("groovity.defaultBinding".equals(key)){
						defaultBinding= (Map<String, Object>) new JsonSlurper().parse(new StringReader(val.toString()));
					}
				}
			}
		}
	}

	protected ClassLoader createClassLoader( ClassLoaderScope scope) throws MojoExecutionException, DependencyResolutionRequiredException{
		List<String> classpathElements = null;
		if(scope == ClassLoaderScope.COMPILE){
			classpathElements = project.getCompileClasspathElements();
		}
		else if(scope == ClassLoaderScope.TEST){
			classpathElements = project.getTestClasspathElements();
		}
		else{
			throw new MojoExecutionException("phase for class loader must be TEST or COMPILE, not "+scope);
		}
		Set<URL> projectClasspathList = new HashSet<URL>();
		for (String element : classpathElements) {
			try {
				projectClasspathList.add(new File(element).toURI().toURL());
			} catch (MalformedURLException e) {
				throw new MojoExecutionException(element + " is an invalid classpath element", e);
			}
		};
		URLClassLoader loader = new URLClassLoader(projectClasspathList.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
		return loader;
	}
}

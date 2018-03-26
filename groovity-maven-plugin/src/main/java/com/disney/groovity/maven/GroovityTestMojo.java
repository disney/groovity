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

import groovy.lang.Closure;
import groovy.lang.Script;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityConstants;
import com.disney.groovity.ScriptBody;
import com.disney.groovity.compile.GatherStatistics;
import com.disney.groovity.compile.GroovityClassLoader;
import com.disney.groovity.servlet.container.GroovityServletContainer;
import com.disney.groovity.servlet.container.GroovityServletContainerBuilder;
import com.disney.groovity.source.FileGroovitySourceLocator;
import com.disney.groovity.source.GroovitySource;
import com.disney.groovity.source.GroovitySourceLocator;
import com.disney.groovity.stats.GroovityStatistics;
import com.disney.groovity.stats.GroovityStatistics.Statistics;
import com.disney.groovity.util.TypeLabel;

/**
 * The test mojo executes test scripts during the maven test lifecycle phase,
 * and for servlet projects spins up a local webserver available for testing
 * using HTTP methods. It supports 3 additional configuration options:
 * <ul>
 * <li><b>groovityTestDirectory</b> - defines the home folder for groovity test
 * scripts, defaults to src/test/groovity</li>
 * 
 * <li><b>port</b> - defines the HTTP port for testing groovity-servlet
 * projects, defaults to 9880</li>
 * 
 * <li><b>path</b> - Optionally specify a single or comma-separated list of test
 * paths to execute (defaults to executing all tests)</li>
 * </ul>
 * <p>
 * Interactive test development
 * <p>
 * The test mojo also offers an interactive mode; in this mode the test
 * environment starts up and initializes, and waits for command line
 * instructions to execute individual or all tests in an iterative manner, where
 * you can tweak test or app code, compile and rerun tests without the overhead
 * of a complete shutdown/startup. This facilitates rapid test development and
 * test-driven development, especially for complex applications with significant
 * startup or test preparation costs. To enter interactive test mode set the
 * system property interactive=true:
 * <p>
 * 
 * <pre>
 * mvn test -Dinteractive=true
 * </pre>
 * 
 * @author Alex Vigdor
 */
@Mojo(defaultPhase = LifecyclePhase.TEST, name = "test", requiresDependencyResolution = ResolutionScope.TEST)
public class GroovityTestMojo extends AbstractGroovityMojo {
	@Parameter
	private File[] sources;
	@Parameter(defaultValue = "src/test/groovity")
	private File groovityTestDirectory;
	@Parameter(defaultValue = "-1")
	private String port;
	@Parameter(property = "path")
	private String path;
	@Parameter(property = "interactive")
	private boolean interactive = false;
	@Parameter( property = "skipTests", defaultValue = "false" )
    private boolean skipTests;
	@Parameter( property = "maven.test.skip", defaultValue = "false" )
    private boolean skip;

	protected String getClassLabel(Class<?> theClass){
		String name = theClass.getName();
		StringBuilder builder = new StringBuilder();
		String[] parts = name.split("___",-1);
		for(int i=0;i<parts.length;i++){
			if(i>0){
				builder.append("_");
			}
			builder.append(parts[i].replaceAll("_", "/"));
		}
		return builder.toString();
	}
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			if(skip) {
				return;
			}
			getLog().info("STARTING Groovity test");
			populateSystemProperties();
			ClassLoader testLoader = createClassLoader(ClassLoaderScope.TEST);
			GroovityServletContainerBuilder builder = new GroovityServletContainerBuilder()
					.setPort(Integer.valueOf(port))
					.setWebappDirectory(new File(project.getBasedir(), "src/main/webapp")).setClassLoader(testLoader);
			if (groovitySourceDirectory != null && groovitySourceDirectory.exists()) {
				builder.addSourceLocation(groovitySourceDirectory.toURI(), true);
			}
			if (groovityTestDirectory != null && groovityTestDirectory.exists()) {
				builder.addSourceLocation(groovityTestDirectory.toURI(), true);
			}
			if (sources != null) {
				for (File source : sources) {
					builder.addSourceLocation(source.toURI(), true);
				}
			}
			GroovityStatistics.reset();
			GroovityServletContainer container = builder.build();
			container.start();
			Groovity groovity = container.getGroovity();
			ArrayList<String> appSources = new ArrayList<String>();
			try {
				if(failOnError) {
					validateFactory(groovity);
				}
				if(skipTests) {
					return;
				}
				GroovitySourceLocator[] sourceLocators = groovity.getSourceLocators();
				for (GroovitySourceLocator locator : sourceLocators) {
					if (!interactive
							&& ((FileGroovitySourceLocator) locator).getDirectory().equals(groovityTestDirectory)) {
						if (path != null) {
							String[] pathParts = path.split("\\s*,\\s*");
							for (String pathPart : pathParts) {
								container.run(pathPart);
							}
						} else {
							for (GroovitySource source : locator) {
								String scriptPath = source.getPath();
								scriptPath = scriptPath.substring(0, scriptPath.lastIndexOf("."));
								String scriptName = scriptPath.substring(scriptPath.lastIndexOf('/') + 1);
								if (scriptName.startsWith("test")) {
									container.run(scriptPath);
								}
							}
						}
					}
					if (((FileGroovitySourceLocator) locator).getDirectory().equals(groovitySourceDirectory)) {
						for (GroovitySource source : locator) {
							appSources.add(source.getPath());
						}
					}
				}
				if (interactive) {
					// in interactive mode we wait for instructions and compile each time to allow
					// real-time development
					container.enterConsole();
				}
			} finally {
				container.stop();
			}
			Map<String, CodeCoverage> coverageMap = new TreeMap<String, GroovityTestMojo.CodeCoverage>();
			Collection<Class<Script>> scriptClasses = groovity.getGroovityScriptClasses();
			for (Class<Script> sc : scriptClasses) {
				String sourcePath = groovity.getSourcePath(sc);
				if (appSources.contains(sourcePath)) {
					String scriptLabel = sourcePath.substring(0, sourcePath.length() - 5);
					CodeCoverage cc = new CodeCoverage(sc, scriptLabel);
					if(cc.isCoverable()) {
						coverageMap.put(scriptLabel, cc);
					}
					for(Class<?> c: ((GroovityClassLoader)sc.getClassLoader()).getLoadedClasses()) {
						if(!c.equals(sc) && !(Closure.class.isAssignableFrom(c)) && !c.isInterface()) {
							String cname = getClassLabel(c);
							int p = 0;
							if((p=cname.indexOf("$Trait"))>0) {
								cname = cname.substring(0,p);
							}
							String classLabel = scriptLabel+"->"+cname;
							CodeCoverage icc = new CodeCoverage(c, classLabel);
							if(icc.isCoverable()) {
								coverageMap.put(classLabel, icc);
							}
						}
					}
				}
			}
			List<Statistics> stats = GroovityStatistics.getStatistics();
			for (Statistics stat : stats) {
				String sks = stat.key.toString();
				int dot = sks.indexOf(".");
				if (dot > 0) {
					String className = sks.substring(0, dot);
					String method = sks.substring(dot + 1);
					CodeCoverage cc = coverageMap.get(className);
					if (cc != null) {
						if (method.equals("run()") && cc.runnable) {
							cc.ran = true;
						} else {
							if (cc.methods.containsKey(method)) {
								cc.methods.put(method, true);
							}
						}
					}
				}
			}
			Collection<CodeCoverage> ccs = coverageMap.values();
			double total = 0;
			for (CodeCoverage cc : ccs) {
				total += cc.getCoverage();
			}
			total /= ccs.size();
			getLog().info("TEST COVERAGE " + ((int) (100 * total)) + "% TOTAL");
			for (Entry<String, CodeCoverage> entry : coverageMap.entrySet()) {
				CodeCoverage cc = entry.getValue();
				double covered = cc.getCoverage();
				getLog().info(
						" " + ((int) (100 * covered)) + "% coverage for " + cc.label);
				if (covered < 1.0) {
					if (cc.runnable && !cc.ran) {
						getLog().warn("   Script body did not run during tests");
					}
					List<String> uncovered = cc.getUncoveredMethods();
					if (!uncovered.isEmpty()) {
						for (String m : cc.getUncoveredMethods()) {
							getLog().warn("   " + m + " did not execute during tests");
						}
					}
				}
				/*
				for(String m: cc.getCoveredMethods()) {
					getLog().info("   " + m + " executed during tests");
				}
				*/
			}
		} catch(MojoFailureException e) {
			throw e;
		} catch (Throwable e) {
			getLog().error("ERROR in Groovity test", e);
			throw new MojoFailureException(e.getMessage());
		}
	}

	private static class CodeCoverage implements GroovityConstants {
		boolean runnable = false;
		boolean ran = false;
		Map<String, Boolean> methods;
		String label;

		protected CodeCoverage(Class<?> scriptClass, String label) {
			if (ScriptBody.class.isAssignableFrom(scriptClass)) {
				runnable = true;
			}
			this.label=label;
			methods = new TreeMap<String, Boolean>();
			Method[] dm = scriptClass.getDeclaredMethods();
			for (Method m : dm) {
				if (m.getAnnotation(GatherStatistics.class)!=null) {
					StringBuilder builder = new StringBuilder(m.getName());
					builder.append("(");
					String delim = "";
					for (Type t : m.getGenericParameterTypes()) {
						if(scriptClass.getName().startsWith(t.getTypeName()+"$Trait")) {
							continue;
						}
						builder.append(delim);
						TypeLabel.build(t, builder);
						delim = ", ";
					}
					builder.append(")");
					methods.put(builder.toString(), false);
				}
			}
		}
		
		public boolean isCoverable() {
			return runnable || !methods.isEmpty();
		}

		public List<String> getCoveredMethods() {
			ArrayList<String> covered = new ArrayList<String>();
			for (Entry<String, Boolean> entry : methods.entrySet()) {
				if (entry.getValue().booleanValue()) {
					covered.add(entry.getKey());
				}
			}
			return covered;
		}

		public List<String> getUncoveredMethods() {
			ArrayList<String> uncovered = new ArrayList<String>();
			for (Entry<String, Boolean> entry : methods.entrySet()) {
				if (!entry.getValue().booleanValue()) {
					uncovered.add(entry.getKey());
				}
			}
			return uncovered;
		}

		public double getCoverage() {
			double total = methods.size();
			if (runnable) {
				total++;
			}
			if (total == 0) {
				return 1.0;
			}
			double covered = 0;
			if (ran) {
				covered++;
			}
			covered += getCoveredMethods().size();
			return covered / total;
		}
	}
}

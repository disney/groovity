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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.disney.groovity.ArgsLookup;
import com.disney.groovity.Groovity;
import com.disney.groovity.servlet.container.GroovityServletContainer;
import com.disney.groovity.servlet.container.GroovityServletContainerBuilder;
import com.disney.groovity.stats.GroovityStatistics;

/**
 * The run mojo starts a local web server and interactive shell which will automatically pick up changes 
 * to groovity scripts in both the source and test directory.  Web and websocket endpoints can be accessed 
 * via the configured port, and any application or test scripts that don't depend on a servlet request can 
 * be executed via the command line prompt.
 * <ul>
 * <li><b>groovityTestDirectory</b> - defines the home folder for groovity test scripts, defaults to src/test/groovity</li>
 * <li><b>port</b> - defines the HTTP port for the embedded server, defaults to 9880</li>
 * <li><b>path</b> - Optionally specify a single or comma-separated list of test paths to execute instead of an interactive shell</li>
 * <li><b>sources</b> - Can be used to specify one or more additional source directories to be loaded, for example with local 
 * 	environment setup or utility scripts; these will be loaded in addition to source and test folders during the execution of groovity:run, 
 * 	but will not be referenced in the test or package plugin</li>
 * </ul>
 * <p>
 * <pre>
 * &lt;execution&gt;
 *	&lt;id&gt;default-cli&lt;/id&gt;
 *	&lt;goals&gt;
 *		&lt;goal&gt;run&lt;/goal&gt;
 *	&lt;/goals&gt;
 *	&lt;configuration&gt;
 *		&lt;sources&gt;
 * 			&lt;source&gt;src/env/local&lt;/source&gt;
 *  		&lt;source&gt;src/util/groovity&lt;/source&gt;
 * 		&lt;/sources&gt;
 *	&lt;/configuration&gt;
 * &lt;/execution&gt;
 * </pre>
 * 
 * @author Alex Vigdor
 */
@Mojo(defaultPhase=LifecyclePhase.NONE, name = "run",requiresDependencyResolution=ResolutionScope.TEST)
public class GroovityRunMojo extends AbstractGroovityMojo{
	@Parameter
	private File[] sources;
	@Parameter(defaultValue="src/test/groovity")
	private File groovityTestDirectory;
	@Parameter(defaultValue="-1")
	private String port;
	@Parameter(defaultValue="-1")
	private String securePort;
	@Parameter
	private String secureKeyStoreLocation;
	@Parameter
	private String secureKeyStorePassword;
	@Parameter(property="path")
	private String path;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			getLog().info("STARTING Groovity run ");
			populateSystemProperties();
			ClassLoader testLoader = createClassLoader(ClassLoaderScope.TEST);
			GroovityServletContainerBuilder builder = new GroovityServletContainerBuilder()
					.setPort(Integer.valueOf(port))
					.setSecurePort(Integer.valueOf(securePort))
					.setSecureKeyStoreLocation(secureKeyStoreLocation)
					.setSecureKeyStorePassword(secureKeyStorePassword)
					.setWebappDirectory(new File(project.getBasedir(),"src/main/webapp"))
					.setClassLoader(testLoader);
			if(groovitySourceDirectory!=null && groovitySourceDirectory.exists()){
				builder.addSourceLocation(groovitySourceDirectory.toURI(),true);
			}
			if(groovityTestDirectory!=null && groovityTestDirectory.exists()){
				builder.addSourceLocation(groovityTestDirectory.toURI(),true);
			}
			if(sources!=null){
				for(File source: sources){
					builder.addSourceLocation(source.toURI(),true);
				}
			}
			GroovityServletContainer container = builder.build();
			container.start();
			try{
				GroovityStatistics.reset();
				Groovity groovity = container.getGroovity();
				if(path!=null){
					groovity.getArgsLookup().chainLast(new ArgsLookup.ConsoleArgsLookup());
					String[] pathParts = path.split("\\s*,\\s*");
					for(String pathPart: pathParts){
						container.run(pathPart);
					}
				}
				else{
					container.enterConsole();
				}
			}
			finally{
				container.stop();
			}
			getLog().info("DONE with Groovity run ");
		} 
		catch (Throwable e) {
			getLog().error("ERROR in Groovity run ",e);
			throw new MojoFailureException(e.getMessage());
		}
	}

}

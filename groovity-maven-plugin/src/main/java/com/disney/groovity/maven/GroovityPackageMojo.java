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
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityBuilder;
import com.disney.groovity.GroovityPhase;

/**
 * The package mojo allows you to generate compiled jar files from your groovity scripts, 
 * e.g. to embed within a WAR file for a binary-only deployment. It supports one additional configuration option:
 * <ul>
 * <li><b>groovityJarDirectory</b> - defines the output folder where JAR files should be saved,
 * 	 	defaults to "${project.build.directory}/${project.build.finalName}/WEB-INF/groovity-classes"
 * </li>
 * </ul>
 * @author Alex Vigdor
 */
@Mojo(defaultPhase=LifecyclePhase.PREPARE_PACKAGE, name = "package",requiresDependencyResolution=ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GroovityPackageMojo extends AbstractGroovityMojo{
	private static final String DISABLE_AUTH = "groovity.cluster.disableAuth";
	@Parameter(defaultValue="${project.build.directory}/classes/groovity")
	private File groovityJarDirectory;

	public void execute() throws MojoExecutionException, MojoFailureException {
		String oldAuth = System.getProperty(DISABLE_AUTH);
		System.setProperty(DISABLE_AUTH, "true");
		try {
			getLog().info("STARTING Groovity package");
			populateSystemProperties();
			Groovity groovity = new GroovityBuilder()
					.setSourceLocations(Arrays.asList(groovitySourceDirectory.toURI()))
					.setDefaultBinding(defaultBinding)
					.setParentClassLoader(createClassLoader(ClassLoaderScope.COMPILE))
					.setJarDirectory(groovityJarDirectory)
					.setJarPhases(EnumSet.of(GroovityPhase.RUNTIME))
					.build(false);
			try{
				if(failOnError) {
					validateFactory(groovity);
				}
			}
			finally{
				groovity.destroy();
			}
			if(groovityJarDirectory.exists()){
				//now we will generate a manifest of the generated jar files
				File manifest = new File(groovityJarDirectory,"manifest");
				FileOutputStream stream = new FileOutputStream(manifest);
				PrintWriter writer = new PrintWriter(stream);
				try{
					walk(groovityJarDirectory.toPath(),groovityJarDirectory,writer);
				}
				finally{
					writer.close();
				}
			}
		} 
		catch(MojoFailureException e) {
			throw e;
		}
		catch (Throwable e) {
			getLog().error("ERROR in Groovity package",e);
			throw new MojoFailureException(e.getMessage());
		}
		finally{
			if(oldAuth!=null){
				System.setProperty(DISABLE_AUTH, oldAuth);
			}
			else{
				System.getProperties().remove(DISABLE_AUTH);
			}
		}
	}
	
	private void walk(Path root, File current, PrintWriter writer){
		if(current.isDirectory()){
			File[] kids = current.listFiles();
			if(kids!=null){
				for(int i=0;i<kids.length;i++){
					File kid = kids[i];
					if(kid.isDirectory() || kid.getName().endsWith(".grvt.jar")){
						walk(root,kids[i],writer);
					}
				}
			}
		}
		else{
			Path relative = root.relativize(current.toPath());
			for(int i=0;i<relative.getNameCount();i++){
				if(i>0){
					writer.write("/");
				}
				writer.write(relative.getName(i).toString());
			}
			writer.write('\n');
		}
	}

}

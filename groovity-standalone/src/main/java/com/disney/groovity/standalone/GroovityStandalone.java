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
package com.disney.groovity.standalone;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import com.disney.groovity.servlet.container.GroovityServletContainer;
import com.disney.groovity.servlet.container.GroovityServletContainerBuilder;


/**
 * Standalone executable for groovity scripts.  Accepts port assignment as only argument,
 * defaults to 9880.
 *
 * @author Alex Vigdor
 */
public class GroovityStandalone {
	static Logger logger = Logger.getLogger("GroovityStandalone");

	public static void main(String[] args) {
		try{
			int port = -1;
			int securePort = -1;
			String keyStoreLocation = null;
			String keyStorePassword = null;
			if(args.length>0){
				port = Integer.parseInt(args[0]);
				if(args.length>1) {
					securePort = Integer.parseInt(args[1]);
					if(args.length>2) {
						keyStoreLocation = args[2];
						if(args.length > 3) {
							keyStorePassword = args[3];
						}
						else {
							keyStorePassword = "";
						}
					}
				}
			}
			File workingDirectory = new File("").getAbsoluteFile();
			File groovityDirectory = new File(workingDirectory,"groovity");
			if(!groovityDirectory.exists() || !groovityDirectory.isDirectory()){
				throw new FileNotFoundException("No directory found at "+groovityDirectory.getAbsolutePath());
			}
			List<URL> projectClasspathList = new ArrayList<URL>();
			File libDirectory = new File(workingDirectory,"lib");
			if(libDirectory.exists() && libDirectory.isDirectory()){
				File[] jars = libDirectory.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.toLowerCase().endsWith(".jar");
					}
				});
				if(jars!=null && jars.length>0){
					for(int i=0;i<jars.length;i++){
						projectClasspathList.add(jars[i].toURI().toURL());
					}
				}
			}
			File resourceDirectory = new File(workingDirectory,"static");
			if(!resourceDirectory.exists() || !resourceDirectory.isDirectory()){
				resourceDirectory = null;
			}
			File jarDirectory = new File(workingDirectory,"target");
			if(!jarDirectory.exists() || !jarDirectory.isDirectory()){
				jarDirectory = null;
			}
			File confDirectory = new File(workingDirectory,"conf");
			if(!confDirectory.exists() || !confDirectory.isDirectory()){
				confDirectory = null;
			}
			URLClassLoader loader = new URLClassLoader(projectClasspathList.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
			
			GroovityServletContainerBuilder groovityServletContainerBuilder = new GroovityServletContainerBuilder()
					.addSourceLocation(groovityDirectory.toURI(),true)
					.setPort(port)
					.setSecurePort(Integer.valueOf(securePort))
					.setSecureKeyStoreLocation(keyStoreLocation)
					.setSecureKeyStorePassword(keyStorePassword)
					.setWebappDirectory(workingDirectory)
					.setClassLoader(loader);
			
			if(jarDirectory!=null){
				groovityServletContainerBuilder
					.setJarDirectory(jarDirectory);
			}
			if(confDirectory!=null){
				groovityServletContainerBuilder.setPropsFile(confDirectory.getAbsolutePath());
			}
			
			DefaultServlet resourceServlet = new DefaultServlet();
			ServletHolder resourceServletHolder = new ServletHolder(resourceServlet);
			GroovityServletContainer container = groovityServletContainerBuilder.build();
			WebAppContext context = container.getContext();
			context.addServlet(resourceServletHolder, "/static/*");
			container.start();
			try{
				container.enterConsole();
		    }
		    finally{
		    	container.stop();
		    }
		}
		catch(Throwable e){
			logger.log(Level.SEVERE, "Error in groovity standalone", e);
		}
		System.exit(0);
	}

}

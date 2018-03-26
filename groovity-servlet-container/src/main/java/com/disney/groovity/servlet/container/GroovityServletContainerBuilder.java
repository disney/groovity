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
package com.disney.groovity.servlet.container;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An embedded groovity servlet container builder
 * 
 * Allows configuration of the essential things needed; source locations, jar directory, classloader, webapp directory, port,
 * admin password and cluster secret
 * 
 * @author Alex Vigdor
 *
 */
public class GroovityServletContainerBuilder {
	private List<String> sourceLocations = new ArrayList<>();
	private List<String> consoleSourceLocations = new ArrayList<>();
	private File webapp = null;
	private ClassLoader classLoader = null;
	private int port = 9880;
	private File jarDirectory = null;
	private String adminPassword = null;
	private String clusterSecret = null;
	private String propsFile = null;
	
	public GroovityServletContainerBuilder addSourceLocation(URI sourceLocation, boolean showInConsole){
		sourceLocations.add(sourceLocation.toString());
		if(showInConsole){
			consoleSourceLocations.add(sourceLocation.toString());
		}
		return this;
	}
	public GroovityServletContainerBuilder setWebappDirectory(File webapp){
		this.webapp=webapp;
		return this;
	}
	public GroovityServletContainerBuilder setClassLoader(ClassLoader loader){
		this.classLoader=loader;
		return this;
	}
	public GroovityServletContainerBuilder setPort(int port){
		if(port > 0) {
			this.port=port;
		}
		return this;
	}
	public GroovityServletContainerBuilder setJarDirectory(File jarDirectory){
		this.jarDirectory=jarDirectory;
		return this;
	}
	public GroovityServletContainerBuilder setAdminPassword(String adminPassword){
		this.adminPassword=adminPassword;
		return this;
	}
	public GroovityServletContainerBuilder setClusterSecret(String clusterSecret){
		this.clusterSecret=clusterSecret;
		return this;
	}
	public GroovityServletContainerBuilder setPropsFile(String propsFile){
		this.propsFile=propsFile;
		return this;
	}
	
	private void revertableSet(String key, String value, Map<String,String> revert){
		revert.put(key,System.getProperty(key));
		System.setProperty(key, value);
	}
	
	public GroovityServletContainer build() throws IOException{
		Map<String,String> revertProperties = new HashMap<>();
		if(!sourceLocations.isEmpty()){
			revertableSet("groovity.sourceLocation",String.join("\n",sourceLocations), revertProperties);
			revertableSet("groovity.sourcePhases", "STARTUP,RUNTIME", revertProperties);
			revertableSet("groovity.sourcePollSeconds", "2", revertProperties);
		}
		if(jarDirectory!=null && jarDirectory.exists()){
			revertableSet("groovity.jarPhases", "STARTUP,RUNTIME",revertProperties);
			revertableSet("groovity.jarDirectory", jarDirectory.getAbsolutePath(),revertProperties);
		}
		boolean delete = false;
		if(webapp==null || !webapp.exists()){
			webapp = Files.createTempDirectory("groovity-jetty").toFile();
			delete=true;
		}
		if(classLoader==null){
			classLoader = Thread.currentThread().getContextClassLoader();
		}
		if(adminPassword==null){
			revertableSet("groovity.cluster.disableAuth", "true",revertProperties);
		}
		else{
			revertableSet("groovity.cluster.password", adminPassword, revertProperties);
		}
		if(clusterSecret!=null){
			revertableSet("groovity.cluster.secret", clusterSecret, revertProperties);
		}
		if(propsFile!=null){
			revertableSet("groovity.propsFile", propsFile, revertProperties);
		}
		if(System.getProperty("groovity.port")==null){
			revertableSet("groovity.port", String.valueOf(port), revertProperties);
		}
        GroovityServletContainer container = new GroovityServletContainer(port,webapp,classLoader,delete,consoleSourceLocations,revertProperties);
		return container;
	}
}

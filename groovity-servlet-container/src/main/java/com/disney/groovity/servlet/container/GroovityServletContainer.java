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

import java.io.Closeable;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;

import com.disney.groovity.ArgsLookup;
import com.disney.groovity.Groovity;
import com.disney.groovity.servlet.GroovityServlet;
import com.disney.groovity.source.FileGroovitySourceLocator;
import com.disney.groovity.source.GroovitySource;
import com.disney.groovity.source.GroovitySourceLocator;

import groovy.lang.Binding;

/**
 * An embedded groovity servlet container based on jetty with interactive console, used by groovity maven plugins and standalone
 * 
 * Relies on system properties to configure the underlying groovity which is loaded indirectly by the webapp; use
 * GroovityServletContainerBuilder to set the essential options and it will set the appropriate system properties as well as 
 * constructing the container.
 * 
 * Calling code has access to the WebAppContext and may perform further customization after the container is built and before it 
 * is started.
 * 
 * @author Alex Vigdor
 *
 */
public class GroovityServletContainer {
	final static Logger log = Logger.getLogger(GroovityServletContainer.class.getSimpleName());
	final private File webapp;
	final private ClassLoader classLoader;
	final private boolean delete;
	final private List<String> consoleSourceLocations;
	final private Map<String, String> revertProperties;
	boolean started = false;
	boolean stopped = false;
	Server server;
	WebAppContext context;
	Groovity groovity;
	
	protected GroovityServletContainer(int port, int securePort, SslContextFactory.Server sslContextFactory, File webapp, ClassLoader classLoader, boolean delete, List<String> consoleSourceLocations, Map<String,String> revertProperties) throws IOException{
		this.webapp=webapp;
		this.classLoader=classLoader;
		this.delete=delete;
		this.consoleSourceLocations=consoleSourceLocations;
		this.revertProperties=revertProperties;
		server=new Server();
		
		org.eclipse.jetty.webapp.Configuration.ClassList classlist = org.eclipse.jetty.webapp.Configuration.ClassList.setServerDefault(server);
		classlist.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration",
                "org.eclipse.jetty.plus.webapp.EnvConfiguration",
                "org.eclipse.jetty.plus.webapp.PlusConfiguration");
		classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
				"org.eclipse.jetty.annotations.AnnotationConfiguration");
		
		if (port > 0) {
			HttpConfiguration hc = new HttpConfiguration();
			hc.setSendServerVersion(false);
			HttpConnectionFactory factory = new HttpConnectionFactory(hc);
			ServerConnector connector = new ServerConnector(server, factory);
			connector.setPort(port);
			server.addConnector(connector);
		}
		if (securePort > 0) {
			HttpConfiguration hc = new HttpConfiguration();
			hc.setSendServerVersion(false);
			hc.addCustomizer(new SecureRequestCustomizer());
			hc.setSecurePort(securePort);
			hc.setSecureScheme("https");
			ServerConnector httpsConnector = new ServerConnector(server,
					new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(hc));
			httpsConnector.setPort(securePort);
			server.addConnector(httpsConnector);
		}
        context = new WebAppContext();
        context.setResourceBase(webapp.getAbsolutePath());
        context.setContextPath("/");
		context.setClassLoader(new WebAppClassLoader(classLoader, context));
		CodeSource servletSource = GroovityServlet.class.getProtectionDomain().getCodeSource();
		URL su = servletSource != null ? servletSource.getLocation() : null;
		URLClassLoader ucl = (URLClassLoader) classLoader;
		boolean hasServlet = false;
		for (URL url : ucl.getURLs()) {
			File file = new File(url.getFile());
			if (file.exists()) {
				context.getMetaData().addWebInfJar(Resource.newResource(file));
				if (url.equals(su)) {
					hasServlet = true;
				}
			}
		}
		if (!hasServlet && su != null) {
			context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", su.toString());
		}
		server.setHandler(context);
		// this is a patch to prevent harmless but annoying classloader issues at shutdown
		BlockingArrayQueue<String> baq = new BlockingArrayQueue<>();
		baq.add("test");
		baq.listIterator().hasNext();
  		//end patch
	}
	
	public void start() throws Exception{
		if (!started) {
			started = true;
			server.start();
			// context.getServletHandler().dumpStdErr();
			groovity = (Groovity) context.getServletContext().getAttribute(GroovityServlet.SERVLET_CONTEXT_GROOVITY_INSTANCE);
		}
	}
	public Groovity getGroovity(){
		return groovity;
	}
	public WebAppContext getContext(){
		return context;
	}
	public void stop() throws Exception{
		if(started && !stopped){
			stopped=true;
			server.stop();
			if(delete){
				webapp.delete();
			}
			if(groovity!=null) {
				groovity.destroy();
			}
			Properties sysProps = System.getProperties();
			for(Entry<String, String> entry: revertProperties.entrySet()){
				if(entry.getValue()==null){
					sysProps.remove(entry.getKey());
				}
				else{
					sysProps.put(entry.getKey(), entry.getValue());
				}
			}
		}
	}
	
	public void run(String path) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException{
		PrintWriter pw = new PrintWriter(System.out);
		try{
			pw.println("RUNNING "+path);
			Binding binding = new Binding();
			binding.setProperty("out", pw);
			Object o = groovity.run(path, binding);
			pw.println();
			if(o!=null && !(o instanceof Closeable)){
				pw.println("DONE "+path+", return value was "+o);
			}
			else{
				pw.println("DONE "+path);
			}
		}
		finally{
			pw.flush();
		}
	}
	
	public void enterConsole(){
		groovity.getArgsLookup().chainLast(new ArgsLookup.ConsoleArgsLookup());
		try{
			Console console = System.console();
			if(console==null) {
				log.warning("Unable to retrieve System.console(), GroovityServletContainer will not be interactive");
				try {
					Thread.currentThread().join();
				} catch (InterruptedException e) {
				}
				return;
			}
			String cmdFmt = "(l)ist, (a)ll, (q)uit, or script path [%1$s] : ";
			String scriptPath = "";
			String command = console.readLine(cmdFmt, scriptPath);
			while(!"q".equals(command)){
				if(!command.trim().isEmpty()){
					scriptPath = command.trim();
				}
				if(scriptPath.isEmpty()){
					console.printf("Error: enter a script path or q to quit\n");
				}
				else{
					//pick up any code changes
					groovity.compileAll(false, true);
					final AtomicBoolean error = new AtomicBoolean(false);
					groovity.getCompilerEvents().forEach((k,v)->{
						if(v.getError()!=null){
							error.set(true);
						}
					});
					if(!error.get()){
						String[] pathParts;
						if("a".equals(scriptPath) || "l".equals(scriptPath)){
							ArrayList<String> paths = new ArrayList<>();
							for(GroovitySourceLocator locator:groovity.getSourceLocators()){
								if(locator instanceof FileGroovitySourceLocator){
									String directory = ((FileGroovitySourceLocator)locator).getDirectory().toURI().toString();
									if(consoleSourceLocations.contains(directory)){
										for(GroovitySource source: locator){
											paths.add(source.getPath().substring(0,source.getPath().lastIndexOf(".")));
										}
									}
								}
							}
							if(paths.isEmpty()) {
								paths.addAll(groovity.getGroovityScriptNames());
							}
							pathParts = paths.toArray(new String[0]);
						}
						else{
							pathParts = scriptPath.split("\\s*,\\s*");
						}
						if("l".equals(scriptPath)){
							console.printf("All available paths:\n");
							for(String pathPart: pathParts){
								console.printf("\t%1$s\n",pathPart);
							}
						}
						else{
							for(String pathPart: pathParts){
								try{
									run(pathPart);
								}
								catch(Throwable e){
									console.printf("%1$s running script %2$s :\n", e.getClass().getName(), pathPart);
									e.printStackTrace(console.writer());
								}
							}
						}
					}
				}
				command = console.readLine(cmdFmt, scriptPath);
			}
		}
		finally{
			groovity.getArgsLookup().removeLast();
		}
	}
}

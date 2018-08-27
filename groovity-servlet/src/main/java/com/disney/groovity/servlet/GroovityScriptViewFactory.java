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
package com.disney.groovity.servlet;

import groovy.lang.Script;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.Session;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityObserver;
import com.disney.groovity.servlet.auth.VerifierFactory;
import com.disney.groovity.servlet.cors.CORSFactory;
import com.disney.groovity.servlet.error.GroovityErrorHandlerChain;
import com.disney.groovity.servlet.uri.PathTemplate;
import com.disney.groovity.websocket.WebSocket;

/**
 * GroovityScriptViewFactory is responsible for registering GroovityScriptViews in sync with an underlying Groovity used for compiling application sources.
 * GroovityServlet calls GroovityScriptViewFactory to create processors from registered views for every incoming request.
 * 
 * @author Alex Vigdor
 *
 */
public class GroovityScriptViewFactory{
	private static final Logger logger = Logger.getLogger(GroovityScriptViewFactory.class.getName());
	private final ConcurrentHashMap<String, GroovityScriptView> views = new ConcurrentHashMap<String, GroovityScriptView>();
	private final ConcurrentSkipListMap<PathTemplateMethods, GroovityScriptView> viewPaths = new ConcurrentSkipListMap<PathTemplateMethods, GroovityScriptView>();
	private final ConcurrentHashMap<String, GroovityScriptView> sockets = new ConcurrentHashMap<String, GroovityScriptView>();
	private PathView[] pathViews;
	private Groovity factory;
	
	private boolean caseSensitive = true;
	private GroovityErrorHandlerChain errorHandlers = GroovityErrorHandlerChain.createDefault();
	private ServletContext servletContext;
	private VerifierFactory verifierFactory;
	private CORSFactory corsFactory;
	private AtomicLong socketOpenCount = new AtomicLong();
	private AtomicLong socketCloseCount = new AtomicLong();
	private AtomicLong socketErrorCount = new AtomicLong();
	
	public GroovityScriptViewFactory(){
		verifierFactory = new VerifierFactory();
		verifierFactory.setViewResolver(this);
		corsFactory = new CORSFactory();
		corsFactory.setViewResolver(this);
	}
	
	public void init() throws IOException, URISyntaxException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		if(factory==null){
			throw new IllegalArgumentException("GroovityScriptViewFactory requires a non-null Groovity instance to provide scipts");
		}
		//observe script factory for changes to scripts
		factory.addObserver(new GroovityScriptViewObserver());
	}
	
	public void destroy(){
		factory.destroy();
		//adminFactory.destroy();
		//clusterClient.destroy();
	}
	
	public GroovityScriptView getViewByName(String viewName) throws Exception {     
		return views.get(viewName);
	}
	
	public GroovityScriptView getSocketByName(String socketName) throws Exception {     
		return sockets.get(socketName);
	}

	public WebSocket createSocket(String socketName, Session session) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException{
		GroovityScriptView gsv = sockets.get(socketName);
		if(gsv!=null){
			WebSocket ws = gsv.getSocket(session);
			ws.setName(socketName);
			return ws;
		}
		return null;
	}
	
	public GroovityScriptView.Processor createProcessor(HttpServletRequest request) throws Exception {
		String requestPath = request.getPathInfo();
		if(requestPath==null){
			//when running as default servlet fall back
			requestPath = request.getServletPath();
		}
		String requestMethod = request.getMethod();
		if(!requestPath.startsWith("/")){
			requestPath = "/".concat(requestPath);
		}
		if(!caseSensitive){
			requestPath = requestPath.toLowerCase();
		}			
		Set<String> possibleMethods = null;
		//evaluate path templates
		Map<String,String> resolved  = new HashMap<>();
		final PathView[] pvs = pathViews;
		for(int i=0;i<pvs.length;i++){
			final PathView pv = pvs[i];
			final PathTemplateMethods templateMethods = pv.pathTemplateMethods;
			final PathTemplate template = templateMethods.getPathTemplate();
			//System.out.println("Evaluating "+template.getTemplate()+" for "+viewName);
			if(template.match(requestPath, resolved)){
				final Set<String> supportedMethods = templateMethods.getMethods();
				//validate request method if required
				if(supportedMethods==null || supportedMethods.isEmpty() || supportedMethods.contains(requestMethod)){
					//System.out.println("Found match ");
					return pv.groovityScriptView.getProcessor(template, resolved);
				}
				else{
					if(possibleMethods==null){
						possibleMethods = new HashSet<String>();
					}
					possibleMethods.addAll(supportedMethods);
				}
			}
		}
		if(possibleMethods!=null){
			request.setAttribute(GroovityServlet.REQUEST_ATTRIBUTE_ALLOW_METHODS, possibleMethods);
		}
		return null;
	}
	
	public Groovity getGroovity(){
		return factory;
	}

	public void setGroovity(Groovity groovity){
		this.factory=groovity;
	}
	

	
	public GroovityErrorHandlerChain getErrorHandlers() {
		return errorHandlers;
	}

	public void setErrorHandlers(GroovityErrorHandlerChain errorHandlers) {
		this.errorHandlers = errorHandlers;
	}

	public void setServletContext(ServletContext context) {
		this.servletContext=context;
	}
	
	public ServletContext getServletContext(){
		return servletContext;
	}

	public long getSocketOpenCount() {
		return socketOpenCount.get();
	}

	public void socketOpen() {
		socketOpenCount.incrementAndGet();
	}

	public long getSocketCloseCount() {
		return socketCloseCount.get();
	}

	public void socketClose() {
		socketCloseCount.incrementAndGet();
	}

	public long getSocketErrorCount() {
		return socketErrorCount.get();
	}

	public void socketError() {
		socketErrorCount.incrementAndGet();
	}
	
	private void buildPathViews(){
		ArrayList<PathView> pv = new ArrayList<>();
		for(Entry<PathTemplateMethods, GroovityScriptView> entry: viewPaths.entrySet()){
			pv.add(new PathView(entry.getKey(),entry.getValue()));
		}
		pathViews = pv.toArray(new PathView[0]);
	}
	private static class PathView implements Comparable<PathView>{
		final PathTemplateMethods pathTemplateMethods;
		final GroovityScriptView groovityScriptView;
		
		private PathView(PathTemplateMethods pathTemplateMethods, GroovityScriptView groovityScriptView){
			this.pathTemplateMethods = pathTemplateMethods;
			this.groovityScriptView = groovityScriptView;
		}

		@Override
		public int compareTo(PathView o) {
			return pathTemplateMethods.compareTo(o.pathTemplateMethods);
		}
		
	}
	
	private class GroovityScriptViewObserver implements GroovityObserver.Field{
		@Override
		public String getField() {
			return "web";
		}

		@Override
		public void destroy(Groovity groovity) {
			
		}
		public void scriptStart(Groovity groovity, String scriptPath, Class<Script> scriptClass, Object web) {
			if(logger.isLoggable(Level.FINE)){
				logger.fine("Registering new script view "+scriptPath);
			}
			if(!(web instanceof Map)) {
				throw new IllegalArgumentException("static web field must contain a Map for configuration");
			}
			@SuppressWarnings("rawtypes")
			GroovityScriptView view = new GroovityScriptView(scriptPath, scriptClass, (Map) web, groovity, errorHandlers, verifierFactory, corsFactory);
			GroovityScriptView oldView = views.put(scriptPath, view);
			Collection<PathTemplateMethods> paths = view.getPathTemplateMethods();
			Collection<PathTemplateMethods> oldPaths = oldView!=null?oldView.getPathTemplateMethods():null;
			if(oldPaths!=null){
				Collection<PathTemplateMethods> toRemove = oldPaths;
				if(paths!=null){
					toRemove =  oldPaths.stream().filter(path -> !paths.contains(path)).collect(Collectors.toSet());
				}
				//System.out.println("Removing paths "+toRemove);
				for(PathTemplateMethods tmpl:toRemove){
					viewPaths.remove(tmpl);
				}
			}
			if(paths!=null){
				for(PathTemplateMethods tmpl:paths){
					//System.out.println("Registering path "+tmpl.getTemplate());
					viewPaths.put(tmpl, view);
				}
			}
			List<String> oldSockets = oldView!=null?oldView.getSocketNames():null;
			if(oldSockets!=null){
				for(String oldSock:oldSockets){
					if(logger.isLoggable(Level.FINE)){
						logger.fine("DeRegistering socket "+oldSock);
					}
					sockets.remove(oldSock);
				}
			}
			List<String> socketNames = view.getSocketNames();
			if(socketNames!=null){
				for(String sock: socketNames){
					if(logger.isLoggable(Level.FINE)){
						logger.info("Registering socket "+sock);
					}
					sockets.put(sock, view);
				}
			}
			buildPathViews();
		}

		public void scriptDestroy(Groovity groovity, String scriptPath, Class<Script> scriptClass, Object web) {
			GroovityScriptView oldView = views.remove(scriptPath);
			if(oldView!=null){
				Collection<PathTemplateMethods> oldTemplates = oldView.getPathTemplateMethods();
				if(oldTemplates!=null){
					for(PathTemplateMethods tmpl: oldTemplates){
						//System.out.println("Removing path "+tmpl);
						viewPaths.remove(tmpl,oldView);
					}
				}
				List<String> oldSockets = oldView!=null?oldView.getSocketNames():null;
				if(oldSockets!=null){
					for(String oldSock:oldSockets){
						sockets.remove(oldSock);
					}
				}
				buildPathViews();
			}
		}
	}

}

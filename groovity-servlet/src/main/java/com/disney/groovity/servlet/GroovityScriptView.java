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

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;

import org.apache.http.impl.EnglishReasonPhraseCatalog;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityObjectConverter;
import com.disney.groovity.compile.GroovityClassLoader;
import com.disney.groovity.servlet.auth.VerifierFactory;
import com.disney.groovity.servlet.cors.CORSFactory;
import com.disney.groovity.servlet.cors.CORSProcessor;
import com.disney.groovity.servlet.error.ForwardErrorHandlerImpl;
import com.disney.groovity.servlet.error.GroovityError;
import com.disney.groovity.servlet.error.GroovityErrorHandlerChain;
import com.disney.groovity.servlet.uri.PathTemplate;
import com.disney.groovity.util.ScriptHelper;
import com.disney.groovity.websocket.WebSocket;
import com.disney.http.auth.AuthConstants;
import com.disney.http.auth.server.AuthenticatedRequestWrapper;
import com.disney.http.auth.server.ServletAuthorizationRequest;
import com.disney.http.auth.server.Verifier;
import com.disney.http.auth.server.VerifierResult;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.Script;

/**
 * Turns a groovity Script into a web View; this involves parsing the declarative static "web" configuration of the script
 * and wiring up the JAX-RS paths, instantiating security verifier and cors processor and error handlers.  Individual
 * requests are handled by generating a single-use Processor via getProcessor that encapsulates the output of JAX-RS path matching
 * and path variable resolution for the given url path.
 * 
 * @author Alex Vigdor
 *
 */
public class GroovityScriptView implements AuthConstants{
	private static final Logger logger = Logger.getLogger(GroovityScriptView.class.getName());
	public static String GROOVITY_ERROR = "groovity.error";
	private static final Pattern pathPattern = Pattern.compile("(?<=^|})([^{]+)");
	private static final Pattern sizePattern = Pattern.compile("([\\d.]+)([GMK]B)?", Pattern.CASE_INSENSITIVE);
	private final String name;
	private final Class<Script> scriptClass;
	private final Groovity viewFactory;
	private final Set<PathTemplateMethods> pathTemplateMethods;
	private final List<String> outputs;
	private final List<Pattern> inputs;
	private final List<String> charsets;
	private final List<Locale> languages;
	private final List<String> varies;
	private final List<String> methods;
	protected GroovityErrorHandlerChain errorHandlers;
	private Verifier verifier;
	private CORSProcessor corsProcessor;
	@SuppressWarnings("rawtypes")
	private Map webMap;
	private List<String> socketNames;
	@SuppressWarnings("rawtypes")
	private Class messageSocketType;
	private Method openMethod;
	private Method closeMethod;
	private Method errorMethod;
	protected int bufferSize = -1;
	boolean omitXmlDeclaration = false;

	@SuppressWarnings("rawtypes")
	public GroovityScriptView(String name, Class<Script> scriptClass, Groovity viewFactory, GroovityErrorHandlerChain errorHandlers, VerifierFactory verifierFactory, CORSFactory corsFactory){
		this.name=name;
		this.scriptClass=scriptClass;
		this.viewFactory=viewFactory;
		this.errorHandlers = errorHandlers;
		this.webMap = getStaticMap("web");
		
		//record static output declarations
		this.outputs=getStaticValues("output");
		List<String> inputSpecs = getStaticValues("input");
		if(inputSpecs!=null) {
			this.inputs= new ArrayList<>();
			for(String is: inputSpecs) {
				this.inputs.add(Pattern.compile(is));
			}
		}
		else {
			this.inputs = null;
		}
		this.charsets=getStaticValues("charset");
		List<String> langs = getStaticValues("language");
		List<Locale> locs = new ArrayList<Locale>();
		if(langs!=null){
			for(String lang: langs){
				String[] tag = lang.split("[-_]");
				if(tag.length==1){
					locs.add(new Locale(tag[0]));
				}
				else if(tag.length==2){
					locs.add(new Locale(tag[0],tag[1]));
				}
				else if(tag.length>2){
					locs.add(new Locale(tag[0],tag[1],tag[2]));
				}
			}
		}
		this.languages = locs.size() > 0 ? locs : null;
		this.varies = getStaticValues("vary");
		List<String> errorPages = getStaticValues("errorPage");
		if(errorPages!=null && errorPages.size()>0){
			GroovityErrorHandlerChain newErrorHandlerChain = new GroovityErrorHandlerChain();
			if(errorHandlers!=null){
				errorHandlers.forEach(h->{
					if(!(h instanceof ForwardErrorHandlerImpl)) {
						newErrorHandlerChain.add(h);
					}
				});
			}
			for(String errorPage:errorPages){
				newErrorHandlerChain.add(new ForwardErrorHandlerImpl(errorPage));
			}
			this.errorHandlers=newErrorHandlerChain;
		}
		List auths = getStaticObjects("auth");
		if(auths!=null){
			//System.out.println("Got auths for view "+name+": "+auths);
			try {
				this.verifier = verifierFactory.createVerifier(auths, scriptClass);
			} catch (Exception e) {
				logger.log(Level.SEVERE,"Unable to create verifier for "+name,e);
			} 
		}
		this.methods = getStaticValues("method");
		Set<String> mtds = methods!=null ? new HashSet<String>(methods) : null;
		if(mtds!=null && mtds.contains("GET") && !mtds.contains("HEAD")){
			//implicitly support HEAD if GET is supported
			mtds.add("HEAD");
		}
		List cors = getStaticObjects("cors");
		if(cors!=null){
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> corsMap = new HashMap((Map)cors.get(0));
				if(mtds!=null){
					//force enable OPTIONS so preflight CORS requests can make it in
					mtds.add("OPTIONS");
					if(corsMap.get("methods")==null){
						//if no specific CORS methods are configured but the script has methods, let's sync them up
						corsMap.put("methods",new ArrayList<String>(mtds));
					}
				}
				this.corsProcessor = corsFactory.createProcessor(corsMap);
			} catch (Exception e) {
				logger.log(Level.SEVERE,"Unable to create CORS processor for "+name,e);
			}
		}
		//Resolve static path templates
		Set<PathTemplateMethods>  templates = new ConcurrentSkipListSet<PathTemplateMethods>();
		
		List<String> pathTemplates = getStaticValues("path");
		if(pathTemplates!=null){
			
			for(String pt:pathTemplates){
				templates.add(new PathTemplateMethods(new PathTemplate(fixPatternCase(pt)),mtds));
			}
		}

		this.pathTemplateMethods = templates.size()>0 ? templates :null;
		
		this.socketNames = getStaticValues("socket");
		List<Object> messages = getStaticObjects("message");
		if(messages!=null && !messages.isEmpty()){
			Object o = messages.get(0);
			if(o!=null){
				if(!(o instanceof Class)){
					try {
						o = Class.forName(o.toString());
					} catch (ClassNotFoundException e) {
						logger.severe("No class found for socket message type "+o.toString());
					}
				}
				this.messageSocketType = (Class) o;
			}
		}
		openMethod = getDeclaredMethod(scriptClass, "open", 1);
		closeMethod = getDeclaredMethod(scriptClass, "close", 1);
		errorMethod = getDeclaredMethod(scriptClass, "error", 1);
		configureBuffer();
		configureXmlDeclaration();
	}

	static Method getDeclaredMethod(@SuppressWarnings("rawtypes") Class clz, String method, int maxParams) {
		Method[] classMethods = clz.getDeclaredMethods();
		Method best = null;
		for(int i=0;i<classMethods.length;i++) {
			Method check = classMethods[i];
			if(method.equals(check.getName()) &&!Modifier.isStatic(check.getModifiers())) {
				if(best==null || check.getParameterCount() > best.getParameterCount()) {
					best = check;
					if(check.getParameterCount()==maxParams) {
						break;
					}
				}
			}
		}
		return best;
	}
	@SuppressWarnings("rawtypes")
	private Map getStaticMap(String fieldName){
		try {
			java.lang.reflect.Field outputField = scriptClass.getDeclaredField(fieldName);
			if(Modifier.isStatic(outputField.getModifiers())){
				outputField.setAccessible(true);
				Object outputValue = outputField.get(null);
				if(outputValue == null){
					return null;
				}
				if(!(outputValue instanceof Map)){
					throw new IllegalArgumentException("Expecting Map in field "+fieldName+", found "+outputValue.getClass().getName());
				}
				return (Map) outputValue;
			}
		}
		catch(Exception e){
		}
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	private void configureXmlDeclaration() {
		if(webMap!=null) {
			Object omitXmlConfig = webMap.get("omitXmlDeclaration");
			if(omitXmlConfig instanceof Closure) {
				omitXmlConfig = ((Closure)omitXmlConfig).call();
			}
			if(omitXmlConfig == null) {
				return;
			}
			omitXmlDeclaration = (Boolean) GroovityObjectConverter.convert(omitXmlConfig, Boolean.class);
		}
	}
	
	private void configureBuffer(){
		if(webMap!=null){
			Object bufferConfig = webMap.get("buffer");
			if(bufferConfig instanceof Closure){
				bufferConfig = ((Closure<?>)bufferConfig).call();
			}
			if(bufferConfig == null){
				return;
			}
			if(bufferConfig instanceof Boolean){
				if(Boolean.TRUE.equals(bufferConfig)){
					//true indicates unlimited buffer
					bufferSize = Integer.MAX_VALUE;
				}
			}
			else if(bufferConfig instanceof Number){
				bufferSize  = ((Number)bufferConfig).intValue();
			}
			else{
				String bufStr = bufferConfig.toString();
				Matcher sizeMatcher = sizePattern.matcher(bufStr);
				if(sizeMatcher.matches()){
					int size = Integer.parseInt(sizeMatcher.group(1));
					String unit = sizeMatcher.group(2);
					if(unit!=null){
						size *= 1024;
						if(!unit.equalsIgnoreCase("KB")){
							size *= 1024;
							if(!unit.equalsIgnoreCase("MB")){
								size*=1024;
							}
						}
					}
					bufferSize = size;
				}
				else{
					logger.warning("Unable to parse buffer size "+bufStr+" for "+name);
				}
			}
			logger.fine("After configuration, bufferSize is "+bufferSize+" for "+name);
		}
	}

	private List<String> getStaticValues(String fieldName){
		List<String> outputs = new ArrayList<String>();
		if(webMap!=null){
			Object val = webMap.get(fieldName);
			if(val!=null){
				outputs.add(val.toString());
			}
			String plural = fieldName;
			if(plural.endsWith("y")){
				plural = plural.substring(0, plural.length()-1).concat("ies");
			}
			else{
				plural = plural.concat("s");
			}
			Object outputValue = webMap.get(plural);
			if(outputValue instanceof Collection){
				@SuppressWarnings("rawtypes")
				Collection col = ((Collection)outputValue);
				for(Object output:col){
					outputs.add(output.toString());
				}
			}
		}
		return outputs.size()>0?outputs:null;
	}
	
	private List<Object> getStaticObjects(String fieldName){
		List<Object> outputs = new ArrayList<Object>();
		if(webMap!=null){
			Object val = webMap.get(fieldName);
			if(val!=null){
				outputs.add(val);
			}
			String plural = fieldName;
			if(plural.endsWith("y")){
				plural = plural.substring(0, plural.length()-1).concat("ies");
			}
			else{
				plural = plural.concat("s");
			}
			Object outputValue = webMap.get(plural);
			if(outputValue instanceof Collection){
				@SuppressWarnings("rawtypes")
				Collection col = ((Collection)outputValue);
				for(Object output:col){
					outputs.add(output);
				}
			}
		}
		return outputs.size()>0?outputs:null;
	}
	
	private String fixPatternCase(String pattern){
		if(!viewFactory.isCaseSensitive()){
			//we need to convert static elements in the path pattern to lowercase for case insensitive matching
			Matcher matcher = pathPattern.matcher(pattern);
			StringBuffer buf = new StringBuffer();
			while(matcher.find()){
				matcher.appendReplacement(buf, matcher.group(1).toLowerCase());
			}
			matcher.appendTail(buf);
			//System.out.println("Changed pattern "+pattern+" to "+buf.toString());
			pattern = buf.toString();
		}
		return pattern;
	}
	
	public Processor getProcessor(final PathTemplate path, final Map<String, String> resolved){
		return new Processor(path,resolved);
	}
	
	public WebSocket getSocket(Session session) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException{
		WebSocket ws = new WebSocket(session);
		Binding binding = new Binding();
		binding.setVariable("socket", ws);
		final Script script = viewFactory.load(name, binding);
		//look for "open" method on socket
		if(openMethod!=null){
			Binding oldBinding = ScriptHelper.THREAD_BINDING.get();
			try{
				ScriptHelper.THREAD_BINDING.set(binding);
				if(openMethod.getParameterCount()==0) {
					openMethod.invoke(script);
				}
				else {
					openMethod.invoke(script, session);
				}
			}
			finally{
				if(oldBinding==null) {
					ScriptHelper.THREAD_BINDING.remove();
				}
				else {
					ScriptHelper.THREAD_BINDING.set(oldBinding);
				}
			}
		}
		ws.setMessageHandler(arg -> {
			synchronized (script) {
				script.getBinding().setVariable("message", arg);
				script.run();
			}
		}, messageSocketType);
		if(closeMethod!=null){
			ws.setCloseHandler(reason -> {
				Binding oldBinding = ScriptHelper.THREAD_BINDING.get();
				try{
					ScriptHelper.THREAD_BINDING.set(binding);
					if(closeMethod.getParameterCount()==0) {
						closeMethod.invoke(script);
					}
					else {
						closeMethod.invoke(script, reason);
					}
				}
				catch (Exception e) {
					logger.log(Level.SEVERE, "Error closing web socket", e);
				}
				finally{
					if(oldBinding==null) {
						ScriptHelper.THREAD_BINDING.remove();
					}
					else {
						ScriptHelper.THREAD_BINDING.set(oldBinding);
					}
				}
			});
		}
		if(errorMethod!=null) {
			ws.setErrorHandler(thrown -> {
				Binding oldBinding = ScriptHelper.THREAD_BINDING.get();
				try{
					ScriptHelper.THREAD_BINDING.set(binding);
					if(errorMethod.getParameterCount()==0) {
						errorMethod.invoke(script);
					}
					else {
						errorMethod.invoke(script, thrown);
					}
				}
				catch (Exception e) {
					logger.log(Level.SEVERE, "Error handling error for web socket", e);
					logger.log(Level.SEVERE, "Original error was ", thrown);
				}
				finally{
					if(oldBinding==null) {
						ScriptHelper.THREAD_BINDING.remove();
					}
					else {
						ScriptHelper.THREAD_BINDING.set(oldBinding);
					}
				}
			});
		}
		return ws;
	}
	
	public class Processor{
		private final PathTemplate path;
		private final Map<String,String> resolvedVariables;
		
		private Processor(PathTemplate path, Map<String,String> resolvedVariables){
			this.path=path;
			this.resolvedVariables=resolvedVariables;
		}

		/**
		 * Apply request parameters to binding and run!
		 */
		public void process(HttpServletRequest request, final HttpServletResponse response) throws Exception {
			StringBuilder varyHeader = new StringBuilder();
			if(corsProcessor!=null){
				corsProcessor.process(request, response);
				if(varyHeader.length()>0){
					varyHeader.append(", ");
				}
				varyHeader.append("Origin");	
			}
			if(request.getMethod().equals("OPTIONS")){
				//bypass all further processing for OPTIONS requests
				if(varyHeader.length()>0){
					response.setHeader("Vary", varyHeader.toString());
				}
				return;
			}
			GroovityServletResponseWrapper responseWrapper = new GroovityServletResponseWrapper(request, response, GroovityScriptView.this);
			if(verifier!=null){
				if(varyHeader.length()>0){
					varyHeader.append(", ");
				}
				varyHeader.append("Authorization, Signature");
				VerifierResult vf = verifier.verify(new ServletAuthorizationRequest(request));
				if(vf.getAuthenticationInfo()!=null){
					response.setHeader(AUTHENTICATION_INFO,	vf.getAuthenticationInfo());
				}
				if(vf.isAuthenticated()){
					if(!vf.isAuthorized()){
						response.setHeader("Vary", varyHeader.toString());
						responseWrapper.sendError(403, vf.getMessage());
						return;
					}
					if(vf.getPrincipal()!=null) {
						request = new AuthenticatedRequestWrapper(request,vf.getPrincipal());
					}
				}
				else{
					response.setHeader("Vary", varyHeader.toString());
					if(vf.getChallenge()!=null){
						response.setHeader(WWW_AUTHENTICATE_HEADER, vf.getChallenge());
					}
					responseWrapper.sendError(401, vf.getMessage());
					return;
				}
			}
			
			if(!viewFactory.isCaseSensitive()){
				ArrayList<String> values = new ArrayList<String>();
				String tpath = request.getPathInfo();//.toLowerCase();
				String ipath = tpath;
				ipath = tpath.toLowerCase();
				//lookup value positions and retrieve from original path
				//so we can preserve variable case, otherwise everything is converted to lowercase
				int startpos = 0;
				int pos = 0;
				for(int i=0;i<values.size();i++){
					String value = values.get(i);
					pos = ipath.indexOf(value, startpos);
					if(pos > 0){
						startpos = pos+value.length();
						String cValue = tpath.substring(pos,startpos);
						if(!cValue.equals(value) && cValue.equalsIgnoreCase(value)){
							values.set(i, cValue);
						}
					}
				}
				List<String> names = path.getTemplateVariables();
				for(int i=0;i<names.size();i++){
					resolvedVariables.put(names.get(i), values.get(i));
				}
			}
				
			if(request.getAttribute("com.newrelic.agent.TRANSACTION_NAME")==null){
				request.setAttribute("com.newrelic.agent.TRANSACTION_NAME", name);
			}
			request.setAttribute("viewName",name);
			Binding binding = new Binding(resolvedVariables);
			binding.setVariable("request", request);
			GroovityError gError = (GroovityError) request.getAttribute(GROOVITY_ERROR);
			if(gError!=null){
				binding.setVariable("error", gError);
			}
			else if(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) != null){
				gError = new GroovityError();
				gError.setCause((Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION));
				gError.setMessage((String)request.getAttribute(RequestDispatcher.ERROR_MESSAGE));
				gError.setStatus((Integer)request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
				if(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE)!=null){
					gError.setReason(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE).toString());
				}
				gError.setUri((String)request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
				binding.setVariable("error", gError);
			}
			if(inputs!=null) {
				if(request.getMethod().equals("PUT") || request.getMethod().equals("POST")) {
					//validate input requirements
					String ct = request.getContentType();
					int semi = ct.indexOf(";");
					if(semi>0) {
						ct = ct.substring(0,semi);
					}
					boolean match = false;
					for(int i=0; i< inputs.size(); i++) {
						if(inputs.get(i).matcher(ct).matches()) {
							match = true;
							break;
						}
					}
					if(!match) {
						responseWrapper.sendError(406, "This endpoint cannot process Content-Type="+ct+"; supported input content types are "+inputs);
						return;
					}
				}
			}
			//Note: we used to load the response writer into the binding, 
			//but that prevents the script from changing the encoding, so 
			//the script should call response.getWriter() or response.getOutputStream();
			Map<String,String> variant = new HashMap<>();
			
			StringBuilder contentTypeHeader = new StringBuilder();
			if(outputs!=null){
				String output = null;
				if(outputs.size()>1){
					if(varyHeader.length()>0){
						varyHeader.append(", ");
					}
					varyHeader.append("Accept");
					//determine the right output to use
					String accepts = request.getHeader("Accept");
					if(accepts!=null){
						output = TypeNegotiator.bestMatch(outputs, accepts);
						if(output.equals("")){
							if(gError!=null){
								//for error page just produce default
								output = outputs.get(0);
							}
							else{
								responseWrapper.sendError(406, "Could not provide representation to satisfy Accept="+accepts+"; supported output content types are "+outputs);
								return;
							}
						}
					}
					else{
						//use the first available
						output = outputs.get(0);
					}
					variant.put("output", output);
				}
				else{
					//no variations
					output = outputs.get(0);
				}
				contentTypeHeader.append(output);
			}
			if(charsets!=null){
				String enc = null;
				if(charsets.size()>1){
					if(varyHeader.length()>0){
						varyHeader.append(", ");
					}
					varyHeader.append("Accept-Charset");
					//determine the right output to use
					String accepts = request.getHeader("Accept-Charset");
					if(accepts!=null){
						String[] accSets = accepts.toLowerCase().split(",");
						for(String accSet: accSets){
							for(String mycset: charsets){
								if(accSet.equals("*") || accSet.startsWith(mycset.toLowerCase())){
									enc = mycset;
									break;
								}
							}
						}
					}
					if(enc==null){
						//use the first available
						enc = charsets.get(0);
					}
					variant.put("charset", enc);
				}
				else{
					enc = charsets.get(0);
				}
				contentTypeHeader.append("; charset=").append(enc);
			}
			if(languages!=null){
				Locale lang = null;
				if(languages.size()>1){
					if(varyHeader.length()>0){
						varyHeader.append(", ");
					}
					varyHeader.append("Accept-Language");
					List<Locale> requestLocales = Collections.list(request.getLocales());
					for(Locale locale: requestLocales){
						//System.out.println("Evaluating locale "+locale);
						for(Locale myLang: languages){
							//System.out.println("Comparing "+locale+" to "+myLang);
							if(locale.equals(myLang)){
								lang = myLang;
								break;
							}
							if(locale.getCountry().equals("") && locale.getLanguage().equals(myLang.getLanguage())){
								lang=myLang;
								break;
							}
						}
						if(lang!=null){
							break;
						}
					}
					if(lang==null){
						for(Locale locale: requestLocales){
							//System.out.println("Evaluating locale 2 "+locale);
							for(Locale myLang: languages){
								//System.out.println("Comparing 2 "+locale+" to "+myLang);
								if(locale.getLanguage().equals(myLang.getLanguage())){
									lang=myLang;
									break;
								}
							}
						}
					}
				}
				if(lang==null){
					lang = languages.get(0);
				}
				response.setLocale(lang);
				//response.setHeader("Content-Language",lang);
				variant.put("language", lang.toString());
			}
			
			if(varies!=null){
				for(String vary: varies){
					if(varyHeader.length()>0){
						varyHeader.append(", ");
					}
					varyHeader.append(vary);
					variant.put(vary, request.getHeader(vary));
				}
			}
			
			if(varyHeader.length()!=0){
				response.setHeader("Vary", varyHeader.toString());
			}
			String theContentType = null;
			if(contentTypeHeader.length() != 0){
				theContentType = contentTypeHeader.toString();
				response.setHeader("Content-Type", theContentType);
				
			}
			if(!variant.isEmpty()){
				binding.setVariable("variant", variant);
			}
			try{
				Script script = viewFactory.load(name, binding);
				//wait to set the response until after the load phase is complete
				//this will help enforce proper use of the load pattern
				binding.setVariable("response", responseWrapper);
				if(theContentType!=null){ 
					//we will preset out to be a writer or output stream if we can determine based on content type
					if(theContentType.contains("charset=") || theContentType.startsWith("text/") || theContentType.contains("xml") || theContentType.contains("json")){
						PrintWriter writer = responseWrapper.getWriter();
						binding.setVariable("out", writer);
						if(!omitXmlDeclaration && theContentType.contains("xml")) {
							writer.write("<?xml version=\"1.0\" encoding=\"");
							writer.write(response.getCharacterEncoding().toUpperCase());
							writer.println("\" standalone=\"yes\"?>");
						}
					}
					else if(theContentType.startsWith("image/") || theContentType.startsWith("video/") || theContentType.startsWith("audio/") || theContentType.contains("octet") || theContentType.contains("binary")){
						binding.setVariable("out", responseWrapper.getOutputStream());
					}
				}
				Object rval = script.run();
				((GroovityClassLoader)script.getClass().getClassLoader()).getScriptHelper().processReturn(script, rval);
				responseWrapper.commit();
			}
			catch(Throwable e){
				GroovityError groovyError = getGroovyError(request);
				groovyError.setCause(e);
				groovyError.setMessage(e.getMessage());
				groovyError.setStatus(500);
				groovyError.setReason(EnglishReasonPhraseCatalog.INSTANCE.getReason(500, response.getLocale()));
				boolean handled = false;
				if(errorHandlers!=null){
					//System.out.println("Handling errors with "+errorHandlers.size());
					handled = errorHandlers.handleError(request, responseWrapper, groovyError);
				}
				if(!handled){
					if(e instanceof Exception){
						throw (Exception)e;
					}
					else if(e instanceof Error){
						throw (Error) e;
					}
				}
			}
		}
	}
	
	protected GroovityError getGroovyError(HttpServletRequest request){
		GroovityError groovyError = (GroovityError) request.getAttribute(GROOVITY_ERROR);
		if(groovyError==null){
			groovyError = new GroovityError();
			groovyError.setScriptClass(this.scriptClass);
			groovyError.setScriptPath(this.name);
			String uri = request.getRequestURI();
			if(request.getQueryString()!=null){
				uri = uri.concat("?").concat(request.getQueryString());
			}
			groovyError.setUri(uri);
			request.setAttribute(GROOVITY_ERROR, groovyError);
			groovyError.setLogger(((GroovityClassLoader)scriptClass.getClassLoader()).getLogger());
		}
		return groovyError;
	}
	
	public Collection<PathTemplateMethods> getPathTemplateMethods(){
		return pathTemplateMethods;
	}
	
	public List<String> getSocketNames(){
		return socketNames;
	}

	public Verifier getVerifier() {
		return verifier;
	}

	public CORSProcessor getCORSProcessor(){
		return corsProcessor;
	}
}

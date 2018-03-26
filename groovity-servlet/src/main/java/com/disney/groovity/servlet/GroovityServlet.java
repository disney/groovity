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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.HostnameVerifier;
import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

import com.disney.groovity.ArgsLookup;
import com.disney.groovity.BindingDecorator;
import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityBuilder;
import com.disney.groovity.conf.Configurator;
import com.disney.groovity.servlet.GroovityScriptView.Processor;
import com.disney.groovity.servlet.error.GroovityError;
import com.disney.groovity.servlet.error.GroovityErrorHandler;
import com.disney.groovity.servlet.error.GroovityErrorHandlerChain;
import com.disney.groovity.servlet.error.GroovityErrorHandlerChainDecorator;
import com.disney.groovity.source.GroovitySourceLocator;

import groovy.json.JsonSlurper;
import java.util.logging.Logger;

/**
 * Provides core servlet functionality for Groovity applications; can be
 * configured using a number of options via web.xml init-params or via system
 * properties prefixed with "groovity.".
 * 
 * Options:
 * <ul>
 * <li><b>asyncThreads</b> - max number of async HTTP workers</li>
 * <li><b>caseSensitive</b> - whether to force case-sensitive template loading</li>
 * <li><b>maxConnPerRoute</b> - maximum number of HTTP client connections per route</li>
 * <li><b>maxConnTotal</b> - maximum number of total HTTP client connections</li>
 * <li><b>jarDirectory</b> - folder for reading and/or writing jar files of compiled scripts</li>
 * <li><b>jarPhases</b> - lifecycle phases for jar files, STARTUP to read, RUNTIME to write</li>
 * <li><b>scriptBaseClass</b> - class name of base class for groovity scripts (must descend from groovy.lang.Script)</li>
 * <li><b>sourcePhases</b> - phases for automatic source compilation, STARTUP and/or RUNTIME</li>
 * <li><b>sourceLocation</b> - specify a URL for loading script sources, file or http</li>
 * <li><b>sourceLocator</b> - specify a custom source loader className, e.g. for s3 support</li>
 * <li><b>sourcePollSeconds</b> - frequency with which the runtime compiler polls for source changes</li>
 * <li><b>defaultBinding</b> - simple JSON blob with some values to add to the default binding for all scripts</li>
 * <li><b>bindingDecorator</b> - java className of custom bindingDecorator to add arbitrary java objects to default binding for all scripts</li>
 * <li><b>errorHandlerChainDecorator</b> - java className of a GroovityErrorHandlerChainDecorator implementation to customize error handling</li>
 * <li><b>shutdownHandler</b> - java className of a runnable to execute when the servlet is destroyed</li>
 * <li><b>configurator</b> - java className of a custom Configurator implementation, e.g. to load configuration from a database or service</li>
 * <li><b>propsFile</b> - resource or file path to a properties file to load to configure the servlet and/or application
 * </ul>
 * 
 * @author Alex Vigdor
 */
@WebServlet(name = "groovity", urlPatterns = { "/" }, loadOnStartup = 0)
@MultipartConfig
public class GroovityServlet extends HttpServlet implements Servlet {
	private static final long serialVersionUID = -385204301640186889L;
	public static final String ASYNC_THREADS_PARAM = "asyncThreads";
	public static final String CASE_SENSITIVE_PARAM = "caseSensitive";
	public static final String MAX_CONN_PER_ROUTE_PARAM = "maxConnPerRoute";
	public static final String MAX_CONN_TOTAL_PARAM = "maxConnTotal";
	public static final String JAR_DIRECTORY_PARAM = "jarDirectory";
	public static final String JAR_DIRECTORY_PARAM_DEFAULT_VALUE = "WEB-INF/groovity-classes";
	public static final String JAR_PHASES_PARAM = "jarPhases";
	public static final String SCRIPT_BASE_CLASS_PARAM = "scriptBaseClass";
	public static final String SOURCE_PHASES_PARAM = "sourcePhases";
	public static final String SOURCE_LOCATION_PARAM = "sourceLocation";
	public static final String SOURCE_LOCATOR_PARAM = "sourceLocator";
	public static final String SOURCE_POLL_SECONDS = "sourcePollSeconds";
	public static final String DEFAULT_BINDING = "defaultBinding";
	public static final String BINDING_DECORATOR = "bindingDecorator";
	public static final String ERROR_CHAIN_DECORATOR = "errorHandlerChainDecorator";
	public static final String SHUTDOWN_HANDLER = "shutdownHandler";
	public static final String HOSTNAME_VERIFIER = "hostnameVerifier";
	public static final String TRUST_STRATEGY = "trustStrategy";
	public static final String CONFIGURATOR = "configurator";
	public static final String PROPS_FILE = "propsFile";
	public static final String SERVLET_CONTEXT_GROOVITY_INSTANCE = "com.disney.groovity.servlet.GroovityServlet$GroovityInstance";
	public static final String SERVLET_CONTEXT_GROOVITY_VIEW_FACTORY = "com.disney.groovity.servlet.GroovityServlet$GroovityViewFactory";
	public static final String REQUEST_ATTRIBUTE_ALLOW_METHODS = "com.disney.groovity.servlet.GroovityServlet$AllowMethods";
	public static final String IGNORE_STATUS_CODES = "groovity.ignoreStatusCodes";
	public static final String ERROR_PAGE = "groovity.errorPage";

	private static final String GROOVITY_SYSTEM_PROPERTY_PREFIX = "groovity.";
	private static final String SOURCE_LOCATOR_SPLIT_REGEX = "(\\s*[\\r\\n]\\s*)";

	private static final Logger LOG = Logger.getLogger(GroovityServlet.class.getName());

	// TODO how would we configure parent classloader here? Factory implementation classname?

	GroovityScriptViewFactory groovityScriptViewFactory;
	private Runnable shutdownHandler;
	private Properties configProperties;

	/**
	 * see {@link Servlet#destroy}
	 */
	@Override
	public void destroy() {
		groovityScriptViewFactory.destroy();
		if (shutdownHandler != null) {
			shutdownHandler.run();
		}
	}

	/**
	 * see {@link GenericServlet#getServletInfo}
	 */
	@Override
	public String getServletInfo() {
		return "GroovityServlet";
	}

	/**
	 * Get an input parameter by name; order of precedence will check System
	 * Properties first, followed by an entry in the groovity.propsFile (if
	 * present), finally looking at the servletConfig
	 */
	public String getParam(String name) {
		String propertyName = GROOVITY_SYSTEM_PROPERTY_PREFIX.concat(name);
		String param = System.getProperty(propertyName);
		if (isBlank(param)) {
			param = configProperties.getProperty(propertyName);
		}
		if (isBlank(param)) {
			param = getServletConfig().getInitParameter(name);
		}
		return param;
	}

	private Object loadInstance(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return Class.forName(className, true, getServletContext().getClassLoader()).newInstance();
	}

	private static boolean isNotBlank(String str) {
		return str != null && str.trim().length() > 0;
	}

	private static boolean isBlank(String str) {
		return str == null || str.trim().length() == 0;
	}

	/**
	 * see {@link GenericServlet#init}
	 */
	@Override
	public void init() throws ServletException {
		try {
			LOG.info("Initializing GroovityServlet");
			ServletConfig config = getServletConfig();
			if (groovityScriptViewFactory == null) {
				GroovityBuilder builder = new GroovityBuilder();
				configProperties = new Properties();
				String propsFile = getParam(PROPS_FILE);
				if (isNotBlank(propsFile)) {
					URL url = config.getServletContext().getClassLoader().getResource(propsFile);
					if (url == null && propsFile.startsWith("/")) {
						url = config.getServletContext().getResource(propsFile);
					}
					if (url != null) {
						LOG.info("Found groovity properties resource " + url);
						builder.setPropsUrl(url);
						try (InputStream configStream = url.openStream()) {
							configProperties.load(configStream);
						}
					} else {
						File file = new File(propsFile);
						if (file.exists()) {
							LOG.info("Found groovity properties file " + file.getAbsolutePath());
							builder.setPropsFile(file);
							if (!file.isDirectory()) {
								try (InputStream configStream = new FileInputStream(file)) {
									configProperties.load(configStream);
								}
							}
						} else {
							LOG.warning("Groovity properties file " + propsFile + " not found");
						}
					}
				} else {
					URL url = config.getServletContext().getClassLoader().getResource("groovity.properties");
					if (url != null) {
						LOG.info("Found groovity.properties on classpath");
						builder.setPropsUrl(url);
						try (InputStream configStream = url.openStream()) {
							configProperties.load(configStream);
						}
					}
				}
				if (configProperties.containsKey(IGNORE_STATUS_CODES) && !System.getProperties().containsKey(IGNORE_STATUS_CODES)) {
					System.setProperty(IGNORE_STATUS_CODES, configProperties.getProperty(IGNORE_STATUS_CODES));
				}
				if(configProperties.containsKey(ERROR_PAGE) && !System.getProperties().containsKey(ERROR_PAGE)) {
					System.setProperty(ERROR_PAGE, configProperties.getProperty(ERROR_PAGE));
				}
				String async = getParam(ASYNC_THREADS_PARAM);
				if (isNotBlank(async)) {
					builder.setAsyncThreads(Integer.parseInt(async));
				}
				String caseSens = getParam(CASE_SENSITIVE_PARAM);
				if (isNotBlank(caseSens)) {
					builder.setCaseSensitive(Boolean.parseBoolean(caseSens));
				}
				String maxPerRoute = getParam(MAX_CONN_PER_ROUTE_PARAM);
				if (isNotBlank(maxPerRoute)) {
					builder.setMaxHttpConnPerRoute(Integer.parseInt(maxPerRoute));
				}
				String maxTotal = getParam(MAX_CONN_TOTAL_PARAM);
				if (isNotBlank(maxTotal)) {
					builder.setMaxHttpConnTotal(Integer.parseInt(maxTotal));
				}
				File jarDirectory;
				String jarDir = getParam(JAR_DIRECTORY_PARAM);
				if (isNotBlank(jarDir)) {
					jarDirectory = new File(jarDir);
				} else {
					// default jar directory;
					jarDirectory = new File(getServletContext().getRealPath("/"), JAR_DIRECTORY_PARAM_DEFAULT_VALUE);
				}
				builder.setJarDirectory(jarDirectory);
				String jarPhase = getParam(JAR_PHASES_PARAM);
				if (isNotBlank(jarPhase)) {
					builder.setJarPhase(jarPhase);
				}
				String scriptBase = getParam(SCRIPT_BASE_CLASS_PARAM);
				if (isNotBlank(scriptBase)) {
					builder.setScriptBaseClass(scriptBase);
				}
				String defaultBinding = getParam(DEFAULT_BINDING);
				if (isNotBlank(defaultBinding)) {
					@SuppressWarnings("unchecked")
					Map<String, Object> db = (Map<String, Object>) new JsonSlurper().parse(new StringReader(defaultBinding));
					builder.setDefaultBinding(db);
				}
				String sourcePhase = getParam(SOURCE_PHASES_PARAM);
				if (isNotBlank(sourcePhase)) {
					builder.setSourcePhase(sourcePhase);
				}
				String sourcePoll = getParam(SOURCE_POLL_SECONDS);
				if (isNotBlank(sourcePoll)) {
					builder.setSourcePollSeconds(Integer.parseInt(sourcePoll));
				}
				String configurator = getParam(CONFIGURATOR);
				if (isNotBlank(configurator)) {
					builder.setConfigurator((Configurator) loadInstance(configurator));
				}
				String shutdown = getParam(SHUTDOWN_HANDLER);
				if (isNotBlank(shutdown)) {
					shutdownHandler = (Runnable) loadInstance(shutdown);
				}
				String hostnameVerifier = getParam(HOSTNAME_VERIFIER);
				if (isNotBlank(hostnameVerifier)) {
					builder.getHttpClientBuilder().setSSLHostnameVerifier((HostnameVerifier) loadInstance(hostnameVerifier));
				}
				String trustStrategy = getParam(TRUST_STRATEGY);
				if (isNotBlank(trustStrategy)) {
					SSLContextBuilder sslb = new SSLContextBuilder();
					sslb.loadTrustMaterial((TrustStrategy) loadInstance(trustStrategy));
					builder.getHttpClientBuilder().setSSLContext(sslb.build());
				}
				String sourceLocation = getParam(SOURCE_LOCATION_PARAM);
				String sourceLocator = getParam(SOURCE_LOCATOR_PARAM);
				if (isNotBlank(sourceLocation)) {
					// newlines separate multiple
					String[] sources = sourceLocation.split(SOURCE_LOCATOR_SPLIT_REGEX);
					ArrayList<URI> sourceURIs = new ArrayList<URI>(sources.length);
					for (String source : sources) {
						if (isNotBlank(source)) {
							sourceURIs.add(new URI(source));
						}
					}
					builder.setSourceLocations(sourceURIs);
				} else if (isNotBlank(sourceLocator)) {
					String[] sources = sourceLocator.split(SOURCE_LOCATOR_SPLIT_REGEX);
					ArrayList<GroovitySourceLocator> sourceLocators = new ArrayList<GroovitySourceLocator>(sources.length);
					for (String source : sources) {
						if (isNotBlank(source)) {
							sourceLocators.add((GroovitySourceLocator) loadInstance(source));
						}
					}
					builder.setSourceLocators(sourceLocators);
				}
				// we want to allow unconfigured groovities to run, presuming there are embedded
				// groovity classes
				BindingDecorator userDecorator = null;
				String userDecoratorClass = getParam(BINDING_DECORATOR);
				if (isNotBlank(userDecoratorClass)) {
					userDecorator = (BindingDecorator) loadInstance(userDecoratorClass);
				}
				builder.setBindingDecorator(new BindingDecorator(userDecorator) {
					@Override
					public void decorate(Map<String, Object> binding) {
						binding.put("servletContext", GroovityServlet.this.getServletContext());
					}
				});
				builder.setArgsLookup(new ArgsLookup(new RequestArgsLookup()));
				GroovityErrorHandlerChain errorHandlers = GroovityErrorHandlerChain.createDefault();
				String chainDecorator = getParam(ERROR_CHAIN_DECORATOR);
				if (isNotBlank(chainDecorator)) {
					((GroovityErrorHandlerChainDecorator) loadInstance(chainDecorator)).decorate(errorHandlers);
				}
				ServiceLoader.load(GroovityErrorHandlerChainDecorator.class).forEach(decorator ->{
					decorator.decorate(errorHandlers);
				});
				Groovity groovity = builder.build();
				groovityScriptViewFactory = new GroovityScriptViewFactory();
				groovityScriptViewFactory.setGroovity(groovity);
				groovityScriptViewFactory.setServletContext(getServletContext());
				groovityScriptViewFactory.setErrorHandlers(errorHandlers);
				groovityScriptViewFactory.init();
				config.getServletContext().setAttribute(SERVLET_CONTEXT_GROOVITY_VIEW_FACTORY, groovityScriptViewFactory);
				config.getServletContext().setAttribute(SERVLET_CONTEXT_GROOVITY_INSTANCE, groovity);
			}
			javax.websocket.server.ServerContainer webSocketServer = (javax.websocket.server.ServerContainer) config
					.getServletContext().getAttribute("javax.websocket.server.ServerContainer");
			if (webSocketServer != null) {
				// register websocket endpoint
				webSocketServer.addEndpoint(ServerEndpointConfig.Builder
						.create(GroovityServerEndpoint.class, "/ws/{socketName}")
						.configurator(new GroovityServerEndpoint.Configurator(groovityScriptViewFactory)).build());
				LOG.info("Created groovity web socket endpoint");
			}
			LOG.info("Completed initialization of GroovityServlet");
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	/**
	 * Primary request entry method for this servlet
	 *
	 * see {@link HttpServlet#service(HttpServletRequest, HttpServletResponse)}
	 */
	@Override
	public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		GroovityError error = null;
		try {
			Processor processor = groovityScriptViewFactory.createProcessor(req);
			if (processor != null) {
				if (req.getMethod().equals("HEAD")) {
					// prevent any body from being written, but we'll capture
					// the length
					final AtomicLong length = new AtomicLong(0);
					final AtomicBoolean scriptSetLength = new AtomicBoolean(false);
					final AtomicReference<PrintWriter> respWriter = new AtomicReference<PrintWriter>(null);
					final ServletOutputStream nullStream = new ServletOutputStream() {
						@Override
						public void write(int b) throws IOException {
							length.incrementAndGet();
						}

						@Override
						public void write(byte buf[], int offset, int len) throws IOException {
							length.addAndGet(len);
						}

						public void setWriteListener(WriteListener arg0) {
						}

						public boolean isReady() {
							return true;
						}
					};
					processor.process(req, new HttpServletResponseWrapper(res) {
						PrintWriter writer;

						@Override
						public ServletOutputStream getOutputStream() throws IOException {
							return nullStream;
						}

						@Override
						public PrintWriter getWriter() throws UnsupportedEncodingException {
							if (writer == null) {
								writer = new PrintWriter(new OutputStreamWriter(nullStream, getCharacterEncoding()));
								respWriter.set(writer);
							}
							return writer;
						}

						@Override
						public void setContentLength(int len) {
							res.setContentLength(len);
							scriptSetLength.set(true);
						}
					});
					if (!scriptSetLength.get()) {
						if (respWriter.get() != null) {
							respWriter.get().flush();
						}
						res.setContentLength((int) length.get());
					}
				} else {
					processor.process(req, res);
				}
			} else {
				error = new GroovityError();
				Object acceptMethods = req.getAttribute(REQUEST_ATTRIBUTE_ALLOW_METHODS);
				if (acceptMethods != null && acceptMethods instanceof Collection) {
					@SuppressWarnings("unchecked")
					Collection<String> am = (Collection<String>) acceptMethods;
					String sep = "";
					StringBuilder sb = new StringBuilder();
					for (Object m : am) {
						String method = m.toString();
						sb.append(sep).append(method);
						sep = ", ";
					}
					res.setHeader("Allow", sb.toString());
					error.setStatus(405);
					error.setMessage("Allowed methods for "+req.getRequestURI()+" are "+sb.toString());
				} else {
					error.setStatus(404);
				}
				
			}
		} catch (Throwable e) {
			error = new GroovityError();
			error.setCause(e);
			error.setStatus(500);
		}
		if(error!=null){
			error.setReason(EnglishReasonPhraseCatalog.INSTANCE.getReason(error.getStatus(), res.getLocale()));
			String uri = req.getRequestURI();
			if(req.getQueryString()!=null){
				uri = uri.concat("?").concat(req.getQueryString());
			}
			error.setUri(uri);
			req.setAttribute(GroovityScriptView.GROOVITY_ERROR, error);
			boolean handled = false;
			GroovityErrorHandlerChain handlers = groovityScriptViewFactory.getErrorHandlers();
			if(handlers!=null){
				handled = handlers.handleError(req, res, error);
			}
			if(!handled){
				Throwable cause = error.getCause();
				if(cause!=null){
					if(cause instanceof RuntimeException){
						throw (RuntimeException)cause;
					}
					throw new ServletException(cause);
				}
				else{
					res.sendError(error.getStatus(), error.getMessage());
				}
			}
		}
	}

	public GroovityScriptViewFactory getGroovityScriptViewFactory() {
		return groovityScriptViewFactory;
	}

	public void setGroovityScriptViewFactory(GroovityScriptViewFactory groovityScriptViewFactory) {
		this.groovityScriptViewFactory = groovityScriptViewFactory;
	}

}

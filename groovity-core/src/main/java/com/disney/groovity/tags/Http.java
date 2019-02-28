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
package com.disney.groovity.tags;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.Writable;
import groovy.util.slurpersupport.GPathResult;
import groovy.xml.XmlUtil;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.model.ModelJsonWriter;
import com.disney.groovity.model.ModelXmlWriter;
import com.disney.groovity.stats.GroovityStatistics;
import com.disney.groovity.tags.Credentials.UserPass;
import com.disney.groovity.util.ScriptHelper;
import com.disney.http.auth.client.signer.HttpSignatureSigner;

/**
 * Tag to perform an HTTP request
 * <p>
 * http( <ul>	
 *	<li><b>url</b>: 
 *	The URL on which to perform the operation,</li>	
 *	<li><i>var</i>: 
 *	A variable name to store the response data (XML Document, JSON Object, text or byte[] for sync requests, or Future for async requests, or arbitrary for custom handler),</li>	
 *	<li><i>async</i>: 
 *	if true, request will run in the background and a Future is set for var, defaults to false or synchronous execution,</li>	
 *	<li><i>method</i>: 
 *	The HTTP method to use, defaults to GET,</li>	
 *	<li><i>data</i>: 
 *	Optional text, xml or json object to use in POST or PUT body; shorthand for scripting common body data,</li>	
 *	<li><i>to</i>: 
 *	java class name or concrete object to parse data into, defaults to Map or List,</li>	
 *	<li><i>timeout</i>: 
 *	Number of seconds after which to abort a synchronous HTTP import, defaults to no timeout,</li>	
 *	<li><i>redirects</i>: 
 *	Boolean controls whether redirects are automatically followed, defaults to true,</li>	
 *	<li><i>cookies</i>: 
 *	Set cookie handling policy, options are (off|default|netscape|standard|strict),</li>	
 *	<li><i>context</i>: 
 *	Provide a custom HttpClient context, e.g. to share cookies across multiple requests,</li>
 *	</ul>{
 *	<blockquote>// param and header tags to add to the request, optional handler tag to control response processing, and arbitrary scripting to generate POST or PUT body.  Default response handling parses JSON, XML or text based on Content-Type for synchronous requests, and disposes async responses (only logging errors)</blockquote>
 * 	});
 *	
 *	<p><b>returns</b> response data (see var attribute)
 *	
 *	<p>Sample
 *	<pre>
 *	httpFuture = http(async:true,url:&quot;http://jsonip.com&quot;,timeout:5,{
 *		header(name:&quot;User-Agent&quot;,value:&quot;Lynx/2.8.8dev.3 libwww-FM/2.14 SSL-MM/1.4.1&quot;)
 *		param(name:&quot;example&quot;,value:&quot;sample&quot;)
 *	})
 *	&lt;~${httpFuture.get().ip}~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */ 
@Tag(
	info="Tag to perform an HTTP request",
	body="<g:param> and <g:header> tags to add to the request, optional <g:handler> tag to control response processing, and arbitrary scripting to generate POST or PUT body.  Default response handling parses JSON, XML or text based on Content-Type for synchronous requests, and disposes async responses (only logging errors)",
	sample="httpFuture = http(async:true,url:\"http://jsonip.com\",timeout:5,{\n" + 
			"	header(name:\"User-Agent\",value:\"Lynx/2.8.8dev.3 libwww-FM/2.14 SSL-MM/1.4.1\")\n" + 
			"	param(name:\"example\",value:\"sample\")\n" + 
			"})\n" + 
			"<~${httpFuture.get().ip}~>",
	returns="response data (see var attribute)",
	attrs={
	      @Attr(name=GroovityConstants.URL,info="The URL on which to perform the operation",required=true),
	      @Attr(name=GroovityConstants.VAR,info="A variable name to store the response data (XML Document, JSON Object, text or byte[] for sync requests, or Future for async requests, or arbitrary for custom handler)", required=false),
	      @Attr(name="async",info="if true, request will run in the background and a Future is set for var, defaults to false or synchronous execution",required=false),
	      @Attr(name="method",info="The HTTP method to use, defaults to GET", required=false),
	      @Attr(name="data",info="Optional text, xml or json object to use in POST or PUT body; shorthand for scripting common body data", required=false),
	      @Attr(name="to", info="java class name or concrete object to parse data into, defaults to Map or List", required = false),
	      @Attr(name=GroovityConstants.TIMEOUT, info="Number of seconds after which to abort a synchronous HTTP import, defaults to no timeout", required=false),
	      @Attr(name="redirects", info="Boolean controls whether redirects are automatically followed, defaults to true", required=false),
	      @Attr(name="cookies", info="Set cookie handling policy, options are (default|netscape|standard|strict|off)", required=false),
	      @Attr(name="context", info="Provide a custom HttpClient context, e.g. to share cookies across multiple requests", required=false)
	}
)
public class Http implements Taggable {
	private static final String INTERCEPTOR_BINDING = INTERNAL_BINDING_PREFIX+"HttpInterceptor";
	private static final Pattern charsetPattern = Pattern.compile("(?i)(?<=charset=)([^;,\\r\\n]+)");
	private static final Log log = LogFactory.getLog(Http.class);
	private enum CookieOption{
		DEFAULT {
			@Override
			public String getCookieSpec() {
				return CookieSpecs.DEFAULT;
			}
		},
		OFF{
			@Override
			public String getCookieSpec() {
				return CookieSpecs.IGNORE_COOKIES;
			}
		},
		STRICT{
			@Override
			public String getCookieSpec() {
				return CookieSpecs.STANDARD_STRICT;
			}
		},
		STANDARD{
			@Override
			public String getCookieSpec() {
				return CookieSpecs.STANDARD;
			}
		},
		NETSCAPE{
			@Override
			public String getCookieSpec() {
				return CookieSpecs.NETSCAPE;
			}
		};
		public abstract String getCookieSpec();
	}
	
	private DocumentBuilderFactory docBuilderFactory;
	private ExecutorService asyncExecutor;
	private HttpClient httpClient;
	private Timer timeoutTimer;

	public void init(){
		 timeoutTimer = new Timer();
	}
	public void destroy(){
		timeoutTimer.cancel();
	}
	public void setGroovity(Groovity groovity){
		this.httpClient=groovity.getHttpClient();
		this.docBuilderFactory = DocumentBuilderFactory.newInstance();
		docBuilderFactory.setNamespaceAware(true);
		docBuilderFactory.setIgnoringElementContentWhitespace(true);
		docBuilderFactory.setCoalescing(true);
		docBuilderFactory.setIgnoringComments(true);
		docBuilderFactory.setValidating(false);
		this.asyncExecutor = groovity.getAsyncExecutor();
	}
	@SuppressWarnings({"rawtypes","unchecked"})
	public Object tag(Map attributes, Closure body) throws Exception {
		Object url = resolve(attributes,"url");
		if(url==null){
			throw new RuntimeException("<g:http> requires 'url' attribute");
		}
		Object var = resolve(attributes,VAR);
		String method = "GET";
		Object methodAtt = resolve(attributes,"method");
		if(methodAtt!=null){
			method=methodAtt.toString();
		}
		boolean followRedirects = true;
		Object redirectsAtt = resolve(attributes,"redirects");
		if(redirectsAtt!=null){
			followRedirects = Boolean.parseBoolean(redirectsAtt.toString());
		}
		CookieOption cookieOption = CookieOption.DEFAULT;
		Object cookiesAtt = resolve(attributes,"cookies");
		if(cookiesAtt!=null){
			cookieOption = CookieOption.valueOf(cookiesAtt.toString().toUpperCase());
		}
		Object timeout = resolve(attributes,TIMEOUT);
		final int timeoutSeconds = 
				timeout == null ? -1 :
				timeout instanceof Number ?  ((Number)timeout).intValue() :
				Integer.parseInt(timeout.toString());
		Object target = resolve(attributes,"to");
		if(target instanceof Class) {
			if(!Object.class.equals(target)) {
				target = ((Class)target).newInstance();
			}
		}
		if(target == null) {
			target = Object.class;
		}
		Object async = resolve(attributes,"async");
		if(async!=null && !(async instanceof Boolean)){
			async = Boolean.valueOf(async.toString());
		}
		HttpEntity dataEntity = null;
		Object data = resolve(attributes,"data");
		HttpClientContext clientContext = resolve(attributes, "context", HttpClientContext.class);
		if(clientContext==null){
			clientContext = HttpClientContext.create();
		}
		if(clientContext.getCookieStore()==null){
			//we don't want to let cookies be shared across contexts
			clientContext.setCookieStore(new BasicCookieStore());
		}
		if(clientContext.getAuthCache()==null){
			//we also don't want to share credentials across contexts
			clientContext.setAuthCache(new BasicAuthCache());
		}
		final HttpClientContext fContext = clientContext;
		ScriptHelper context = getScriptHelper(body);
		Object oldOut = get(context,OUT);
			//execute body to assemble URL params, headers, post body
			Map variables = context.getBinding().getVariables();
			URI uri;
			URIBuilder builder;
			ArrayList<Header> headers;
			Optional<UserPass> userPass;
			Optional<HttpSignatureSigner> signer;
			Optional<HttpRequestInterceptor> interceptor;
			ContentType targetType= null;
			try {
				builder = new URIBuilder(url.toString());
				bind(context,Uri.CURRENT_URI_BUILDER, builder);
				headers = new ArrayList<Header>();
				bind(context,com.disney.groovity.tags.Header.CURRENT_LIST_FOR_HEADERS, headers);
				Credentials.acceptCredentials(variables);
				Signature.acceptSigner(variables);
				acceptInterceptor(variables);
				StringWriter sw = new StringWriter();
				bind(context,OUT, sw);
				try{
					Object rval = body.call();
					if(rval instanceof Writable){
						((Writable)rval).writeTo(sw);
					}
				}
				finally{
					bind(context,OUT, oldOut);
					userPass = Credentials.resolveCredentials(variables);
					signer = Signature.resolveSigner(variables);
					interceptor = resolveInterceptor(variables);
				}
				for(Header header: headers) {
					if(header.getName().equalsIgnoreCase("Content-Type")) {
						targetType = ContentType.parse(header.getValue());
						String m = targetType.getMimeType();
						if(targetType.getCharset()==null && (
								m.startsWith("text") ||
								m.contains("xml") ||
								m.contains("json")
							)) {
							targetType = targetType.withCharset("UTF-8");
						}
						break;
					}
				}
				String val = sw.toString();
				if(val.trim().length()>0){
					dataEntity = new StringEntity(val, targetType);
				}
				if(builder.getScheme().equals("http") && builder.getPort()==80) {
					builder.setPort(-1);
				}
				else if(builder.getScheme().equals("https") && builder.getPort()==443) {
					builder.setPort(-1);
				}
				uri = builder.build();
				if(userPass.isPresent()){
					CredentialsProvider credsProvider = new BasicCredentialsProvider();
					credsProvider.setCredentials(new AuthScope(uri.getHost(), uri.getPort()),
							new UsernamePasswordCredentials(userPass.get().getUser(), new String(userPass.get().getPass())));
					clientContext.setCredentialsProvider(credsProvider);
				}
			}
			catch (URISyntaxException e1) {
				throw new RuntimeException("Invalid URI "+url,e1);
			}
			finally{
				unbind(context,Uri.CURRENT_URI_BUILDER);
				unbind(context,com.disney.groovity.tags.Header.CURRENT_LIST_FOR_HEADERS);
			}
			final HttpRequestBase request = 
				"POST".equalsIgnoreCase(method)? new HttpPost(uri) :
				"PUT".equalsIgnoreCase(method)? new HttpPut(uri) :
				"HEAD".equalsIgnoreCase(method)? new HttpHead(uri) :
				"DELETE".equalsIgnoreCase(method)? ( data!=null ? new HttpBodyDelete(uri) : new HttpDelete(uri)) :
				"OPTIONS".equalsIgnoreCase(method)? new HttpOptions(uri) :
				new HttpGet(uri);
			if(headers.size()>0){
				request.setHeaders(headers.toArray(new Header[0]));
			}
			if(request instanceof HttpEntityEnclosingRequest){
				if(data!=null){
					//decide on strategy to convert data to entity
					if(data instanceof HttpEntity){
						dataEntity = (HttpEntity) data;
					}
					else if(data instanceof CharSequence) {
						if(targetType!=null) {
							dataEntity = new StringEntity(data.toString(), targetType);
						}
						else {
							dataEntity = new StringEntity(data.toString(), "UTF-8");
						}
					}
					else if(data instanceof byte[]) {
						dataEntity = new ByteArrayEntity((byte[])data, targetType);
					}
					else if(data instanceof File) {
						dataEntity = new FileEntity((File)data, targetType);
					}
					else if (data instanceof InputStream) {
						dataEntity = new InputStreamEntity((InputStream)data, targetType);
					}
					else{
						//look at content type for a hint
						if(targetType!=null && targetType.getMimeType().contains("json")){
							CharArrayWriter caw = new CharArrayWriter();
							new ModelJsonWriter(caw).visit(data);
							dataEntity = new StringEntity(caw.toString(), targetType);
						}
						else if(targetType!=null && targetType.getMimeType().contains("xml")){
							if(data instanceof groovy.util.Node){
								dataEntity = new StringEntity(XmlUtil.serialize((groovy.util.Node)data), targetType);
							}
							else if (data instanceof GPathResult){
								dataEntity = new StringEntity(XmlUtil.serialize((GPathResult)data), targetType);
							}
							else if(data instanceof Element){
								dataEntity = new StringEntity(XmlUtil.serialize((Element)data), targetType);
							}
							else if(data instanceof Document){
								dataEntity = new StringEntity(XmlUtil.serialize(((Document)data).getDocumentElement()), targetType);
							}
							else {
								CharArrayWriter caw = new CharArrayWriter();
								new ModelXmlWriter(caw).visit(data);
								dataEntity = new StringEntity(caw.toString(), targetType);
							}
						}
						else if((targetType!=null && targetType.getMimeType().contains("x-www-form-urlencoded"))
								|| (targetType==null && (data instanceof Map || data instanceof List))){
							//key/value pairs, accept a map, a list of maps, or a list of NameValuePairs
							Iterator source = data instanceof Map? ((Map)data).entrySet().iterator() : ((List)data).iterator();
							ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();
							while(source.hasNext()){
								Object next = source.next();
								if(next instanceof Map.Entry){
									Map.Entry entry = (Entry) next;
									pairs.add(new BasicNameValuePair(entry.getKey().toString(), entry.getValue()!=null ? entry.getValue().toString() : ""));
								}
								else if (next instanceof NameValuePair){
									pairs.add((NameValuePair) next);
								}
								else if(next instanceof Map){
									Iterator<Map.Entry> sub = ((Map)next).entrySet().iterator();
									while(sub.hasNext()){
										Map.Entry se = sub.next();
										pairs.add(new BasicNameValuePair(se.getKey().toString(), se.getValue()!=null ? se.getValue().toString() : ""));
									}
								}
							}
							dataEntity = new UrlEncodedFormEntity(pairs, "UTF-8");
						}
						else if(targetType!=null && targetType.getMimeType().contains("multipart/form-data")){
							//list of maps, each map must contain "name" and "body", plus optional "type" and "filename"
							Iterator<Map> parts = ((List<Map>)data).iterator();
							MultipartEntityBuilder meBuilder = MultipartEntityBuilder.create();
							while(parts.hasNext()){
								Map part = parts.next();
								Object pbody = part.get("body");
								String name = (String) part.get("name");
								String type = (String) part.get("type");
								String filename = (String) part.get("filename");
								ContentType ct = type!=null ? ContentType.parse(type) : null;
								if(pbody instanceof File){
									if(ct==null){
										ct = ContentType.DEFAULT_BINARY;
									}
									meBuilder.addBinaryBody(name, (File) pbody, ct, filename);
								}
								else if(pbody instanceof byte[]){
									if(ct==null){
										ct = ContentType.DEFAULT_BINARY;
									}
									meBuilder.addBinaryBody(name, (byte[]) pbody, ct, filename);
								}
								else if(pbody instanceof ContentBody){
									meBuilder.addPart(name, (ContentBody) pbody);
								}
								else if(pbody instanceof InputStream){
									if(ct==null){
										ct = ContentType.DEFAULT_BINARY;
									}
									meBuilder.addBinaryBody(name, (InputStream) pbody, ct, filename);
								}
								else{
									if(ct==null){
										ct = ContentType.DEFAULT_TEXT;
									}
									meBuilder.addTextBody(name, pbody.toString(),ct);
								}
							}
							dataEntity = meBuilder.build();
						}
						else{
							//no help from content type header, check for modeled XML
							if(data instanceof groovy.util.Node){
								dataEntity = new StringEntity(XmlUtil.serialize((groovy.util.Node)data), ContentType.APPLICATION_XML.withCharset("UTF-8"));
							}
							else if (data instanceof GPathResult){
								dataEntity = new StringEntity(XmlUtil.serialize((GPathResult)data), ContentType.APPLICATION_XML.withCharset("UTF-8"));
							}
							else if(data instanceof Element){
								dataEntity = new StringEntity(XmlUtil.serialize((Element)data), ContentType.APPLICATION_XML.withCharset("UTF-8"));
							}
							else if(data instanceof Document){
								dataEntity = new StringEntity(XmlUtil.serialize(((Document)data).getDocumentElement()), ContentType.APPLICATION_XML.withCharset("UTF-8"));
							}
							else if(data instanceof byte[]){
								dataEntity = new ByteArrayEntity((byte[]) data); 
							}
							else if(data instanceof InputStream){
								dataEntity = new InputStreamEntity((InputStream)data);
							}
							else if(data instanceof File){
								dataEntity = new FileEntity((File)data);
							}
							else{
								//best option left is to post the toString value of the data
								dataEntity = new StringEntity(data.toString(),"UTF-8");
							}
						}
					}
				}
				if(dataEntity!=null){
					((HttpEntityEnclosingRequest)request).setEntity(dataEntity);
					if(dataEntity.getContentType() != null){
						request.setHeader("Content-Type", dataEntity.getContentType().getValue());
					}
				}
			}
			RequestConfig.Builder configBuilder = request.getConfig() ==null? RequestConfig.custom() : RequestConfig.copy(request.getConfig());
			if(!followRedirects){
				configBuilder.setRedirectsEnabled(followRedirects);
			}
			configBuilder.setCookieSpec(cookieOption.getCookieSpec());
			request.setConfig(configBuilder.build());
			final String varName = var!=null?var.toString():null;
			ResponseHandler handler = null;
			try{
				Function handlerFunction = (Function) get(body,Handler.HANDLER_BINDING);
				if(handlerFunction!=null){
					handler = new ResponseHandler<Object>() {
						@Override
						public Object handleResponse(HttpResponse response)
								throws ClientProtocolException, IOException {
							return handlerFunction.apply(response);
						}
					};
				}
				unbind(body,Handler.HANDLER_BINDING);
			}
			catch(Exception e){	
			}
			if(handler==null){
				handler=new AutoParsingResponseHandler( target);
			}
			final List<HttpRequestInterceptor> interceptors = new ArrayList<>();
			if(signer.isPresent()){
				interceptors.add(signer.get());
			}
			if(interceptor.isPresent()){
				interceptors.add(interceptor.get());
			}
			final ResponseHandler rHandler = handler;
			final boolean isAsync = (async!=null && Boolean.TRUE.equals(async));
			Callable<Object> requester = new Callable(){
				public Object call() throws Exception {
					TimeoutTask timeoutTask = null;
					if(timeoutSeconds > 0){
						timeoutTask = new TimeoutTask(request);
						timeoutTimer.schedule(timeoutTask, timeoutSeconds*1000);
					}
					try{
						Binding oldThreadBinding = null;
						if(isAsync){
							oldThreadBinding = ScriptHelper.THREAD_BINDING.get();
							Binding asyncBinding = new Binding();
							asyncBinding.setVariable("request", request);
							ScriptHelper.THREAD_BINDING.set(asyncBinding);
						}
						try{
							for(HttpRequestInterceptor interceptor: interceptors){
								interceptor.process(request, null);
							}
							return httpClient.execute(request,rHandler,fContext);
						}
						finally{
							if(isAsync){
								if(oldThreadBinding==null){
									ScriptHelper.THREAD_BINDING.remove();
								}
								else{
									ScriptHelper.THREAD_BINDING.set(oldThreadBinding);
								}
							}
						}
						
					}catch (HttpResponseException e) {
						if(isAsync){
							log.error("Async HTTP response error for "+request.getURI()+": "+e.getMessage());
						}
						throw e;
					} 
					catch (Exception e) {
						if(request.isAborted()){
							if(isAsync){
								log.error("Async <g:http> request timed out for "+request.getURI());
							}
							throw new TimeoutException("Timed out executing <g:http> for "+request.getURI());
						}
						else{
							if(isAsync){
								log.error("Async <g:http> request error for "+request.getURI(),e);
							}
							throw new RuntimeException("Error executing <g:http> for "+request.getURI(),e);
						}
					}
					finally{
						if(timeoutTask!=null){
							timeoutTask.cancel();
						}
					}
				}
				
			};
			Object responseVar = null;
			if(isAsync){
				//return the Future to the calling code
				Future<Object> f = asyncExecutor.submit(requester);
				responseVar = new Future<Object>() {

					@Override
					public boolean cancel(boolean mayInterruptIfRunning) {
						return f.cancel(mayInterruptIfRunning);
					}

					@Override
					public boolean isCancelled() {
						return f.isCancelled();
					}

					@Override
					public boolean isDone() {
						return f.isDone();
					}

					@Override
					public Object get() throws InterruptedException, ExecutionException {
						GroovityStatistics.startExecution("http(async)");
						try {
							return f.get();
						}finally {
							GroovityStatistics.endExecution();
						}
					}

					@Override
					public Object get(long timeout, TimeUnit unit)
							throws InterruptedException, ExecutionException, TimeoutException {
						GroovityStatistics.startExecution("http(async)");
						try {
							return f.get(timeout, unit);
						}finally {
							GroovityStatistics.endExecution();
						}
					}
					
				};
			}
			else{
				//return the parsed/handled response object
				GroovityStatistics.startExecution("http(sync)");
				try {
					responseVar = requester.call();
				}finally {
					GroovityStatistics.endExecution();
				}
			}
			if(varName!=null){
				bind(context,varName,responseVar);
			}
			return responseVar;
	}
	
	public final class AutoParsingResponseHandler implements ResponseHandler<Object>{
		final Object target;
		
		public AutoParsingResponseHandler(Object target){
			this.target=target;
		}

		public Object handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
			if(response.getStatusLine().getStatusCode()>=400){
				String message = "HTTP error "+response.getStatusLine().getStatusCode()+": "+response.getStatusLine().getReasonPhrase();
				HttpEntity entity = response.getEntity();
				if(entity!=null){
					message+=": "+EntityUtils.toString(entity);
				}
				throw new HttpResponseException(response.getStatusLine().getStatusCode(),message);
			}
			HttpEntity entity = response.getEntity();
			if(entity!=null){
				Header ct = response.getFirstHeader("Content-Type");
				if(ct!=null){
					String contentType = ct.getValue();
					String charset = "UTF-8";
					Matcher charMatcher = charsetPattern.matcher(contentType);
					if(charMatcher.find()){
						charset = charMatcher.group(1);
						//System.out.println("Found charset "+charset);
					}
					try {
						if(contentType.contains("json")){
							return Parse.parse(entity.getContent(), "json", target);
						}
					}
					catch(IOException e) {
						throw e;
					}
					catch(RuntimeException e) {
						throw e;
					}
					catch(Exception e) {
						throw new RuntimeException(e);
					}
					if(contentType.contains("xml")) {
						try {
							return Parse.parse(entity.getContent(), "xml", target);
						}
						catch(IOException e) {
							throw e;
						}
						catch(RuntimeException e) {
							throw e;
						}
						catch(Exception e) {
							throw new RuntimeException(e);
						}
					}
					//plain text
					if(contentType.contains("text")){
						return EntityUtils.toString(entity, charset);
					}
				}
				//no content type specified, return byte array and allow calling code to deal with it
				return EntityUtils.toByteArray(entity);
			}
			return null;
		}
		
	}
	
	final class TimeoutTask extends TimerTask{
		final HttpUriRequest request;
		
		public TimeoutTask(HttpUriRequest request){
			this.request=request;
		}
		
		@Override
		public void run() {
			request.abort();
		}
		
	}

	@SuppressWarnings({"rawtypes","unchecked"})
	private static void acceptInterceptor(Map variables){
		variables.put(INTERCEPTOR_BINDING, Optional.empty());
	}
	@SuppressWarnings({"rawtypes","unchecked"})
	private static Optional<HttpRequestInterceptor> resolveInterceptor(Map variables){
		return (Optional<HttpRequestInterceptor>)variables.remove(INTERCEPTOR_BINDING);
	}
	@SuppressWarnings({"rawtypes","unchecked"})
	public static void offerInterceptor(Map variables, HttpRequestInterceptor interceptor){
		variables.put(INTERCEPTOR_BINDING, Optional.of(interceptor));
	}

	static class HttpBodyDelete extends HttpEntityEnclosingRequestBase {
		public HttpBodyDelete(final URI uri) {
			super();
			setURI(uri);
		}

		@Override
		public String getMethod() {
			return "DELETE";
		}

	}
}

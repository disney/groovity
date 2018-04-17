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
package com.disney.groovity.websocket;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.tags.Credentials;
import com.disney.groovity.tags.Credentials.UserPass;
import com.disney.groovity.tags.Handler;
import com.disney.groovity.tags.Signature;
import com.disney.groovity.tags.Uri;
import com.disney.groovity.util.ScriptHelper;
import com.disney.http.auth.AuthConstants;
import com.disney.http.auth.AuthorizationRequest;
import com.disney.http.auth.DigestAuthorization;
import com.disney.http.auth.client.signer.HttpSignatureSigner;

import groovy.lang.Closure;
import groovy.lang.Writable;

/**
 * Open a websocket connection to a remote server
 * <p>
 * param( <ul>	
 *  <li><b>url</b>: 
 *	The URL of the websocket server endpoint to connect to,</li>
 *	<li><i>var</i>: 
 *	A variable name to store the websocket which can be used to send messages and close the connection,</li>	
 *	<li><i>message</i>: 
 *	java class name to try and parse for JSON messages, by default a String or byte[] is used to convey the message,</li>	
 *	<li><i>close</i>: 
 *	closure to execute when the socket is closed,</li>
 *  <li><i>error</i>: 
 *	closure to execute when the socket encounters an exception,</li>
 *	<li><i>timeout</i>: 
 *	Number of seconds of inactivity after which to close a socket, defaults to no timeout</li>
 *  </ul>{
 *	<blockquote>// param() and header() tags to add to the request, optional handler{} tag to control message processing; return value or stream output is sent to websocket as an opening message</blockquote>
 * 	});
 * 
 * <p><b>returns</b> WebSocket with call(message), isOpen(), close() methods
 *	
 *	<p>Sample
 *	<pre>
 *	ws(
 *		var:"mySocket",
 *		url:"ws://localhost:9880/someEndpoint",
 *		close:{
 *			log(info:"Socket closed")
 *		}
 *  ){
 *		param(name:'channel',value:123)
 *		handler{
 *			log(info:"Incoming message ${message}")
 *		}
 * 		"Hello Socket, I'm here"
 *  };
 *	</pre>	
 * @author Alex Vigdor
 */
@Tag(
		info="Open a websocket connection to a remote server",
		body="<g:param> and <g:header> tags to add to the request, optional <g:handler> tag to control message processing; return value or stream output is sent to websocket as an opening message",
		sample="ws(\n" + 
				"	var:\"mySocket\",\n" + 
				"	url:\"ws://localhost:9880/someEndpoint\",\n" + 
				"	close:{\n" + 
				"		log(info:\"Socket closed\")\n" + 
				"	}\n" + 
				"){\n" + 
				"	param(name:'channel',value:123)\n" + 
				"	handler{\n" + 
				"		log(info:\"Incoming message ${message}\")\n" + 
				"	}\n" + 
				"	\"Hello Socket, I'm here\"\n" + 
				"};",
		returns="WebSocket with call(message), isOpen(), close() methods",
		attrs={
		      @Attr(name=GroovityConstants.URL,info="The URL of the websocket server endpoint to connect to",required=true),
		      @Attr(name=GroovityConstants.VAR,info="A variable name to store the websocket which can be used to send messages and close the connection", required=false),
		      @Attr(name=GroovityConstants.MESSAGE, info="java class name to try and parse for JSON messages, by default a String or byte[] is used to convey the message", required = false),
		      @Attr(name=GroovityConstants.CLOSE, info="closure to execute when the socket is closed", required = false),
		      @Attr(name=GroovityConstants.ERROR, info="closure to execute when the socket encounters an exception", required = false),
		      @Attr(name=GroovityConstants.TIMEOUT, info="Number of seconds of inactivity after which to close a socket, defaults to no timeout", required=false),
		}
	)
public class Ws implements Taggable, AuthConstants{
	final static Logger log = Logger.getLogger(Ws.class.getName());
	
	protected HttpClient httpClient;
	protected java.util.Set<Session> openSessions;
	protected AtomicReference<WebSocketContainer> container = new AtomicReference<>();
	protected AtomicLong openCount = new AtomicLong(0);
	protected AtomicLong closeCount = new AtomicLong(0);
	protected AtomicLong errorCount = new AtomicLong(0);
	
	public void setGroovity(Groovity groovity){
		this.httpClient = groovity.getHttpClient();
	}
	
	public void init() {
		openSessions = ConcurrentHashMap.newKeySet();
	}
	
	public void destroy() {
		openSessions.forEach(session -> { 
			try {
				session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY,"Shutting down"));
			} catch (IOException e) {
				log.log(Level.WARNING, "Error closing client websocket", e);
			}
		});
		openSessions = null;
		container=null;
	}
	
	public long getOpenCount() {
		return openCount.get();
	}
	
	public long getCloseCount() {
		return closeCount.get();
	}
	
	public long getErrorCount() {
		return errorCount.get();
	}
	
	WebSocketContainer getContainer() {
		WebSocketContainer wc = container.get();
		if(wc==null) {
			wc = ContainerProvider.getWebSocketContainer();
			if(!container.compareAndSet(null, wc)) {
				wc = container.get();
			}
		}
		return wc;
	}
	
	@SuppressWarnings({"rawtypes","unchecked"})
	@Override
	public Object tag(Map attributes, Closure body) throws Exception {
		Object url = resolve(attributes,URL);
		if(url==null){
			throw new RuntimeException("ws() requires 'url' attribute");
		}
		ScriptHelper context = getScriptHelper(body);
		Map variables = context.getBinding().getVariables();
		URI uri;
		URIBuilder builder;
		ArrayList<Header> headers;
		Function handlerFunction;
		Optional<UserPass> userPass;
		Optional<HttpSignatureSigner> signer;
		final AtomicReference openMessage = new AtomicReference<>();
		try {
			builder = new URIBuilder(url.toString());
			bind(context,Uri.CURRENT_URI_BUILDER, builder);
			headers = new ArrayList<Header>();
			bind(context,com.disney.groovity.tags.Header.CURRENT_LIST_FOR_HEADERS, headers);
			Credentials.acceptCredentials(variables);
			Signature.acceptSigner(variables);
			Object oldOut = get(context,OUT);
			StringWriter sw = new StringWriter();
			Object rval = null;
			bind(context,OUT, sw);
			try{
				rval = body.call();
				if(rval instanceof Writable){
					((Writable)rval).writeTo(sw);
				}
			}
			finally{
				bind(context,OUT, oldOut);
				userPass = Credentials.resolveCredentials(variables);
				signer = Signature.resolveSigner(variables);
			}
			String val = sw.toString().trim();
			if(val.length()>0){
				openMessage.set(val);
			}
			else if(rval!=null){
				openMessage.set(rval);
			}
			uri = builder.build();
			handlerFunction = (Function) get(body,Handler.HANDLER_BINDING);
		}
		catch (URISyntaxException e1) {
			throw new RuntimeException("Invalid URI "+url,e1);
		}
		finally{
			unbind(context,Uri.CURRENT_URI_BUILDER);
			unbind(context,com.disney.groovity.tags.Header.CURRENT_LIST_FOR_HEADERS);
			unbind(context,Handler.HANDLER_BINDING);
		}
		final Closure closer = resolve(attributes,CLOSE,Closure.class);
		final Closure errorHandler = resolve(attributes,ERROR,Closure.class);
		final Class messageFormat = resolve(attributes, MESSAGE, Class.class);
		final Integer timeout = resolve(attributes,TIMEOUT,Integer.class);
		final AtomicReference<WebSocket> socket = new AtomicReference<>();
		ClientEndpointConfig.Builder configBuilder = ClientEndpointConfig.Builder.create();
		Session session;
		try {
			session = getContainer().connectToServer(
			new Endpoint() {
				@Override
				public void onOpen(Session session, EndpointConfig config) {
					try{
						openCount.incrementAndGet();
						if(timeout!=null){
							session.setMaxIdleTimeout(timeout*1000);
						}
						WebSocket ws = new WebSocket(session);
						socket.set(ws);
						ws.setName(uri.toString());
						if(handlerFunction!=null){
							ws.setMessageHandler(
								arg -> {
									synchronized(handlerFunction) { 
										handlerFunction.apply(arg); 
									}
								}, 
								messageFormat
							);
						}
						if(openMessage.get()!=null){
							ws.call(openMessage.get());
						}
					}
					catch(Exception e){
						log.log(Level.SEVERE,"Error opening web socket session "+uri,e);
					}
				}
				@Override
				public void onClose(Session session, CloseReason reason) {
					try {
						closeCount.incrementAndGet();
						openSessions.remove(session);
						if(closer!=null) {
							if(closer.getMaximumNumberOfParameters()>0) {
								closer.call(reason);
							}
							else {
								closer.call();
							}
						}
					}
					catch(Exception e){
						log.log(Level.SEVERE,"Error closing web socket session "+uri,e);
					}
				}
				@Override
				public void onError(Session session, Throwable th) {
					try{
						errorCount.incrementAndGet();
						if(errorHandler==null) {
							throw th;
						}
						errorHandler.call(th);
					}
					catch(Throwable e){
						Level logLevel = Level.WARNING;
						if(th!=e) {
							log.log(logLevel,"Error handling error for web socket session "+uri,e);
						}
						else if(th instanceof IOException) {
							logLevel = Level.FINE;
						}
						log.log(logLevel,"WebSocket client error: "+uri,th);
					}
				}
			},
			configBuilder.configurator(
				new ClientEndpointConfig.Configurator(){
					public void beforeRequest(Map<String,List<String>> reqHeaders){
						//copy programmatic headers
						for(Header header:headers){
							List<String> hl = reqHeaders.get(header.getName());
							if(hl==null){
								hl = new ArrayList<>();
								reqHeaders.put(header.getName(),hl);
							}
							hl.add(header.getValue());
						}
						Map<String,Map<String,String>> allChallenges = null;
						if(userPass.isPresent() || signer.isPresent()){
							allChallenges = getChallenges(uri, reqHeaders);
						}
						if(userPass.isPresent()){
							UserPass user = userPass.get();if(allChallenges!=null){
								List<String> auths = reqHeaders.get(AUTHORIZATION_HEADER);
								if(auths == null){
									auths = new ArrayList<>();
									reqHeaders.put(AUTHORIZATION_HEADER, auths);
								}
								if(allChallenges.containsKey("basic")){
									StringBuilder authBuilder= new StringBuilder(user.getUser());
									authBuilder.append(":");
									char[] pass = user.getPass();
									for(char c : pass){
										authBuilder.append(c);
									}
									try {
										auths.add("Basic "+printBase64Binary(authBuilder.toString().getBytes("UTF-8")));
									} catch (UnsupportedEncodingException e) {
										log.severe(e.getMessage());
									}
								}
								if(allChallenges.containsKey("digest")){
									final String digestUri = uri.getPath() + ((uri.getRawQuery()!=null) ?  "?"+uri.getRawQuery() : "");
									Map<String,String> digestChallenge = allChallenges.get("digest");
									if(log.isLoggable(Level.FINE)){
										log.fine("Generating digest auth for "+digestChallenge.toString());
									}
									DigestAuthorization digestAuth = new DigestAuthorization();
									digestAuth.setUsername(user.getUser());
									digestAuth.setQop("auth");
									digestAuth.setCnonce(String.valueOf(ThreadLocalRandom.current().nextLong(10000000,999999999999l)));
									digestAuth.setNonceCount("000001");
									digestAuth.setUri(digestUri);
									for(Entry<String, String> entry: digestChallenge.entrySet()){
										String k = entry.getKey();
										String v = entry.getValue();
										if("nonce".equalsIgnoreCase(k)){
											digestAuth.setNonce(v);
										}
										else if("realm".equalsIgnoreCase(k)){
											digestAuth.setRealm(v);
										}
										else if("opaque".equalsIgnoreCase(k)){
											digestAuth.setOpaque(v);
										}
									}
									String signingString;
									try {
										signingString = digestAuth.generateSigningString(user.getUser(), new String(user.getPass()), new AuthorizationRequest() {
											@Override
											public String getURI() {
												return digestUri;
											}
											
											@Override
											public String getMethod() {
												return "GET";
											}
											
											@Override
											public List<String> getHeaders(String name) {
												return reqHeaders.get(name);
											}
										});
										MessageDigest md5 = MessageDigest.getInstance("MD5");
										digestAuth.setDigest(md5.digest(signingString.toString().getBytes()));
										if(log.isLoggable(Level.FINE)){
											log.fine("Generated digest auth "+digestAuth.toString());
										}
										auths.add(digestAuth.toString());
									} catch (NoSuchAlgorithmException e) {
										log.severe("Missing MD5 "+e.getMessage());
									}
									
								}
							}
						}
						if(signer.isPresent()){
							if(allChallenges.containsKey("signature")){
								HttpSignatureSigner sig = signer.get();
								HttpGet signReq = createRequest(uri, reqHeaders);
								List<Header> beforeHeaders = Arrays.asList(signReq.getAllHeaders());
								try {
									sig.process(signReq, null);
								} catch (HttpException | IOException e) {
									log.log(Level.SEVERE,"Error processing http signature",e);
								}
								Header[] afterHeaders = signReq.getAllHeaders();
								for(Header h: afterHeaders){
									if(!beforeHeaders.contains(h)){
										List<String> hl = reqHeaders.get(h.getName());
										if(hl==null){
											hl = new ArrayList<>();
											reqHeaders.put(h.getName(),hl);
										}
										hl.add(h.getValue());
										if(log.isLoggable(Level.FINE)){
											log.fine("Copied HTTP signature header "+h);
										}
									}
								}
							}
						}
					}
				}).build(),
			uri);
		}
		catch(Exception e) {
			errorCount.incrementAndGet();
			throw e;
		}
		openSessions.add(session);
		String var = resolve(attributes,VAR,String.class);
		if(var!=null){
			context.getBinding().setVariable(var, socket.get());
		}
		return socket.get();
	}
	
	private String cleanValue(String value){
		value = value.trim();
		if(value.endsWith("\"")){
			value = value.substring(0, value.length()-1);
		}
		if(value.startsWith("\"")){
			value = value.substring(1);
		}
		return value;
	}
	
	private HttpGet createRequest(URI uri, Map<String,List<String>> headers){
		try {
			if(uri.getScheme().equalsIgnoreCase("ws")){
				uri = new URIBuilder(uri).setScheme("http").build();
			}
			else if(uri.getScheme().equalsIgnoreCase("wss")){
				uri = new URIBuilder(uri).setScheme("https").build();
			}
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		HttpGet httpGet = new HttpGet(uri);
		headers.entrySet().forEach( entry -> {
			String name = entry.getKey();
			entry.getValue().forEach(value -> {
				httpGet.addHeader(name, value);
			}); 
		});
		return httpGet;
	}
	
	private Map<String,Map<String,String>> getChallenges(URI uri, Map<String,List<String>> headers){
		if(log.isLoggable(Level.FINE)){
			log.fine("Attempting pre-auth request for challege "+uri);
		}
		try {
			HttpGet probeAuth = createRequest(uri, headers); 
			String challenge = httpClient.execute(probeAuth, new ResponseHandler<String>() {
				public String handleResponse(HttpResponse response){
					try{
						if(response.getStatusLine().getStatusCode()==401){
							return response.getFirstHeader(WWW_AUTHENTICATE_HEADER).getValue();
						}
					}
					finally{
						HttpEntity entity = response.getEntity();
						if(entity!=null){
							EntityUtils.consumeQuietly(entity);
						}
					}
					return null;
				}
			});
			if(challenge!=null){
				if(log.isLoggable(Level.FINE)){
					log.fine("Received challenge "+challenge);
				}
				String[] segments = challenge.split("\\s*,\\s*");
				int cPos = -1;
				Map<String,Map<String,String>> allChallenges = new LinkedHashMap<>();
				String currentChallengeType=null;
				Map<String,String> currentChallengeProps=null;
				for(String segment: segments){
					int eq = segment.indexOf("=");
					String start = segment.substring(0,eq).trim();
					if((cPos=start.indexOf(" "))>0){
						if(currentChallengeType!=null){
							allChallenges.put(currentChallengeType, currentChallengeProps);
						}
						currentChallengeType = start.substring(0,cPos).toLowerCase();
						currentChallengeProps = new LinkedHashMap<>();
						currentChallengeProps.put(start.substring(cPos+1), cleanValue(segment.substring(eq+1)));
					}
					else{
						currentChallengeProps.put(start, cleanValue(segment.substring(eq+1)));
					}
				}
				if(currentChallengeType!=null){
					allChallenges.put(currentChallengeType, currentChallengeProps);
				}
				if(log.isLoggable(Level.FINE)){
					log.fine("Received challenges "+allChallenges);
				}
				return allChallenges;
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error establishing auth handshake", e);
		}
		return null;
	}

}

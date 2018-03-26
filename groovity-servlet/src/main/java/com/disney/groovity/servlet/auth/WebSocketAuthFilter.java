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
package com.disney.groovity.servlet.auth;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.disney.groovity.servlet.GroovityScriptView;
import com.disney.groovity.servlet.GroovityScriptViewFactory;
import com.disney.groovity.servlet.GroovityServlet;
import com.disney.groovity.servlet.cors.CORSProcessor;
import com.disney.http.auth.AuthConstants;
import com.disney.http.auth.server.AuthenticatedRequestWrapper;
import com.disney.http.auth.server.ServletAuthorizationRequest;
import com.disney.http.auth.server.VerifierResult;
/**
 * A filter that monitors Upgrade calls under /ws/ and applies authentication as defined by the underlying sockets
 *
 * @author Alex Vigdor
 */
public class WebSocketAuthFilter implements Filter {
	private static final String ORIGIN_HEADER = "Origin";
	private static final String HOST_HEADER = "Host";
	private static final String UPGRADE_HEADER = "Upgrade";
	private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final Logger log = Logger.getLogger(WebSocketAuthFilter.class.getSimpleName());
	private GroovityScriptViewFactory factory;
	
	@Override
	public void init(FilterConfig arg0) throws ServletException { }

	@Override
	public void destroy() {}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		if(factory==null){
			this.factory = (GroovityScriptViewFactory) req.getServletContext().getAttribute(GroovityServlet.SERVLET_CONTEXT_GROOVITY_VIEW_FACTORY);
		}
		if(req instanceof HttpServletRequest){
			HttpServletRequest hreq = (HttpServletRequest) req;
			HttpServletResponse hres = (HttpServletResponse) res;
			if("websocket".equalsIgnoreCase(hreq.getHeader(UPGRADE_HEADER))){
				String requestPath = hreq.getPathInfo();
				if(requestPath==null){
					//when running as default servlet fall back
					requestPath = hreq.getServletPath();
				}
				String socketName = null;
				if(requestPath.startsWith("ws/")){
					socketName = requestPath.substring(3);
				}
				else if(requestPath.startsWith("/ws/")){
					socketName = requestPath.substring(4);
				}
				if(socketName!=null){
					if(log.isLoggable(Level.FINE)){
						log.fine("VALIDATING WEB SOCKET REQUEST for socket "+socketName+" "+hreq.getHeader("authorization"));
					}
					try {
						GroovityScriptView gsv = factory.getSocketByName(socketName);
						if(gsv!=null) {
							if(gsv.getVerifier()!=null){
								VerifierResult vf = gsv.getVerifier().verify(new ServletAuthorizationRequest(hreq));
								if(vf.getAuthenticationInfo()!=null){
									hres.setHeader(AuthConstants.AUTHENTICATION_INFO,	vf.getAuthenticationInfo());
								}
								if(vf.isAuthenticated()){
									if(vf.isAuthorized()){
										if(vf.getPrincipal()!=null) {
											hreq = new AuthenticatedRequestWrapper(hreq, vf.getPrincipal());
										}
									}
									else{
										if(log.isLoggable(Level.FINE)){
											log.fine("Verification failed 403 "+vf.getMessage()+", challenge "+vf.getChallenge());
										}
										hres.sendError(403, vf.getMessage());
										return;
									}
								}
								else{
									if(vf.getChallenge()!=null){
										hres.setHeader(AuthConstants.WWW_AUTHENTICATE_HEADER, vf.getChallenge());
									}
									if(log.isLoggable(Level.FINE)){
										log.fine("Verification failed 401 "+vf.getMessage()+", challenge "+vf.getChallenge());
									}
									hres.sendError(401,vf.getMessage());
									return;
								}
								if(log.isLoggable(Level.FINE)){
									log.fine("Verification succeeded for "+vf.getPrincipal());
								}
							}
							String origin = hreq.getHeader(ORIGIN_HEADER);
							String host = hreq.getHeader(HOST_HEADER);
							if(hreq.isSecure()) {
								host = "https://".concat(host);
							}
							else {
								host = "http://".concat(host);
							}
							if(host.equals(origin)) {
								//default CORS behavior, allow same-origin requests
								if(log.isLoggable(Level.FINE)){
									log.fine("WebSocket Origin "+origin+" matches host "+host);
								}
							}
							else {
								AtomicBoolean allowed = new AtomicBoolean(false);
								CORSProcessor cp = gsv.getCORSProcessor();
								if(cp!=null) {
									cp.process(hreq, new HttpServletResponseWrapper(hres) {
										public void setHeader(String name, String value) {
											if(ACCESS_CONTROL_ALLOW_ORIGIN.equals(name)) {
												allowed.set(true);
											}
											super.setHeader(name, value);
										}
									});
								}
								if(!allowed.get()) {
									if(log.isLoggable(Level.FINE)){
										log.fine("Disallowing websocket due to cors violation from "+origin+" to host "+host);
									}
									hres.sendError(403, "Origin not allowed");
									return;
								}
							}
						}
					} catch (Exception e) {
						throw new ServletException(e);
					}
				}
			}
			chain.doFilter(hreq, hres);
		}
		
	}
}
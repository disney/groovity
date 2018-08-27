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
package com.disney.groovity.portal;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.disney.groovity.Groovity;
import com.disney.groovity.servlet.GroovityServlet;
import com.disney.http.auth.server.AuthenticatedRequestWrapper;

import groovy.lang.Binding;
import groovy.lang.Script;
/**
 * Add authenticated portal user and roles to the servlet request if present in the session; this will make the principal available to servlets and sockets,
 * and allows adminAuthPolicy to act based on both user and roles
 *
 * @author Alex Vigdor
 */
public class PortalSessionAuthorizationFilter implements Filter{
	ServletContext servletContext;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		servletContext = filterConfig.getServletContext();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if(request instanceof HttpServletRequest){
			HttpServletRequest hr = (HttpServletRequest) request;
			HttpSession session = hr.getSession(false);
			if(session!=null){
				Object userId = session.getAttribute("userId");
				if(userId != null){
					Groovity groovity = (Groovity) servletContext.getAttribute(GroovityServlet.SERVLET_CONTEXT_GROOVITY_INSTANCE);
					Script factory;
					try {
						factory = groovity.load("/data/factory", new Binding());
					} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
						throw new ServletException(e);
					}
					Object user = factory.invokeMethod("call", new String[]{"person",userId.toString()});
					if(user!=null && user instanceof Principal){
						request = new AuthenticatedRequestWrapper(hr, (Principal)user);
					}
				}
			}
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		
	}

}

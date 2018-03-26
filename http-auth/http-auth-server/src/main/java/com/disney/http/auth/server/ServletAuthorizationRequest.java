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
package com.disney.http.auth.server;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Expose servlet request for authorization signing
 *
 * @author Alex Vigdor
 */
public class ServletAuthorizationRequest implements ServerAuthorizationRequest{
	private final HttpServletRequest httpRequest;
	
	public ServletAuthorizationRequest(final HttpServletRequest httpRequest){
		this.httpRequest=httpRequest;
	}

	@Override
	public String getMethod() {
		return httpRequest.getMethod();
	}

	@Override
	public String getURI() {
		String uri = httpRequest.getRequestURI();
		if(httpRequest.getQueryString()!=null){
			uri = uri+"?"+httpRequest.getQueryString();
		}
		return uri;
	}

	@Override
	public List<String> getHeaders(String headerName) {
		return Collections.list(httpRequest.getHeaders(headerName));
	}

	@Override
	public Principal getUserPrincipal() {
		return httpRequest.getUserPrincipal();
	}

	@Override
	public boolean isUserInRole(String role) {
		return httpRequest.isUserInRole(role);
	}

	@Override
	public String getRemoteUser() {
		return httpRequest.getRemoteUser();
	}

	@Override
	public HttpSession getSession(boolean create) {
		return httpRequest.getSession(create);
	}
	
	@Override
	public HttpSession getSession() {
		return httpRequest.getSession();
	}

	@Override
	public Object getAttribute(String name) {
		return httpRequest.getAttribute(name);
	}

}

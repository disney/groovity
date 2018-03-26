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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xml.sax.InputSource;

import com.disney.http.auth.AuthConstants;
import com.disney.http.auth.server.policy.XmlPolicyParser;

/**
 * Servlet filter that loads verifiers from a 'configFile' specified as an init-param, and apply the loaded verifiers to gate incoming requests
 * 
 * @author Alex Vigdor
 *
 */
public class VerificationFilter implements Filter, AuthConstants {
	Verifier verifier;

	@Override
	public void destroy() {
		
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		if(verifier!=null){
			try {
				HttpServletRequest hreq = (HttpServletRequest)req;
				ServerAuthorizationRequest authReq = new ServletAuthorizationRequest(hreq);
				HttpServletResponse response = ((HttpServletResponse)res);
				VerifierResult vf = verifier.verify(authReq);
				if(vf.getAuthenticationInfo()!=null){
					response.setHeader(AUTHENTICATION_INFO,	vf.getAuthenticationInfo());
				}
				if(vf.isAuthenticated()){
					if(vf.isAuthorized()){
						AuthenticatedRequestWrapper arw = new AuthenticatedRequestWrapper(hreq, vf.getPrincipal());
						chain.doFilter(arw, res);
					}
					else{
						response.sendError(403, vf.getMessage());
					}
				}
				else{
					if(vf.getChallenge()!=null){
						response.setHeader(WWW_AUTHENTICATE_HEADER, vf.getChallenge());
					}
					response.sendError(401,vf.getMessage());
				}
			} catch (Exception e) {
				throw new ServletException(e);
			} 
		}
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		String configFile = config.getInitParameter("configFile");
		if(configFile==null){
			throw new ServletException("Verification Filter requires a configFile init-param");
		}
		InputStream stream = config.getServletContext().getResourceAsStream(configFile);
		if(stream == null){
			stream = getClass().getResourceAsStream(configFile);
		}
		if(stream==null){
			File file = new File(configFile);
			if(file.exists()){
				try {
					stream = new FileInputStream(file);
				} catch (FileNotFoundException e) {
					throw new ServletException(e);
				}
			}
		}
		if(stream==null){
			throw new ServletException("No verification config file found at "+configFile);
		}
		try {
			verifier = XmlPolicyParser.parsePolicy(new InputSource(stream), config.getServletContext());
		} catch (Exception e) {
			throw new ServletException("Unable to parse Verification Filter configuration",e);
		} 
		finally{
			if(stream!=null){
				try {
					stream.close();
				} catch (IOException e) {
					throw new ServletException(e);
				}
			}
		}
	}

}

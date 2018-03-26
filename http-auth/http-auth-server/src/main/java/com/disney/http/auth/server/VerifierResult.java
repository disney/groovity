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

/**
 * represents the results of a verification attempt; if authorized==true, the attached principal represents the authenticated user or keyId.
 * If authorized==false; either another verifier should be used, or if a challenge is present a 401 response
 * should be issued, otherwise a 403 should result
 * 
 * @author Alex Vigdor
 *
 */
public class VerifierResult {
	private boolean authorized=true;
	private boolean authenticated=false;
	private String challenge;
	private String authenticationInfo;
	private String message;
	private Principal principal;
	
	public boolean isAuthorized() {
		return authorized;
	}
	public void setAuthorized(boolean authorized) {
		this.authorized = authorized;
	}
	public String getChallenge() {
		return challenge;
	}
	public void setChallenge(String challenge) {
		this.challenge = challenge;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public boolean isAuthenticated() {
		return authenticated;
	}
	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}
	public String getAuthenticationInfo() {
		return authenticationInfo;
	}
	public void setAuthenticationInfo(String authenticationInfo) {
		this.authenticationInfo = authenticationInfo;
	}
	public Principal getPrincipal() {
		return principal;
	}
	public void setPrincipal(Principal principal) {
		this.principal = principal;
	} 
}

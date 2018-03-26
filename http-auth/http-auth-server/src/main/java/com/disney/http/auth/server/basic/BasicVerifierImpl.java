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
package com.disney.http.auth.server.basic;

import java.util.List;

import com.disney.http.auth.server.AbstractVerifier;
import com.disney.http.auth.server.AuthenticatedPrincipal;
import com.disney.http.auth.server.ServerAuthorizationRequest;
import com.disney.http.auth.AuthConstants;
import com.disney.http.auth.BasicAuthorization;
import com.disney.http.auth.server.Verifier;
import com.disney.http.auth.server.VerifierResult;
/**
 * Basic authentication verifier; relies on one or more PasswordChecker implementations to determine the validity of credentials
 *
 * @author Alex Vigdor
 */
public class BasicVerifierImpl extends AbstractVerifier implements Verifier, AuthConstants {
	private List<PasswordChecker> passwordCheckers;

	@Override
	protected VerifierResult doVerifyInternal(ServerAuthorizationRequest request)  {
		VerifierResult result = new VerifierResult();
		List<String> basicAuth = request.getHeaders(AUTHORIZATION_HEADER);
		if(basicAuth==null || basicAuth.isEmpty()){
			challenge(result,ERROR_MISSING_CREDENTIALS);
			return result;
		}
		BasicAuthorization auth;
		try{
			auth = new BasicAuthorization(basicAuth.get(0));
		}
		catch(Exception e){
			challenge(result,e.getMessage());
			return result;
		}
		for(int i=0;i<passwordCheckers.size();i++){
			PasswordChecker checker = passwordCheckers.get(i);
			if(checker.check(auth.getUsername(), auth.getPassword())){
				result.setAuthenticated(true);
				result.setPrincipal(new AuthenticatedPrincipal(auth.getUsername()));
				return result;
			}
		}
		challenge(result,ERROR_UNKNOWN_CREDENTIALS);
		return result;
	}
	
	private void challenge(VerifierResult result, String message){
		StringBuilder authn = new StringBuilder(BASIC);
		authn.append(" ").append(REALM).append("=\"").append(getRealm()).append("\"");
		result.setChallenge(authn.toString());
		result.setAuthenticated(false);
		result.setMessage(message);
	}

	public List<PasswordChecker> getPasswordCheckers() {
		return passwordCheckers;
	}

	public void setPasswordCheckers(List<PasswordChecker> passwordCheckers) {
		this.passwordCheckers = passwordCheckers;
	}


}

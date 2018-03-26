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

import java.util.List;

import com.disney.http.auth.AuthConstants;
/**
 * Abstract base class for digest and signature verifiers
 *
 * @author Alex Vigdor
 */
public abstract class AbstractVerifier implements Verifier, AuthConstants {
	private String realm;
	private List<AccessController> accessControllers;

	@Override
	public VerifierResult verify(ServerAuthorizationRequest request) throws Exception {
		VerifierResult vr = doVerifyInternal(request);
		if(vr.isAuthenticated()){
			//second level, check access controllers for authorization
			if(vr.isAuthorized() && accessControllers!=null && accessControllers.size()>0){
				vr.setAuthorized(false);
				for(int i=0;i<accessControllers.size();i++){
					if(accessControllers.get(i).allow(vr.getPrincipal(), request)){
						vr.setAuthorized(true);
						break;
					}
				}
				
			}
			if(!vr.isAuthorized()){
				vr.setMessage(ERROR_NOT_AUTHORIZED);
			}
		}
		return vr;
	}

	protected abstract VerifierResult doVerifyInternal(ServerAuthorizationRequest request) throws Exception;
	
	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public List<AccessController> getAccessControllers() {
		return accessControllers;
	}

	public void setAccessControllers(List<AccessController> accessControllers) {
		this.accessControllers = accessControllers;
	}

}

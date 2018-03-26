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

/**
 * Allow multiple verifiers to coexist in the same auth policy; the first to successfully authenticate a request is honored,
 * even if it does not authorize the request.  If none authenticate, the challenges are concatenated to present a menu of choices
 * to clients.
 * 
 * @author Alex Vigdor
 *
 */
public class VerifierChain implements Verifier {
	private List<Verifier> verifiers;
	
	public VerifierChain(){
		
	}
	
	public VerifierChain(List<Verifier> verifiers){
		this.verifiers=verifiers;
	}

	@Override
	public VerifierResult verify(ServerAuthorizationRequest request) throws Exception {
		VerifierResult fail = new VerifierResult();
		for(int i=0;i<verifiers.size();i++){
			Verifier v = verifiers.get(i);
			VerifierResult vf = v.verify(request);
			if(vf.isAuthenticated()){
				return vf;
			}
			else{
				if(fail.getMessage()==null){
					fail.setMessage(vf.getMessage());
				}
				if(fail.getAuthenticationInfo()==null){
					fail.setAuthenticationInfo(vf.getAuthenticationInfo());
				}
				if(vf.getChallenge()!=null){
					String oldChallenge = fail.getChallenge();
					if(oldChallenge==null){
						oldChallenge="";
					}
					else{
						oldChallenge=oldChallenge+", ";
					}
					fail.setChallenge(oldChallenge+vf.getChallenge());
				}
			}
		}
		return fail;
	}

	public List<Verifier> getVerifiers() {
		return verifiers;
	}

	public void setVerifiers(List<Verifier> verifiers) {
		this.verifiers = verifiers;
	}

}

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
package com.disney.http.auth.server.policy;


import java.util.concurrent.Callable;

import com.disney.http.auth.server.AbstractVerifier;
import com.disney.http.auth.server.ServerAuthorizationRequest;
import com.disney.http.auth.server.Verifier;
import com.disney.http.auth.server.VerifierResult;

/**
 * A policy verifier using a verifier provided by a loader, e.g. using URIParcel to load remote configurations
 * 
 * @author Alex Vigdor
 *
 */
public class PolicyVerifierImpl extends AbstractVerifier {
	private Callable<Verifier> policyLoader;

	@Override
	protected VerifierResult doVerifyInternal(ServerAuthorizationRequest request) throws Exception {
		Verifier vf = getPolicyLoader().call();
		if(vf!=null){
			return vf.verify(request);
		}
		VerifierResult vr = new VerifierResult();
		vr.setMessage("No verifier found in loaded auth policy");
		return vr;
	}

	public Callable<Verifier> getPolicyLoader() {
		return policyLoader;
	}

	public void setPolicyLoader(Callable<Verifier> policyLoader) {
		this.policyLoader = policyLoader;
	}


}

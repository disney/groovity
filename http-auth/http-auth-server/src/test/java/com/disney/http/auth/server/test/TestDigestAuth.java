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
package com.disney.http.auth.server.test;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.disney.http.auth.AuthConstants;
import com.disney.http.auth.DigestAuthorization;
import com.disney.http.auth.server.ACLAccessControllerImpl;
import com.disney.http.auth.server.AccessController;
import com.disney.http.auth.server.ServerAuthorizationRequest;
import com.disney.http.auth.server.ServletAuthorizationRequest;
import com.disney.http.auth.server.VerifierResult;
import com.disney.http.auth.server.digest.DigestVerifierImpl;
import com.disney.http.auth.server.digest.MapPasswordDigester;
import com.disney.http.auth.server.digest.PasswordDigester;

public class TestDigestAuth implements AuthConstants {
	@Test
	public void testDigest() throws Exception{
		DigestVerifierImpl verifier = new DigestVerifierImpl();
		Map<String,String> pmap = new HashMap<String,String>();
		List<String> accessList = new ArrayList<String>();
		ACLAccessControllerImpl acl = new ACLAccessControllerImpl();
		acl.setAcl(accessList);
		pmap.put("mykey", "mypass");
		PasswordDigester pc = new MapPasswordDigester(pmap);
		verifier.setPasswordDigesters(Arrays.asList(pc));
		verifier.setAccessControllers(Arrays.asList((AccessController)acl));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/");
		ServerAuthorizationRequest areq = new ServletAuthorizationRequest(request);
		VerifierResult result = verifier.verify(areq);
		Assert.assertEquals(ERROR_MISSING_CREDENTIALS, result.getMessage());
		
		String challenge = result.getChallenge();
		Pattern noncePattern = Pattern.compile("nonce=\"([^\"]+)\"");
		Matcher matcher = noncePattern.matcher(challenge);
		if(!matcher.find()){
			throw new Exception("No nonce found in challenge");
		}
		String nonce = matcher.group(1);
		Pattern opaquePattern = Pattern.compile("opaque=\"([^\"]+)\"");
		matcher = opaquePattern.matcher(challenge);
		if(!matcher.find()){
			throw new Exception("No opaque found in challenge");
		}
		String opaque = matcher.group(1);
		
		DigestAuthorization ad = new DigestAuthorization();
		ad.setNonce(nonce);
		ad.setCnonce("ClientNonce");
		ad.setNonceCount("000001");
		ad.setOpaque(opaque);
		ad.setQop("auth");
		ad.setUri("/");
		ad.setUsername("mykey");
		ad.setDigest(new byte[0]);
		ad.setRealm(verifier.getRealm());
		request.addHeader("Authorization", ad.toString());
		result = verifier.verify(areq);
		Assert.assertEquals(ERROR_UNKNOWN_CREDENTIALS, result.getMessage());
		
		//now fix the digest
		/*
		StringBuilder signingString = new StringBuilder();
		signingString.append(digest("mykey",verifier.getRealm(),"mypass"));
		signingString.append(":").append(nonce).append(":").append(ad.getNonceCount()).append(":").append(ad.getCnonce()).append(":auth:");
		signingString.append(digest("GET",ad.getUri()));
		*/
		
		request = new MockHttpServletRequest();
		areq = new ServletAuthorizationRequest(request);
		request.setMethod("GET");
		request.setRequestURI("/");
		
		String signingString = ad.generateSigningString("mykey", "mypass",new ServletAuthorizationRequest(request));
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		ad.setDigest(md5.digest(signingString.toString().getBytes()));
		request.addHeader("Authorization", ad.toString());
		result = verifier.verify(areq);
		Assert.assertTrue("Expected successful authentication",result.isAuthenticated());
		Assert.assertFalse("Expected failed authorization",result.isAuthorized());
		
		accessList.add("mykey");
		result = verifier.verify(areq);
		Assert.assertTrue("Expected successful authentication",result.isAuthenticated());
		Assert.assertTrue("Expected successful authorization",result.isAuthorized());
	}
	

}

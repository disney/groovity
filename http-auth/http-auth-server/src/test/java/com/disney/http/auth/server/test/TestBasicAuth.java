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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.disney.http.auth.AuthConstants;
import com.disney.http.auth.server.ACLAccessControllerImpl;
import com.disney.http.auth.server.AccessController;
import com.disney.http.auth.server.ServerAuthorizationRequest;
import com.disney.http.auth.server.ServletAuthorizationRequest;
import com.disney.http.auth.server.VerifierResult;
import com.disney.http.auth.server.basic.BasicVerifierImpl;
import com.disney.http.auth.server.basic.MapPasswordChecker;
import com.disney.http.auth.server.basic.PasswordChecker;


public class TestBasicAuth implements AuthConstants{
	@Test
	public void testBasic() throws Exception{
		BasicVerifierImpl verifier = new BasicVerifierImpl();
		Map<String,String> pmap = new HashMap<String,String>();
		List<String> accessList = new ArrayList<String>();
		ACLAccessControllerImpl acl = new ACLAccessControllerImpl();
		acl.setAcl(accessList);
		pmap.put("mykey", "mypass");
		PasswordChecker pc = new MapPasswordChecker(pmap);
		verifier.setPasswordCheckers(Arrays.asList(pc));
		verifier.setAccessControllers(Arrays.asList((AccessController)acl));
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServerAuthorizationRequest areq = new ServletAuthorizationRequest(request);
		VerifierResult result = verifier.verify(areq);
		Assert.assertEquals(ERROR_MISSING_CREDENTIALS, result.getMessage());
		request.addHeader("Authorization", "Basic "+DatatypeConverter.printBase64Binary("mykey:wrongpass".getBytes()));
		result = verifier.verify(areq);
		Assert.assertEquals(ERROR_UNKNOWN_CREDENTIALS, result.getMessage());
		
		request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Basic "+DatatypeConverter.printBase64Binary("mykey:mypass".getBytes()));
		areq = new ServletAuthorizationRequest(request);
		result = verifier.verify(areq);
		Assert.assertTrue("Expected successful authentication",result.isAuthenticated());
		Assert.assertFalse("Expected failed authorization",result.isAuthorized());
		
		accessList.add("mykey");
		result = verifier.verify(areq);
		Assert.assertTrue("Expected successful authentication",result.isAuthenticated());
		Assert.assertTrue("Expected successful authorization",result.isAuthorized());
	}
}

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
package com.disney.groovity.servlet.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.junit.Assert;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;

import com.disney.groovity.servlet.GroovityServlet;

public class GroovityServletTest {
	static GroovityServlet groovity;
	
	@BeforeClass
	public static void init() throws ServletException{
		groovity = new GroovityServlet();
		MockServletConfig config = new MockServletConfig();
		config.addInitParameter(GroovityServlet.SOURCE_LOCATION_PARAM, "src/test/resources");
		config.addInitParameter(GroovityServlet.BINDING_DECORATOR, "com.disney.groovity.servlet.test.TestCustomBindingDecorator");
		config.addInitParameter(GroovityServlet.ERROR_CHAIN_DECORATOR, "com.disney.groovity.servlet.test.TestCustomErrorChainDecorator");
		groovity.init(config);
	}
	
	@Test
	public void testPath() throws ServletException, IOException{
		String uri = "/foo/Bartholomew";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
		request.setPathInfo(uri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		groovity.service(request, response);
		Assert.assertEquals("Hello Bartholomew",response.getContentAsString().trim());
		
		request = new MockHttpServletRequest("POST", uri);
		request.setPathInfo(uri);
		response = new MockHttpServletResponse();
		groovity.service(request, response);
		Assert.assertEquals("Farewell Bartholomew",response.getContentAsString().trim());
		
		request = new MockHttpServletRequest("DELETE", uri);
		request.setPathInfo(uri);
		response = new MockHttpServletResponse();
		groovity.service(request, response);
		Assert.assertEquals(405, response.getStatus());
		String allowHeader = response.getHeader("Allow");
		Assert.assertNotNull(allowHeader);
		String[] ah = allowHeader.split(", ");
		List<String> ahl = Arrays.asList(ah);
		Assert.assertTrue("Expected GET in Allowed",ahl.contains("GET"));
		Assert.assertTrue("Expected PUT in Allowed",ahl.contains("PUT"));
		Assert.assertTrue("Expected POST in Allowed",ahl.contains("POST"));
		
		request = new MockHttpServletRequest("HEAD", uri);
		request.setPathInfo(uri);
		response = new MockHttpServletResponse();
		groovity.service(request, response);
		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("",response.getContentAsString().trim());
	}
	
	@Test
	public void testCustomBindingDecorator() throws Exception{
		Map<String,Object> binding = new HashMap<>();
		groovity.getGroovityScriptViewFactory().getGroovity().getBindingDecorator().decorateRecursive(binding);
		Assert.assertEquals("bar", binding.get("foo"));
	}
	

	@Test
	public void testCustomErrorDecorator() throws Exception{
		String uri = "/stayAway";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
		request.setPathInfo(uri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		groovity.service(request, response);
		Assert.assertEquals(405, response.getStatus());
	}
	
	
}

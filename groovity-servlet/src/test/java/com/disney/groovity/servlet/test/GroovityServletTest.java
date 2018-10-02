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
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.xml.bind.DatatypeConverter;

import static org.junit.Assert.*;

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
	
	private MockHttpServletRequest makereq(String method, String uri) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
		request.setPathInfo(uri);
		return request;
	}

	@Test
	public void testPath() throws ServletException, IOException{
		String uri = "/foo/Bartholomew";
		MockHttpServletRequest request = makereq("GET", "/foo/Bartholomew");
		MockHttpServletResponse response = new MockHttpServletResponse();
		groovity.service(request, response);
		assertEquals("Hello Bartholomew",response.getContentAsString().trim());
		
		request = makereq("POST", uri);
		response = new MockHttpServletResponse();
		groovity.service(request, response);
		assertEquals("Farewell Bartholomew",response.getContentAsString().trim());
		
		request = makereq("DELETE", uri);
		response = new MockHttpServletResponse();
		groovity.service(request, response);
		assertEquals(405, response.getStatus());
		String allowHeader = response.getHeader("Allow");
		assertNotNull(allowHeader);
		String[] ah = allowHeader.split(", ");
		List<String> ahl = Arrays.asList(ah);
		assertTrue("Expected GET in Allowed",ahl.contains("GET"));
		assertTrue("Expected PUT in Allowed",ahl.contains("PUT"));
		assertTrue("Expected POST in Allowed",ahl.contains("POST"));
		
		request = makereq("HEAD", uri);
		response = new MockHttpServletResponse();
		groovity.service(request, response);
		assertEquals(200, response.getStatus());
		assertEquals("",response.getContentAsString().trim());
	}
	
	@Test
	public void testCustomBindingDecorator() throws Exception{
		Map<String,Object> binding = new HashMap<>();
		groovity.getGroovityScriptViewFactory().getGroovity().getBindingDecorator().decorateRecursive(binding);
		assertEquals("bar", binding.get("foo"));
	}
	

	@Test
	public void testCustomErrorDecorator() throws Exception{
		String uri = "/stayAway";
		MockHttpServletRequest request = makereq("GET", uri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		groovity.service(request, response);
		assertEquals(405, response.getStatus());
	}

	@Test
	public void testBuffered() throws Exception{
		HashSet<String> seenHashes = new HashSet<>();
		assertTrue(seenHashes.add(doBuffered("abcdefg")));
		assertTrue(seenHashes.add(doBuffered("abcdefgh")));
		assertTrue(seenHashes.add(doBuffered("hbcdefg")));
		assertTrue(seenHashes.add(doBuffered("poiuytrewqlkjhgfdsambvcxz0987654321")));
		byte[] underflow = new byte[9999];
		Arrays.fill(underflow, (byte) 98);
		assertTrue(seenHashes.add(doBuffered(new String(underflow))));
		assertFalse(seenHashes.add(doBuffered("abcdefg")));
		assertFalse(seenHashes.add(doBuffered("abcdefgh")));
		assertFalse(seenHashes.add(doBuffered("hbcdefg")));
		assertFalse(seenHashes.add(doBuffered("poiuytrewqlkjhgfdsambvcxz0987654321")));
		byte[] overflow = new byte[10241];
		Arrays.fill(overflow, (byte) 100);
		MockHttpServletResponse response = bufferedResponse(new String(overflow));
		assertNull(response.getHeader("ETag"));
		assertEquals(0, response.getContentLength());
	}

	private String doBuffered(String input) throws Exception{
		MessageDigest digest = MessageDigest.getInstance("MD5");
		byte[] hash = digest.digest(input.getBytes());
		String hashStr = "\""+DatatypeConverter.printBase64Binary(hash)+"\"";
		MockHttpServletResponse response = bufferedResponse(input);
		assertEquals(input.length(), response.getContentLength());
		assertEquals(hashStr, response.getHeader("ETag"));
		return hashStr;
	}

	private MockHttpServletResponse bufferedResponse(String input) throws Exception {
		MockHttpServletRequest request = makereq("GET", "/buffer/"+input);
		MockHttpServletResponse response = new MockHttpServletResponse();
		groovity.service(request, response);
		return response;
	}

	@Test
	public void testBufferedBytes() throws Exception{
		HashSet<String> seenHashes = new HashSet<>();
		assertTrue(seenHashes.add(doBufferedBytes("abcdefg")));
		assertTrue(seenHashes.add(doBufferedBytes("abcdefgh")));
		assertTrue(seenHashes.add(doBufferedBytes("hbcdefg")));
		assertTrue(seenHashes.add(doBufferedBytes("poiuytrewqlkjhgfdsambvcxz0987654321")));
		byte[] underflow = new byte[9999];
		Arrays.fill(underflow, (byte) 98);
		assertTrue(seenHashes.add(doBufferedBytes(new String(underflow))));
		assertFalse(seenHashes.add(doBufferedBytes("abcdefg")));
		assertFalse(seenHashes.add(doBufferedBytes("abcdefgh")));
		assertFalse(seenHashes.add(doBufferedBytes("hbcdefg")));
		assertFalse(seenHashes.add(doBufferedBytes("poiuytrewqlkjhgfdsambvcxz0987654321")));
		byte[] overflow = new byte[10241];
		Arrays.fill(overflow, (byte) 100);
		MockHttpServletResponse response = bufferedBytesResponse(new String(overflow));
		assertNull(response.getHeader("ETag"));
		assertEquals(0, response.getContentLength());
	}

	private String doBufferedBytes(String input) throws Exception{
		MessageDigest digest = MessageDigest.getInstance("MD5");
		byte[] hash = digest.digest(input.getBytes());
		String hashStr = "\""+DatatypeConverter.printBase64Binary(hash)+"\"";
		MockHttpServletResponse response = bufferedBytesResponse(input);
		assertEquals(input.length(), response.getContentLength());
		assertEquals(hashStr, response.getHeader("ETag"));
		return hashStr;
	}

	private MockHttpServletResponse bufferedBytesResponse(String input) throws Exception {
		MockHttpServletRequest request = makereq("GET", "/bufferBytes/"+input);
		MockHttpServletResponse response = new MockHttpServletResponse();
		groovity.service(request, response);
		return response;
	}

}

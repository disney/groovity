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

import org.junit.Test;

import com.disney.groovity.servlet.cors.CORSOrigin;
import org.junit.Assert;

public class CorsTest {
	@Test
	public void testCorsOrigins(){
		CORSOrigin co1 = new CORSOrigin("*.google.com");
		Assert.assertEquals(null, co1.getScheme());
		Assert.assertEquals("google.com", co1.getDomain());
		Assert.assertEquals((Integer) 80, co1.getPort());
		Assert.assertEquals(false, co1.isAllowingAnyPort());
		Assert.assertEquals(true, co1.isAllowingSubdomains());
		
		Assert.assertTrue(co1.matches(new CORSOrigin("https://www.google.com")));
		Assert.assertTrue(co1.matches(new CORSOrigin("http://google.com")));
		Assert.assertFalse(co1.matches(new CORSOrigin("www.google.com:9888")));
		
		CORSOrigin co2 = new CORSOrigin("http://localhost:*");
		Assert.assertEquals(co2.getScheme(), "http");
		Assert.assertEquals(co2.getDomain(), "localhost");
		Assert.assertEquals(co2.isAllowingAnyPort(), true);
		Assert.assertEquals(co2.isAllowingSubdomains(), false);
		
		Assert.assertTrue(co2.matches(new CORSOrigin("http://localhost:9888")));
		Assert.assertTrue(co2.matches(new CORSOrigin("http://localhost:9889")));
		Assert.assertFalse(co2.matches(new CORSOrigin("https://localhost:9888")));
		
		CORSOrigin co3 = new CORSOrigin("chrome-extension://sdfoihadfhsuifhsadfhsd");
		Assert.assertEquals("chrome-extension", co3.getScheme());
		Assert.assertEquals("sdfoihadfhsuifhsadfhsd", co3.getDomain());
		Assert.assertEquals( (Integer) 80, co3.getPort());
		Assert.assertEquals(false, co3.isAllowingAnyPort());
		Assert.assertEquals(false, co3.isAllowingSubdomains());
		
		Assert.assertTrue(co3.matches(new CORSOrigin("chrome-extension://sdfoihadfhsuifhsadfhsd")));
		Assert.assertFalse(co3.matches(new CORSOrigin("sdfoihadfhsuifhsadfhsd")));
		
		CORSOrigin co4 = new CORSOrigin("https://amazon.com:987");
		Assert.assertEquals("https", co4.getScheme());
		Assert.assertEquals("amazon.com", co4.getDomain());
		Assert.assertEquals((Integer) 987, co4.getPort());
		Assert.assertEquals(false,co4.isAllowingAnyPort());
		Assert.assertEquals(false,co4.isAllowingSubdomains());
		
		Assert.assertTrue(co4.matches(new CORSOrigin("https://amazon.com:987")));
		Assert.assertFalse(co4.matches(new CORSOrigin("https://www.amazon.com:987")));
	}
}

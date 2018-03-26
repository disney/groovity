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
package com.disney.groovity.test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import groovy.lang.Binding;
import groovy.lang.Script;

import java.io.CharArrayWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;

import org.apache.http.client.utils.DateUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityBuilder;
import com.disney.groovity.GroovityPhase;
import com.disney.groovity.source.HttpGroovitySourceLocator;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class HttpLocatorTest {
	@Rule
	public WireMockRule wireMockRule = new WireMockRule(28187); // No-args constructor defaults to port 8080

	public HttpLocatorTest() {
	}
	
	@Test
	public void testHTTPCompile() throws Exception{
		stubFor(get(urlEqualTo("/"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Last-Modified", "100")
						.withBody("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">\r\n" + 
								"<html>\r\n" + 
								" <head>\r\n" + 
								"  <title>Index of /groovy/nwsDynGroovy</title>\r\n" + 
								" </head>\r\n" + 
								" <body>\r\n" + 
								"<h1>Index of /groovy/nwsDynGroovy</h1>\r\n" + 
								"<ul><li><a href=\"/groovy/\"> Parent Directory</a></li>\r\n" + 
								"<li><a href=\"test.grvt\"> test.grvt</a></li>\r\n" + 
								"</ul>\r\n" + 
								"</body></html>")));
		String lmod = DateUtils.formatDate(new Date());
		
		stubFor(head(urlEqualTo("/test.grvt"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Last-Modified", lmod)));
		stubFor(get(urlEqualTo("/test.grvt"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Last-Modified", lmod)
						.withBody(" out << \"hello\" ")));
		
		Groovity groovity = new GroovityBuilder().setSourcePhases(EnumSet.of(GroovityPhase.STARTUP))
				.setSourceLocations(Arrays.asList(new URI("http://localhost:28187")))
				.build();
		CharArrayWriter writer = new CharArrayWriter();
		Binding binding = new Binding();
		Script script = groovity.load("/test",binding);
		binding.setProperty("out", writer);
		script.run();
		Assert.assertEquals("hello", writer.toString());
	}
}

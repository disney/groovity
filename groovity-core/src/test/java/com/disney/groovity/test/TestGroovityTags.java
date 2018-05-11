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


import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityBuilder;
import com.disney.groovity.model.ModelJsonWriter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import groovy.lang.Binding;
import groovy.lang.Script;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * invoke and validate test scripts for core groovity tags
 * 
 * @author Alex Vigdor
 *
 */
public class TestGroovityTags {
	static Groovity groovity;
	static  ArrayList<LogRecord> logRecords = new ArrayList<>();;
	static Logger testScriptLogger;
	
	@ClassRule
	public static WireMockRule wireMockRule = new WireMockRule(28197);
	
	@BeforeClass
	public static void setup() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, URISyntaxException{
		Handler testHandler = new Handler(){

			@Override
			public void publish(LogRecord record) {
				logRecords.add(record);
			}

			@Override
			public void flush() {
			}

			@Override
			public void close() throws SecurityException {
			}
			
		};
		testHandler.setLevel(Level.FINE);
		testScriptLogger = Logger.getLogger("/logTestScript.grvt");
		testScriptLogger.addHandler(testHandler);
		testScriptLogger.setLevel(Level.FINE);
		groovity = new GroovityBuilder()
				.setSourceLocations(Arrays.asList(new File("src/test/resources/tags").toURI()))
				.build();
	}
	
	
	
	protected String run(String path) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException{
		return run(path,new Binding());
	}
	
	protected String run(String path, Binding binding) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException{
		StringWriter writer = new StringWriter();
		binding.setVariable("out", writer);
		groovity.run(path, binding);
		//System.out.println("Result for "+path+" is "+writer.toString());
		return writer.toString();
	}
	
	@Test
	public void testIfTag() throws Exception{
		Assert.assertEquals(run("/if"),"0,2,4,6,8,10,");
	}
	
	@Test
	public void testIfElseTag() throws Exception{
		Assert.assertEquals("0.1,2.3:4.5,6.7,8.9:10.",run("/ifElse"));
	}
	
	@Test
	public void testEachTag() throws Exception{
		Assert.assertEquals("0,1,2,3,4,5,6,",run("/each"));
	}
	
	@Test
	public void testSetTag() throws Exception{
		Binding binding = new Binding();
		Script gs = groovity.load("/set",binding);
		StringWriter out = new StringWriter();
		binding.setVariable("out", out);
		gs.run();
		String result = out.toString();
		Assert.assertEquals(81, binding.getVariable("foo"));
		Assert.assertEquals("Something 9", binding.getVariable("bar"));
		Assert.assertEquals("Foo 81 bar Something 9 abc 9 xyz", result);
	}
	
	@Test 
	public void testRunTag() throws Exception{
		Assert.assertEquals("Hello How are you? Goodbye", run("/outer"));
	}
	
	@Test
	public void testLoadTag() throws Exception{
		Assert.assertEquals("Say Something", run("/load").trim());
	}
	
	@Test
	public void testQuotes() throws Exception{
		Assert.assertEquals("one,two,THREE,foUR,FIve,", run("/quotes"));
	}
	

	@Test
	public void testUnless() throws Exception{
		String output = run("/unless");
		Assert.assertEquals("1,3,5,7,9,", output);
	}
	
	@Test
	public void testJoin() throws Exception{
		String output = run("/join");
		Assert.assertEquals("Groovity_Groovy_Java", output);
	}
	@Test
	public void testWhile() throws Exception{
		String output = run("/while");
		Assert.assertEquals("1,2,3,1,2,3,4,5,", output);
	}
	@Test
	public void testEachJoin() throws Exception{
		String output = run("/eachJoin");
		Assert.assertEquals("first=a.b.c;last=x.y.z;", output);
	}
	@Test
	public void testFormatDate() throws Exception{
		String output = run("/formatDate");
		Assert.assertEquals("1970-01-01,1969-12-31;1970-04-26,1970-04-26;1970-12-14,1970-12-13;", output);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testCustomTag() throws Exception{
		String output = run("/customTag");
	//	System.out.println("Output is "+output);
		Assert.assertThat(output,new BaseMatcher() {
			String pattern="^\\s{3,}a  b\\s{3,}c   d\\s{3,}a b\\s{1,2}c d\\s{3,}a  b\\s{3,}c   d\\s+$";

			public boolean matches(Object o) {
				return o.toString().matches(pattern);
			}

			public void describeTo(Description desc) {
				desc.appendText(pattern);
			}
		});
	}
	@Test
	public void testFormatNumber() throws Exception{
		String output = run("/formatNumber");
		Assert.assertEquals("30|$30|1,224.37|$120,000.23|3,000%|120000.231|  120,000.231|030|120,000.2|15.0|15|(000030) hello $120,000.23", output);
	}
	
	@Test	
	public void testComment() throws Exception{
		String output = run("/comments");
		//System.out.println("Output is "+output);
		Assert.assertEquals("0,2,4,", output);
	}
	@Test
	public void testRemove() throws Exception{
		String output = run("/remove");
		Assert.assertEquals("30false", output);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testMinify() throws Exception{
		String output = run("/minify");
	//	System.out.println("Output is "+output);
		Assert.assertThat(output,new BaseMatcher() {
			String pattern="^\\s{3,}a  b\\s{3,}c   d\\s{3,}a b\\s{1,2}c d\\s{3,}a  b\\s{3,}c   d\\s+$";

			public boolean matches(Object o) {
				return o.toString().matches(pattern);
			}

			public void describeTo(Description desc) {
				desc.appendText(pattern);
			}
		});
	}		
	
	@Test
	public void testCatch() throws Exception{
		String output = run("/catch");
		Assert.assertEquals("java.lang.ArithmeticException: Division by zero", output);
	}
	@Test
	public void testFindAll() throws Exception{
		String output = run("/findAll");
		Assert.assertEquals("\nStephen King;\nStephen King;", output);
	}

	@Test
	public void testOut() throws Exception{
		String output = run("/out");
		//System.out.println("Output is "+output);
		Assert.assertEquals(";empty;missing;;abc;abc;abc;abc;me &amp; you &lt;&gt;;me &amp; you &lt;&gt;;me &amp; you &lt;&gt;;me & you <>;", output);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testExportJson() throws Exception{
		String output = run("/exportJson");
		Assert.assertThat(output,new BaseMatcher() {
			String pattern="\\{\"title\":\"hello world\",\"description\":\"how are you\"\\};\\{\\s+" + 
					"\"title\" : \"hello world\",\\s+" +
					"\"description\" : \"how are you\"\\s+" +
					"\\};\\}\"uoy era woh\":\"noitpircsed\",\"dlrow olleh\":\"eltit\"\\{;;\\{\"error\":\"not found\"\\};\\{'error','no data'\\};\\}\"dnuof ton\":\"rorre\"\\{;\\}'atad on','rorre'\\{;";

			public boolean matches(Object o) {
				return o.toString().matches(pattern);
			}

			public void describeTo(Description desc) {
				desc.appendText(pattern);
			}
		});
	}
	
	@Test
	public void testTruncate() throws Exception{
		String output = run("/truncate");
		Assert.assertEquals("abc;xyz;The quick brown;kciuq ehT;The &quot;&apos;&amp;&gt;&lt; quick...;^The &quot;&apos;&amp;&gt;&lt;,,;", output);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testDocs() throws Exception{
		Map map = null;
		List<Map> docs = groovity.getLibraryDocs();
		for(Map doc: docs){
			if(doc.get("path").equals("/doc.grvt")){
				map = doc;
				break;
			}
		}
		CharArrayWriter caw = new CharArrayWriter();
		new ModelJsonWriter(caw).visit(map);
		String output = caw.toString();
		//System.out.println("DOCS TEST "+output);
		Assert.assertEquals("{\"name\":\"doc\",\"path\":\"\\/doc.grvt\",\"functions\":[{\"name\":\"getFullName\",\"info\":\"Construct a full name\",\"args\":[{\"name\":\"first\",\"info\":\"first or given name\",\"nullable\":false,\"type\":\"String\"},{\"name\":\"middleName\",\"info\":\"\",\"nullable\":true,\"type\":\"String\"},{\"name\":\"lastName\",\"info\":\"last or family name\",\"nullable\":false,\"type\":\"String\"}],\"returns\":\"String\"},"+
				"{\"name\":\"getSmaller\",\"info\":\"Decide which number is smaller\",\"args\":[{\"name\":\"first\",\"info\":\"\",\"nullable\":false,\"type\":\"Number\"},{\"name\":\"second\",\"info\":\"\",\"nullable\":false,\"type\":\"Number\"}],\"returns\":\"Number\"}]}"
				, output);
	}
	
	@Test 
	public void testHttp() throws Exception{
		stubFor(put(urlEqualTo("/someEndpoint?x=z"))
				.withHeader("foo", matching("bar"))
				.withRequestBody(matching("This is the post body"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Last-Modified", "100")
						.withHeader("Content-Type", "text/plain")
						.withBody("postBodyOK")));
		stubFor(post(urlEqualTo("/mypost"))
				.withRequestBody(matching("a=b&c=d"))
				.withHeader("Content-Type", matching("application/x-www-form-urlencoded; charset=UTF-8"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Last-Modified", "100")
						.withHeader("Content-Type", "text/plain")
						.withBody("formOK")));
		stubFor(post(urlEqualTo("/mypost"))
				.withRequestBody(matching("\\{\"a\":\"b\",\"c\":\"d\"\\}"))
				.withHeader("Content-Type", matching("application/json.*"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Last-Modified", "100")
						.withHeader("Content-Type", "application/json")
						.withBody("{\"status\":\"jsonOK\"}")));
		stubFor(post(urlEqualTo("/mypost"))
				.withRequestBody(matching("(?s).*?Content-Disposition: form-data; name=\"part1\"; filename=\"file1.txt\"\\s+Content-Type: application/octet-stream\\s+Content-Transfer-Encoding: binary\\s+part1 body.*?Content-Disposition: form-data; name=\"part2\"\\s+Content-Type: text/plain; charset=ISO-8859-1\\s+Content-Transfer-Encoding: 8bit\\s+part2 body.*"))
				.withHeader("Content-Type", matching("multipart/form-data; boundary=.*"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Last-Modified", "100")
						.withHeader("Content-Type", "text/plain")
						.withBody("multipartOK")));
		stubFor(put(urlEqualTo("/myput"))
				.withRequestBody(matching("(?s).*<tag>content</tag>\\s+"))
				.withHeader("Content-Type", matching("application/xml.*"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Last-Modified", "100")
						.withHeader("Content-Type", "application/xml")
						.withBody("<doc><status>xmlOK</status><other>whatever</other></doc>")));
		stubFor(put(urlEqualTo("/myput"))
				.withHeader("Content-Type", matching("application/json.*"))
				.withRequestBody(equalToJson("{\"abc\":123}"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Last-Modified", "100")
						.withHeader("Content-Type", "application/xml")
						.withBody("<doc><status>xmlOK</status><other>whatever</other></doc>")));
		wireMockRule.addMockServiceRequestListener(new RequestListener() {
			
			public void requestReceived(Request req, Response res) {
				System.out.println("Request made "+req.getBodyAsString()+" response was "+(res.getStatus()==200?res.getBodyAsString():res.getStatus()));
			}
		});
		String output = run("/http");
		//System.out.println("Output is "+output);
		Assert.assertEquals("formOK;jsonOK;multipartOK;xmlOK;postBodyOK;formOK", output);

	}
	
	@Test
	public void testParse() throws Exception{
		stubFor(get(urlEqualTo("/someXml"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Last-Modified", "100")
						.withHeader("Content-Type", "application/xml")
						.withBody("<doc><title>Hello</title><description>World of XML</description></doc>")));
		stubFor(get(urlEqualTo("/someJson"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Last-Modified", "100")
						.withHeader("Content-Type", "application/json")
						.withBody("{ \"title\":\"Hello\",\"description\":\"World of Json\"}")));
		String output = run("/parse");
		Assert.assertEquals("Hello;World of XML;Hello;World of Json;Greetings;Hard-coded json;Good day;Hard-coded XML;", output);
	}
	
	
	@Test
	public void testUri() throws Exception{
		String output = run("/uri");
		//System.out.println("Output is "+output);
		Assert.assertEquals("http://www.foo.com/monkeyfist?fool%3F%3F=Surely%25%21|http://www.foo.com/monkeyfist?abc=ghi&abc=def|http://www.foo.com/cheesewhirls?jkl=ghi&abc=def", output);
	}
	
	@Test
	public void testClosure() throws Exception{
		String output = run("/closure");
		//System.out.println("Output is "+output);
		Assert.assertEquals("print('y','x');\n" + 
				"print('x','y');\n" + 
				"print('1','2');\n" + 
				"print('x','y');", output.trim());
	}
	
	@Test
	public void testHmac() throws Exception{
		String output = run("/hmac");
		//System.out.println("Output is "+output);
		Assert.assertEquals("xmL8IAOg7Pqn/IdOm7cuL2bQMQQqZBrsWnLF1NgCSUU=|paeIVUqLbasrL6YaYK7CSg==|C662FC2003A0ECFAA7FC874E9BB72E2F66D031042A641AEC5A72C5D4D8024945|120", output);
	}
	
	@Test
	public void testHash() throws Exception{
		String output = run("/hash");
		//System.out.println("Output is "+output);
		Assert.assertEquals("xE8baoEG2JQXObnNLkDaaUme4cyDTxzWAd+NVnu3Otk=|fRpUEnsiJQL1t5tfsIAwYRUqRPkrN+I8ZSe69mXU2po=|IzDRIAHsAHFZpF2tOpOwAi56J5SpA9u3czgydZiaXik=|9thVh8b7c1lapvz0/uNAwQ==|2330D12001EC007159A45DAD3A93B0022E7A2794A903DBB773383275989A5E29|-31", output);
	}
	
	@Test
	public void testCache() throws Exception{
		String output = run("/cache");
		//System.out.println("Output is "+output);
	}
	
	@Test
	public void testTagMethods() throws Exception{
		String output = run("/tagMethods");
		//System.out.println("Output is "+output);
		Assert.assertTrue("Expected Started up at in "+output, output.startsWith("Started up at "));
		Assert.assertTrue("Expected to end with [x:\"I'm hungry\",y:\"\\\"Are you?\\\"\"] "+output, output.endsWith("[x:\"I'm hungry\",y:\"\\\"Are you?\\\"\"]"));
	}

	@Test
	public void testPrint() throws Exception{
		String output = run("/print");
		output = output.replaceAll("\\r",""); //strip CR for windows compatibility
		//System.out.println("Output is "+output);
		Assert.assertEquals("&amp;&apos;&quot;&lt;&gt;&amp;'&quot;&lt;&gt;&'\\\"<>{\"name\":\"John\",\"age\":32}{\n" + 
				"\t\"name\" : \"John\",\n" +
				"\t\"age\" : 32\n" +
				"}{&quot;name&quot;:&quot;John&quot;,&quot;age&quot;:32}{\\\"name\\\":\\\"John\\\",\\\"age\\\":32}Where?\n" + 
				"[[\n" + 
				"}23:\"ega\",\"nhoJ\":\"eman\"{\n" + 
				"]]\n" + 
				"D5 1973 false\n" + 
				"<ul>\n" + 
				"\n" + 
				"<li>1</li>\n" + 
				"<li>2</li>\n" + 
				"<li>4</li>\n" + 
				"</ul>abc 1", output);
	}
	
	@Test
	public void testLog() throws Exception{
		Binding binding = new Binding();
		Script logScript  = groovity.load("/logTestScript", binding);
		logScript.run();
		/*
		System.out.println("--------------------");
		for(LogRecord r: logRecords){
			System.out.println(r.getLevel()+" "+r.getSourceClassName()+" "+r.getSourceMethodName()+" "+r.getMessage());
		}
		System.out.println("--------------------");
		*/
		ArrayList<LogRecord> expectedRecords = new ArrayList<>();
		LogRecord r1 = new LogRecord(Level.INFO, "INNER-STATICALLY");
		r1.setSourceClassName("/logTestScript->Melog");
		r1.setSourceMethodName("init");
		expectedRecords.add(r1);
		LogRecord r2 = new LogRecord(Level.FINE, "STATICALLY");
		r2.setSourceClassName("/logTestScript");
		r2.setSourceMethodName("init");
		expectedRecords.add(r2);
		LogRecord r3 = new LogRecord(Level.INFO, "LOADALLY");
		r3.setSourceClassName("/logTestScript");
		r3.setSourceMethodName("load");
		expectedRecords.add(r3);
		LogRecord r4 = new LogRecord(Level.WARNING, "Testing 123");
		r4.setSourceClassName("/logTestScript");
		r4.setSourceMethodName("run");
		expectedRecords.add(r4);
		LogRecord r5 = new LogRecord(Level.SEVERE, "INNER-FOO");
		r5.setSourceClassName("/logTestScript->Melog");
		r5.setSourceMethodName("foo");
		expectedRecords.add(r5);
		LogRecord r6 = new LogRecord(Level.INFO, "LOADERLY");
		r6.setSourceClassName("/logTestScript");
		r6.setSourceMethodName("run");
		expectedRecords.add(r6);
		LogRecord r7 = new LogRecord(Level.FINE, "Cannot go there");
		r7.setSourceClassName("/logTestScript->Melog");
		r7.setSourceMethodName("{44-49}");
		expectedRecords.add(r7);
		expecting:
		for(LogRecord expected: expectedRecords){
			for(LogRecord logged: logRecords){
				if(logged.getLevel().equals(expected.getLevel()) 
						&& logged.getMessage().equals(expected.getMessage())
						&& logged.getSourceClassName().equals(expected.getSourceClassName())
						&& logged.getSourceMethodName().equals(expected.getSourceMethodName())){
					continue expecting;
				}
			}
			Assert.fail("Found no logged record matching "+expected.getLevel()+" "+expected.getSourceClassName()+" "+expected.getSourceMethodName()+" "+expected.getMessage());
		}
		
	}
	
	@Test
	public void testAsync() throws Exception{
		String output = run("/async");
		Assert.assertEquals("3 DeadlockFreeThread ABCDEFGHIJK java.lang.InterruptedException", output);
	}
	@Test
	public void testAwait() throws Exception{
		String output = run("/await");
		Assert.assertEquals("abcdef java.util.concurrent.TimeoutException", output);
	}
	@Test
	public void testAccept() throws Exception{
		Logger asyncChannelLogger = Logger.getLogger("com.disney.groovity.util.AsyncChannel");
		Level prev = asyncChannelLogger.getLevel();
		asyncChannelLogger.setLevel(Level.OFF);
		String output = run("/accept");
		Assert.assertEquals("<ul><li>\"0\"</li><li>\"1\"</li><li>\"2\"</li><li>\"3\"</li><li>\"4\"</li><li>\"5\"</li><li>\"6\"</li><li>\"7\"</li><li>\"8\"</li><li>\"9\"</li><li>\"10\"</li>" + 
				"</ul>1,2,3,4,5,closed|1,2,3,4,5,6,7,8,9,10,closed|1,2,3,closed|5", output);
		asyncChannelLogger.setLevel(prev);
	}
	@Test
	public void testWriteXml() throws Exception{
		run("/writeXml");
	}
	
	@Test
	public void testWriteTemplates() throws Exception{
		run("/writeTemplates");
	}
	
	@Test
	public void testParseXml() throws Exception{
		run("/parseXml");
	}
}

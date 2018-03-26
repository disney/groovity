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
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.disney.groovity.ArgsException;
import com.disney.groovity.BindingDecorator;
import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityBuilder;
import com.disney.groovity.GroovityPhase;
import com.disney.groovity.compile.GroovityClassLoader;
import com.disney.groovity.conf.Configurator;

import groovy.lang.Binding;
import groovy.lang.Script;

public class TestCoreGroovity {
	static Groovity groovity;
	static StringWriter groovyityOut;
	static ArrayList<LogRecord> logRecords = new ArrayList<>();
	
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
		Logger confLogger = Logger.getLogger("/conf.grvt");
		confLogger.addHandler(testHandler);
		confLogger.setUseParentHandlers(false);
		groovyityOut = new StringWriter();
		groovity = setupGroovity(true,groovyityOut);
		System.out.println(groovyityOut.toString());
	}
	
	private static Groovity setupGroovity(boolean caseSensitive, final Writer out) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, URISyntaxException{
		Map<String,Object> defaultBinding = new HashMap<>();
		defaultBinding.put("hello", "world");
		defaultBinding.put("extensions", "overrideMe");
		Groovity groovity = new GroovityBuilder()
				.setSourceLocations(Arrays.asList(new File("src/test/resources/core").toURI()))
				.setSourcePhases(EnumSet.of(GroovityPhase.STARTUP))
				.setCaseSensitive(caseSensitive)
				.setDefaultBinding(defaultBinding)
				.setBindingDecorator(new BindingDecorator() {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void decorate(Map binding) {
				binding.put("extensions", Arrays.asList("antivirus","firewall"));
				binding.put("whatToSay", "Orange you glad I didn't say banana?");
				if(!binding.containsKey("out")){
					binding.put("out", out);
				}
			}
		}).build();
		return groovity;
	}
	
	@Test public void testVeggies() throws Exception{
		Binding binding = new Binding();
		binding.setVariable("veggie", "sweet potato");
		String result = run("/veggies",binding);
		Assert.assertEquals("Vegetable(ROOT, sweet potato)", result);
		binding.setVariable("veggie", "collard greens");
		result = run("/veggies",binding);
		Assert.assertEquals("Vegetable(LEAFY, collard greens)", result);
	}
	
	@Test public void testEssential() throws Exception{
		Binding binding = new Binding();
		String result = run("/essential",binding).trim();
		String pattern = "\\d+ processors, \\d+ max memory, extensions \\[antivirus, firewall\\]";
		Assert.assertTrue(pattern+" != "+result,Pattern.compile(pattern).matcher(result).matches());
	}
	
	@Test public void testDependent() throws Exception{
		Binding binding = new Binding();
		String result = run("/dependent",binding).trim();
		Assert.assertEquals("[antivirus, firewall]", result);
	}
	
	@Test public void testCaseSensitive() throws Exception{
		Binding binding = new Binding();
		String result = run("/mixedCase",binding).trim();
		Assert.assertEquals("OK", result);
		Exception e = null;
		try{
			result = run("/mixedcase",binding).trim();
		}
		catch(Exception x){
			e=x;
		}
		Assert.assertNotNull("Expected IllegalArgumentException",e);
		Assert.assertEquals("Expected IllegalArgumentException", ClassNotFoundException.class, e.getClass());
	}
	
	@Test public void testCaseInsensitive() throws Exception{
		Groovity cig = setupGroovity(false, new StringWriter());
		Binding binding = new Binding();
		StringWriter writer = new StringWriter();
		binding.setVariable("out", writer);
		cig.run("/mixedcase", binding);
		Assert.assertEquals("OK", writer.toString().trim());
	}
	
	
	
	@Test public void testMissing() throws Exception{
		Exception e = null;
		try{
			Binding binding = new Binding();
			run("/notfound",binding);
		}
		catch(Exception x){
			e=x;
		}
		Assert.assertNotNull("Expected ClassNotFoundException",e);
		Assert.assertEquals("Expected ClassNotFoundException", ClassNotFoundException.class, e.getClass());

	}
	
	@Test public void testImplicitReturn() throws Exception{
		Binding binding = new Binding();
		Object result = groovity.run("/implicit", binding);
		Assert.assertEquals(Date.class, result.getClass());
	}
	
	@Test public void testLoad() throws Exception{
		Binding binding = new Binding();
		Object result = groovity.run("/loader", binding);
		Assert.assertEquals(Date.class, result.getClass());
	}
	
	@Test public void testDecorate() throws Exception{
		Binding binding = new Binding();
		String result = run("/decorate", binding);
		Assert.assertEquals("[[\"antivirus\",\"firewall\"],\"world\",\"for now\"]", result);
	}
	
	@Test public void testTemplates() throws Exception{
		Binding binding = new Binding();
		String result = run("/templates", binding);
		Assert.assertEquals("|1tset|21tset|321tset||test123|test12|test1|", result);
	}
	
	@Test public void testInit() throws Exception{
		String message = "Orange you glad I didn't say banana?";
		Binding binding = new Binding();
		String result = run("/init", binding);
		Assert.assertEquals(message,result);
	}
	
	@Test public void testArgs() throws Exception{
		Binding binding = new Binding();
		Exception e = null;
		try{
			run("/args", binding);
		}
		catch(ArgsException me){
			e = me;
		}
		Assert.assertNotNull(e);
		binding.setProperty("name", "John");
		binding.setProperty("age", "34");
		binding.setProperty("hobby","abc");
		binding.setProperty("premium", "TRUE");
		String result = run("/args", binding);
		Assert.assertEquals("John java.lang.String [abc] [] [Ljava.lang.String; 34 java.lang.Long [1] [I [2, 4] java.util.ArrayList java.lang.Integer  true 10 java.lang.Integer Always Sometimes", result);
		binding = new Binding();
		binding.setProperty("level", new String[]{"5","6"});
		binding.setProperty("name", "Jack");
		binding.setProperty("age", new byte[] {(byte) 21, (byte) 99});
		binding.setProperty("hobby",new String[]{"abc","def"});
		binding.setProperty("friend",new int[]{123,456});
		Map<String,String> partner = new HashMap<>();
		partner.put("name","Jill");
		binding.setProperty("partner",partner);
		binding.setProperty("limit","33");
		binding.setProperty("fixed","broken");
		binding.setProperty("fluid","flexible");
		binding.setProperty("premium", "false");
		binding.setProperty("range", Arrays.asList("7","9"));
		result = run("/args", binding);
		Assert.assertEquals("Jack java.lang.String [abc, def] [123, 456] [Ljava.lang.String; 21 java.lang.Long [5, 6] [I [7, 9] java.util.ArrayList java.lang.Integer {name=Jill} false 33 java.lang.Integer Always flexible", result);
	}
	
	@Test public void testNestedArgs() throws Exception{
		Binding binding = new Binding();
		AssertionError ae = null;
		try{
			run("/argsCaller", binding);
		}
		catch(AssertionError ae1){
			ae = ae1;
		}
		Assert.assertNotNull(ae);
		binding = new Binding();
		binding.setProperty("api_key", "user");
		ArgsException me = null;
		try{
			run("/argsCaller", binding);
		}
		catch(ArgsException me1){
			me=me1;
		}
		Assert.assertNotNull(me);
		binding = new Binding();
		binding.setProperty("api_key", "admin");
		binding.setProperty("foo", "8172361");
		String result = run("/argsCaller", binding);
		Assert.assertEquals(" 8172361 java.lang.Long says admin ",result);
	}
	
	@Test public void testSyntax() throws Exception{
		String message = " ${a} = $ \\ ~ \\\\ > ~ > ~ $ \\$\\ \\> \\~ ${~> ${ ~> ";
		Binding binding = new Binding();
		String result = run("/syntax", binding);
		Assert.assertEquals(message,result);
	}
	
	@Test public void testImmutable() throws Exception{
		String message = "Widget(widget1, one widget)";
		Binding binding = new Binding();
		String result = run("/immutable", binding);
		Assert.assertEquals(message,result);
	}
	
	@Test public void testModel() throws Exception{
		Binding binding = new Binding();
		String result = run("/model",binding).trim();
		Assert.assertEquals("{\"data\":{\"active\":true,\"flags\":\"xyz\",\"greeting\":\"Hello Joe\",\"inactive\":false,\"sig\":\"YWJj\",\"time\":1000000,\"timeSeconds\":1000},\"part1\":{\"subpartA\":\"subPartA\",\"subPartB\":\"subPartB\",\"subPartC\":\"subPartC\"},\"part2\":\"wheat\",\"part3\":null}{\"count\":2,\"images\":[{\"reverse\":\"gpj.oof\",\"name\":\"foo.jpg\"},{\"reverse\":\"gpj.rab\",\"name\":\"bar.jpg\"}],\"main\":{\"reverse\":\"gpj.zyx\",\"name\":\"xyz.jpg\"}}", result);
	}
	
	@Test public void testPutDeep() throws Exception{
		Binding binding = new Binding();
		run("/putDeep",binding);
	}
	
	@Test public void testConf() throws Exception{
		Binding binding = new Binding();
		CharArrayWriter writer = new CharArrayWriter();
		binding.setVariable("out", writer);
		Script confScript = groovity.load("/conf", binding);
		GroovityClassLoader gcl = (GroovityClassLoader) confScript.getClass().getClassLoader();
		groovity.run("/conf", binding);
		String result = writer.toString();
		Assert.assertEquals("|false||0||test|||", result);
		writer.reset();
		gcl.configure(new Configurator() {
			
			@Override
			public void init() {}
			
			@Override
			public void destroy() {}
			
			@Override
			public void configure(String sourcePath, Set<String> propertyNames, BiConsumer<String, String> propertySetter) {
				propertySetter.accept("testDefaultBoolean", "true");
				propertySetter.accept("testBooleanType", "foo");
				propertySetter.accept("testDefaultInteger", "99");
				propertySetter.accept("testIntegerType", "111");
				propertySetter.accept("testDefaultString", "bar");
				propertySetter.accept("testStringType", "zzz");
				propertySetter.accept("testNull", "qq");
			}
		});
		groovity.run("/conf", binding);
		result = writer.toString();
		Assert.assertEquals("|true|false|99|111|bar|zzz|qq|", result);
		writer.reset();
		logRecords.clear();
		gcl.configure(new Configurator() {
			
			@Override
			public void init() {}
			
			@Override
			public void destroy() {}
			
			@Override
			public void configure(String sourcePath, Set<String> propertyNames, BiConsumer<String, String> propertySetter) {
				propertySetter.accept("testDefaultInteger", "badOnPurpose");
				propertySetter.accept("testIntegerType", "badOnPurpose");
			}
		});
		groovity.run("/conf", binding);
		result = writer.toString();
		Assert.assertEquals("|false||0||test|||", result);
		Assert.assertEquals(3, logRecords.size());
		Assert.assertEquals(Level.WARNING, logRecords.get(0).getLevel());
		Assert.assertEquals(Level.SEVERE, logRecords.get(1).getLevel());
		Assert.assertEquals(Level.SEVERE, logRecords.get(2).getLevel());
	}
	
	protected String run(String path, Binding binding) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException{
		StringWriter writer = new StringWriter();
		binding.setVariable("out", writer);
		groovity.run(path, binding);
		//System.out.println("Result for "+path+" is "+writer.toString());
		return writer.toString();
	}
	
	
}

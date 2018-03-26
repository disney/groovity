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

import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.EnumSet;

import org.junit.Assert;
import org.junit.Test;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityBuilder;
import com.disney.groovity.GroovityPhase;

import groovy.lang.Binding;

public class TestCyclicGroovity {
	static Groovity groovity;
	static StringWriter groovyityOut;
	
	@Test
	public void testCyclicChecking() throws Exception{
		Throwable e = null;
		try{
			groovity = new GroovityBuilder()
					.setSourceLocations(Arrays.asList(new File("src/test/resources/staticbad").toURI()))
					.setSourcePhases(EnumSet.of(GroovityPhase.STARTUP))
					.build();
		}
		catch(Exception x){
			e=x;
		}
		while(e!=null && !(e instanceof IllegalArgumentException)){
			e=e.getCause();
		}
		Assert.assertNotNull("Expected IllegalArgumentException",e);
		Assert.assertEquals("Expected IllegalArgumentException", IllegalArgumentException.class, e.getClass());
	}
	
	@Test
	public void testCyclicLoadChecking() throws Exception{
		groovity = new GroovityBuilder()
				.setSourceLocations(Arrays.asList(new File("src/test/resources/cyclic").toURI()))
				.setSourcePhases(EnumSet.of(GroovityPhase.STARTUP))
				.build();
		Binding binding = new Binding();
		StringWriter writer = new StringWriter();
		binding.setVariable("out", writer);
		Throwable e = null;
		try{
			groovity.run("/top", binding);
		}
		catch(Exception x){
			e=x;
		}
		while(e!=null && !(e instanceof InstantiationException)){
			e=e.getCause();
		}
		Assert.assertNotNull("Expected InstantiationException",e);
		Assert.assertEquals("Expected InstantiationException", InstantiationException.class, e.getClass());
	}
	
	@Test
	public void testNormalCode() throws Exception{
		groovity = new GroovityBuilder()
				.setSourceLocations(Arrays.asList(new File("src/test/resources/static").toURI()))
				.setSourcePhases(EnumSet.of(GroovityPhase.STARTUP))
				.build();
		Exception e = null;
		Binding binding = new Binding();
		StringWriter writer = new StringWriter();
		binding.setVariable("out", writer);
		binding.setVariable("request", "someRuntimeRequest");
		groovity.run("/static", binding);
		System.out.println("Result for /static is "+writer.toString());
		Assert.assertEquals("static|runtime", writer.toString());
	}
	
	@Test
	public void testStaticBinding() throws Exception{
		groovity = new GroovityBuilder()
				.setSourceLocations(Arrays.asList(new File("src/test/resources/static").toURI()))
				.setSourcePhases(EnumSet.of(GroovityPhase.STARTUP))
				.build();
		String result = groovity.run("/initBinding", new Binding()).toString();
		Assert.assertEquals("http://disney.com/characters?name=Minnie", result);
	}
	
	
	
}

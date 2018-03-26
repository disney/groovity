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
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.EnumSet;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityBuilder;
import com.disney.groovity.GroovityPhase;

import groovy.lang.Binding;
/**
 * validate behavior of traits
 * 
 * @author Alex Vigdor
 *
 */
public class TestTraits {
	static Groovity groovity;
	
	@BeforeClass
	public static void setupGroovity() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, URISyntaxException{
		groovity = new GroovityBuilder()
				.setSourceLocations(Arrays.asList(new File("src/test/resources/traits").toURI()))
				.setSourcePhases(EnumSet.of(GroovityPhase.STARTUP))
				.build();
	}
	
	@Test
	public void testOstritch() throws Exception {
		String message = "{\"wingSpan\":72.0,\"spineLength\":145.0,\"numFeathers\":12137,\"numLegs\":2}";
		Binding binding = new Binding();
		String result = run("/testOstritch",binding).replaceAll("\\s+", "");
		Assert.assertEquals(message,result);
	}
	
	@Test
	public void testSwallow() throws Exception {
		String message = "{\"maxAltitude\":3500,\"wingSpan\":2.0,\"spineLength\":2.5,\"numFeathers\":2965,\"numLegs\":2}";
		Binding binding = new Binding();
		String result = run("/testSwallow",binding).replaceAll("\\s+", "");
		Assert.assertEquals(message,result);
	}
	
	protected String run(String path, Binding binding) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException{
		StringWriter writer = new StringWriter();
		binding.setVariable("out", writer);
		groovity.run(path, binding);
		//System.out.println("Result for "+path+" is "+writer.toString());
		return writer.toString();
	}
}

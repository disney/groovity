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
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityBuilder;
import com.disney.groovity.GroovityPhase;

import groovy.lang.Binding;
import groovy.lang.Script;
/**
 * Test to validate thundering herd prevention in cache tag.
 * 
 * @author Alex Vigdor
 *
 */
public class TestExtGroovity {
	static Groovity groovity;
	static StringWriter groovyityOut;
	
	@BeforeClass
	public static void setup() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, URISyntaxException{
		groovyityOut = new StringWriter();
		groovity = setupGroovity(true,groovyityOut);
		System.out.println(groovyityOut.toString());
	}
	
	@AfterClass
	public static void teardown() {
		groovity.destroy();
	}
	
	private static Groovity setupGroovity(boolean caseSensitive, final Writer out) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, URISyntaxException{
		Groovity groovity = new GroovityBuilder()
				.setSourceLocations(Arrays.asList(new File("src/test/resources/ext").toURI()))
				.setSourcePhases(EnumSet.of(GroovityPhase.STARTUP))
				.setCaseSensitive(caseSensitive)
				.build();
		return groovity;
	}
	@Test
	public void testThunderingHerd() throws InterruptedException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		Binding binding = new Binding();
		final Script herdScript = groovity.load("/herd", binding);
		CountDownLatch latch = new CountDownLatch(2);
		Runnable r = new Runnable(){

			@Override
			public void run() {
				herdScript.invokeMethod("getCachedThing", new Object[0]);
				latch.countDown();
			}
			
		};
		Thread t1 = new Thread(r);
		Thread t2 = new Thread(r);
		t1.start();
		t2.start();
		latch.await(1000, TimeUnit.MILLISECONDS);
		AtomicInteger executionCount = (AtomicInteger) herdScript.getProperty("cacheLoadCount");
		Assert.assertEquals(1, executionCount.get());
	}
}

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.reflection.ReflectionCache;
import org.junit.Assert;
import org.junit.Test;

import com.disney.groovity.model.Model;
import com.disney.groovity.model.ModelCollector;
import com.disney.groovity.model.ModelConsumer;
import com.disney.groovity.model.ModelFilter;
import com.disney.groovity.model.ModelJsonWriter;
import com.disney.groovity.model.ModelWalker;

import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import groovy.lang.GroovySystem;
import groovy.lang.MetaBeanProperty;
import groovy.lang.MetaClassImpl;
import groovy.lang.MetaMethod;
/**
 * Test model filter methods
 * 
 * @author Alex Vigdor
 *
 */
public class TestModel {

	@Test public void testNaN() throws Exception{
		List<Number> model = Arrays.asList(1.0,2.0f,3.0,Double.NaN,Float.NaN,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);
		CharArrayWriter caw = new CharArrayWriter();
		ModelJsonWriter jw = new ModelJsonWriter(caw);
		jw.visit(model);
		String s = caw.toString();
		@SuppressWarnings("unchecked")
		List<Number> r = (List<Number>) new JsonSlurper().parseText(s);
		Assert.assertEquals(2, r.get(1).intValue());
		Assert.assertEquals(3, r.get(2).intValue());
		for(int i=3;i<9;i++) {
			Assert.assertEquals(null, r.get(i));
		}
	}

	@Test public void testWalker() throws Exception{
		ModelWalker walker = new ModelWalker() {
		};
		walker.visit(new File("target"));
		walker.visit(new URL("http://whatever.not/123/456?abc=xyz"));
		walker.visit(new Date());
	}

	@SuppressWarnings("rawtypes")
	@Test public void testMeta() throws Exception {
		ModelCheck mc = new ModelCheck();
		mc.setFoo("bar");
		Map mc1 = (Map) Model.mapObject(mc);
		Assert.assertEquals(1, mc1.size());
		Assert.assertEquals("bar", mc1.get("foo"));
		AtomicReference<String> abcref = new AtomicReference<String>(null);
		MetaClassImpl meta = new MetaClassImpl(ModelCheck.class);
		meta.initialize();
		MetaBeanProperty abc = new MetaBeanProperty("abc",String.class,
			new MetaMethod() {
				
				@Override
				public Object invoke(Object arg0, Object[] arg1) {
					 return abcref.get();
				}
				
				@Override
				public Class getReturnType() {
					return String.class;
				}
				
				@Override
				public String getName() {
					return "getAbc";
				}
				
				@Override
				public int getModifiers() {
					return Modifier.PUBLIC;
				}
				
				@Override
				public CachedClass getDeclaringClass() {
					return ReflectionCache.getCachedClass(ModelCheck.class);
				}
			},
			new MetaMethod() {
				
				@Override
				public Object invoke(Object arg0, Object[] arg1) {
					 abcref.set((String)arg1[0]);
					 return null;
				}
				
				@Override
				public Class getReturnType() {
					return null;
				}
				
				@Override
				public String getName() {
					return "setAbc";
				}
				
				@Override
				public int getModifiers() {
					return Modifier.PUBLIC;
				}
				
				@Override
				public CachedClass getDeclaringClass() {
					return ReflectionCache.getCachedClass(ModelCheck.class);
				}
			});
		meta.addMetaBeanProperty(abc);
		GroovySystem.getMetaClassRegistry().setMetaClass(ModelCheck.class,meta);
		Map mc2 = (Map) Model.mapObject(mc);
		Assert.assertEquals(2, mc2.size());
		Assert.assertNull(mc2.get("abc"));
		Model.put(mc, "abc", "xyz");
		Map mc3 = (Map) Model.mapObject(mc);
		Assert.assertEquals("xyz", mc3.get("abc"));
		MetaClassImpl meta2 = new MetaClassImpl(ModelCheck.class);
		meta2.initialize();
		GroovySystem.getMetaClassRegistry().setMetaClass(ModelCheck.class, meta2);
		Map mc4 = (Map) Model.mapObject(mc);
		Assert.assertEquals(1, mc4.size());
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test public void testTypes() throws Exception {
		HashMap main = new HashMap<>();
		main.put("longChange", 100l);
		main.put("intChange", (short)100);
		
		HashMap second = new HashMap();
		second.put("double", 3.0);
		main.put("second",second);
		
		Map third = new LinkedHashMap<>();
		third.put("x", "y");
		third.put("a", "b");
		third.put("longChange", 20l);
		second.put("third",third);
		
		HashMap features = new HashMap();
		features.put("clowns", 5);
		Map menu = new ConcurrentHashMap();
		menu.put("appetizer", "pretzels");
		menu.put("mainCourse", "pizza");
		features.put("menu", menu);
		
		Map place = new HashMap();
		place.put("name", "State Fairground");
		place.put("latitude", 1234);
		place.put("longitude", 5678);
		
		RealEvent event = new RealEvent();
		event.time = new Date(1000000000);
		event.name = "State Fair";
		event.put("place",place);
		event.description = "State Fun";
		event.features = features;
		
		second.put("event", event);
		
		ModelCollector mc = new ModelCollector();
		mc.setFilters(new ModelFilter[] {
				ModelFilter.transform(Short.class, o->{ return o * 13; }),
				ModelFilter.transform(Long.class, o->{ return o * 17; }),
				ModelFilter.transform(Number.class, o->{ return o.doubleValue() * 2; }),
				ModelFilter.exclude(LinkedHashMap.class,"longChange"),
				ModelFilter.add(LinkedHashMap.class, "ordered", o->{ return o.size(); }),
				ModelFilter.collapse(RealEvent.class,"features"),
				ModelFilter.copy(RealEvent.class, "name","allcaps", name -> { return name.toString().toUpperCase(); }),
				ModelFilter.include(RealPlace.class,"name", "shortName"),
				ModelFilter.promote(ConcurrentHashMap.class,"mainCourse"),
				ModelFilter.rename(RealEvent.class,"description","info"),
				ModelFilter.invoke(Date.class, "getTime", ""),
				ModelFilter.invoke(RealPlace.class, "getName", "shortName", 1, 11),
				ModelFilter.transform(RealPlace.class, "name", o->{ return o.toString().concat("..."); })
		});
		mc.visit(main);
		
		Map expected = new HashMap<>();
		expected.put("longChange", 3400.0);
		expected.put("intChange", 2600.0);
		HashMap expectedSecond = new HashMap();
		expectedSecond.put("double", 6.0);
		expected.put("second",expectedSecond);
		Map expectedThird = new HashMap();
		expectedThird.put("x", "y");
		expectedThird.put("a", "b");
		expectedThird.put("ordered", 6.0);
		expectedSecond.put("third", expectedThird);
		Map expectedEvent = new HashMap<>();
		expectedEvent.put("allcaps", "STATE FAIR");
		expectedEvent.put("name", "State Fair");
		expectedEvent.put("info","State Fun");
		expectedEvent.put("time", 1000000000l);
		expectedEvent.put("clowns", 10.0);
		expectedEvent.put("menu", "pizza");
		Map expectedPlace = new HashMap<>();
		expectedPlace.put("name", "State Fairground...");
		expectedPlace.put("shortName", "tate Fairg");
		expectedEvent.put("place", expectedPlace);
		expectedSecond.put("event",expectedEvent);
		Assert.assertEquals(expected, mc.getCollected());
	}

	@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
	@Test public void testModel() throws Exception {
		HashMap place = new HashMap<>();
		place.put("name", "State Fairground");
		place.put("latitude", 1234);
		place.put("longitude", 5678);
		
		HashMap features = new HashMap();
		features.put("clowns", 5);
		HashMap menu = new HashMap();
		menu.put("appetizer", "pretzels");
		menu.put("mainCourse", "pizza");
		features.put("menu", menu);
		
		RealEvent event = new RealEvent();
		event.name = "State Fair";
		event.put("place",place);
		event.description = "State Fun";
		event.features = features;
		Exception e= null;
		try {
			event.put("time", "last week");
		}
		catch(IllegalArgumentException iae) {
			e = iae;
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos =new ObjectOutputStream(baos);
		oos.writeObject(event);
		oos.close();
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
		Object o = ois.readObject();
		Assert.assertEquals(((Model)o).map(), event.map());
		Assert.assertNotNull("Expected IllegalArgumentException, none was thrown",e);
		event.put("time", 1000000000000l);
		ModelCollector collector = new ModelCollector();
		collector.setFilters(new ModelFilter[] {
				ModelFilter.invoke("","reverseName", "reversedName", 2, 8),
				ModelFilter.exclude("description"),
				ModelFilter.copy("name","allcaps", name -> { return name.toString().toUpperCase(); }),
				ModelFilter.promote("place.name"),
				ModelFilter.invoke("place","reverse", ""),
				ModelFilter.collapse("features.menu"),
				ModelFilter.include("*","features.mainCourse","features.clowns","features.bands"),
				ModelFilter.transform("time", date -> { return ((Date)date).getTime(); }),
				ModelFilter.rename("time","date"),
				ModelFilter.add("features.bands", ev -> { return Arrays.asList("Foo","Bar"); }),
				ModelFilter.custom(new Closure(null){
					@SuppressWarnings("unused")
					public void doCall(String k, Object v, ModelConsumer c){
						if(k.equals("clowns")){
							c.call(k,v.toString());
						}
						else{
							c.call(k,v);
						}
					}
				})
		});
		
		HashMap expected = new HashMap<>();
		expected.put("name", "State Fair");
		expected.put("reversedName", "aF eta");
		expected.put("allcaps", "STATE FAIR");
		expected.put("date", 1000000000000l);
		expected.put("place", "dnuorgriaF etatS");
		HashMap expectedFeatures = new HashMap();
		expectedFeatures.put("mainCourse","pizza");
		expectedFeatures.put("clowns","5");
		expectedFeatures.put("bands", Arrays.asList("Foo","Bar"));
		expected.put("features", expectedFeatures);
		collector.visit(event);
		Assert.assertEquals(expected, collector.getCollected());
		
		HashMap firstLevel = new HashMap();
		HashMap secondLevelA = new HashMap();
		HashMap secondLevelB = new HashMap();
		secondLevelA.put("name", "foo");
		secondLevelB.put("name", "bar");
		firstLevel.put("a", secondLevelA);
		firstLevel.put("b", secondLevelB);
		
		ModelCollector acc = new ModelCollector();
		acc.setFilters(new ModelFilter[] {
				ModelFilter.include("a.name")
		});
		acc.visit(firstLevel);
		HashMap exacc = new HashMap<>();
		exacc.put("a", secondLevelA);
		Assert.assertEquals(exacc, acc.getCollected());
		
		ModelCollector acc2 = new ModelCollector();
		acc2.setFilters(new ModelFilter[] {
				ModelFilter.exclude("a.name")
		});
		acc2.visit(firstLevel);
		HashMap exacc2 = new HashMap<>();
		exacc2.put("b", secondLevelB);
		exacc2.put("a", new HashMap<>());
		Assert.assertEquals(exacc2, acc2.getCollected());
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testCircularReference() throws Exception {
		HashMap outerMap = new HashMap();
		HashMap innerMap = new HashMap();
		outerMap.put("type", "outer");
		innerMap.put("type", "inner");
		outerMap.put("inner",innerMap);
		innerMap.put("outer", outerMap);
		String json = ModelJsonWriter.toJsonString(outerMap, "");
		Assert.assertEquals("{\"type\":\"outer\",\"inner\":{\"outer\":{},\"type\":\"inner\"}}", json);
		Map outerCopy = (Map) Model.copy(outerMap);
		Map innerCopy = (Map) outerCopy.get("inner");
		Assert.assertEquals("outer", outerCopy.get("type"));
		Assert.assertEquals("inner", innerCopy.get("type"));
		Map outerLoop = (Map) innerCopy.get("outer");
		Assert.assertEquals("outer", outerLoop.get("type"));
	}
	
	private static class RealPlace implements Model{
		String name;
		long latitude;
		long longitude;

		public RealPlace(){
		}

		@Override
		public void each(ModelConsumer consumer) {
			consumer.call("name", name);
			consumer.call("latitude", latitude);
			consumer.call("longitude", longitude);
		}
		
		public boolean put(String key, Object value) {
			if(key.equals("name")) {
				this.name= value!=null ? value.toString(): null;
				return true;
			}
			if(key.equals("latitude")) {
				this.latitude= value!=null ? Long.valueOf(value.toString()) : null;
				return true;
			}
			if(key.equals("longitude")) {
				this.longitude= value!=null ? Long.valueOf(value.toString()) : null;
				return true;
			}
			return false;
		}
		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
		@SuppressWarnings("unused")
		public String getName(int off, int end) {
			return name.substring(off, end);
		}
	
	}
	
	private static class RealEvent implements Model{
		String name;
		RealPlace place;
		String description;
		Date time;
		@SuppressWarnings("rawtypes")
		Map features;

		public RealEvent(){
		}

		@Override
		public void each(ModelConsumer consumer) {
			consumer.call("name", name);
			consumer.call("place", place);
			consumer.call("description", description);
			consumer.call("time", time);
			consumer.call("features", features);
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public boolean put(String key, Object value) {
			if(key.equals("name")) {
				this.name= value!=null ? value.toString(): null;
				return true;
			}
			if(key.equals("place")) {
				if(value == null) {
					this.place=null;
					return true;
				}
				if(value instanceof RealPlace) {
					place = (RealPlace) value;
				}
				if(value instanceof Map) {
					place = new RealPlace();
					place.putAll((Map<String, Object>)value);
				}
				return true;
			}
			if(key.equals("description")) {
				this.description= value!=null ? value.toString(): null;
				return true;
			}
			if(key.equals("time")) {
				if(value == null) {
					this.time=null;
					return true;
				}
				if(value instanceof Date) {
					this.time = (Date)value;
					return true;
				}
				if(value instanceof Long) {
					this.time = new Date((Long)value);
					return true;
				}
				throw new IllegalArgumentException("Unknown date format "+value);
			}
			if(key.equals("features")) {
				this.features = (Map) value;
				return true;
			}
			return false;
		}
		@SuppressWarnings("unused")
		public String reverseName(int startIndex, int endIndex) {
			return new StringBuilder(name).reverse().toString().substring(startIndex, endIndex);
		}
	}
	
	private static class ModelCheck{
		private String foo;

		@SuppressWarnings("unused")
		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
	}
}

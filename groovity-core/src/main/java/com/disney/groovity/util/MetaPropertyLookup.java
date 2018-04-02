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
package com.disney.groovity.util;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.annotation.XmlType;

import org.codehaus.groovy.reflection.CachedField;
import org.codehaus.groovy.reflection.CachedMethod;
import org.codehaus.groovy.runtime.metaclass.MethodMetaProperty;

import com.disney.groovity.model.ModelOrder;
import com.disney.groovity.model.ModelSkip;

import groovy.lang.GroovySystem;
import groovy.lang.MetaBeanProperty;
import groovy.lang.MetaClass;
import groovy.lang.MetaClassRegistryChangeEvent;
import groovy.lang.MetaClassRegistryChangeEventListener;
import groovy.lang.MetaMethod;
import groovy.lang.MetaProperty;

/**
 * Provide a cached lookup to ordered properties according to groovy metaclasses combined with @ModelOrder annotation
 * @author Alex Vigdor
 *
 */
public class MetaPropertyLookup {
	static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, MetaProperty>> singlePropertyCache = new ConcurrentHashMap<>();
	static final ConcurrentHashMap<Class<?>, MetaProperty[]> orderedPropertiesCache = new ConcurrentHashMap<>();
	
	static {
		GroovySystem.getMetaClassRegistry().addNonRemovableMetaClassRegistryChangeEventListener(new MetaClassRegistryChangeEventListener() {
			@Override
			public void updateConstantMetaClass(MetaClassRegistryChangeEvent event) {
				singlePropertyCache.remove(event.getClassToUpdate());
				orderedPropertiesCache.remove(event.getClassToUpdate());
			}
		});
	}
	
	public static final MetaProperty getSettableMetaProperty(Object o, String name) {
		final Class<?> c = o.getClass();
		ConcurrentHashMap<String, MetaProperty> properties = singlePropertyCache.get(c);
		if(properties == null) {
			MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(c);
			List<MetaProperty> mps = mc.getProperties();
			properties = new ConcurrentHashMap<>();
			for(MetaProperty mp: mps) {
				if(mp instanceof MetaBeanProperty) {
					MetaBeanProperty mbp = ((MetaBeanProperty)mp);
					if(mbp.getSetter()==null) {
						CachedField cf = mbp.getField();
						if(cf==null || cf.isFinal() || cf.isStatic()) {
							continue;
						}
					}
				}
				else if(mp instanceof MethodMetaProperty) {
					continue;
				}
				properties.put(mp.getName(), mp);
			}
			singlePropertyCache.putIfAbsent(c, properties);
		}
		return properties.get(name);
	}
	
	public static final <T extends Annotation> T getAnnotation(MetaProperty mp, Class<T> annotationClass) {
		if(mp instanceof MetaBeanProperty) {
			MetaBeanProperty mbp = (MetaBeanProperty) mp;
			MetaMethod mm = mbp.getGetter();
			if(mm instanceof CachedMethod) {
				CachedMethod cm = (CachedMethod) mm;
				T anno = cm.getCachedMethod().getAnnotation(annotationClass);
				if(anno!=null) {
					return anno;
				}
			}
			CachedField cf = mbp.getField();
			if(cf != null) {
				return cf.field.getAnnotation(annotationClass);
			}
		}
		return null;
	}
	
	public static final MetaProperty[] getOrderedGettableProperties(Object o){
		final Class<?> c = o.getClass();
		MetaProperty[] properties = orderedPropertiesCache.get(c);
		if(properties == null) {
			MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(c);
			List<MetaProperty> mps = mc.getProperties();
			TreeMap<String, MetaProperty> sortedProps;
			String[] declaredOrder = null;
			ModelOrder order = c.getAnnotation(ModelOrder.class);
			if(order!=null) {
				declaredOrder = order.value();
			}
			else {
				XmlType xmlType = c.getAnnotation(XmlType.class);
				if(xmlType!=null) {
					declaredOrder = xmlType.propOrder();
				}
			}
			if(declaredOrder!=null && declaredOrder.length>0) {
				final String[] declared = declaredOrder;
				final int dl = declared.length;
				Comparator<String> comp = new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						int p1 = dl;
						int p2 = dl;
						for(int i=0;i<dl;i++) {
							String d = declared[i];
							if(o1.equals(d)) {
								p1 = i;
							}
							if(o2.equals(d)) {
								p2 = i;
							}
							if(p1<dl && p2<dl) {
								break;
							}
						}
						if(p1==p2) {
							return o1.compareTo(o2);
						}
						return p1-p2;
					}
				};
				sortedProps = new TreeMap<>(comp);
			}
			else {
				sortedProps = new TreeMap<>();
			}
			List<String> skipProperties = new ArrayList<>();
			skipProperties.add("class");
			skipProperties.add("binding");
			if(Map.class.isAssignableFrom(c) || Collection.class.isAssignableFrom(c)) {
				skipProperties.add("empty");
				if(NavigableMap.class.isAssignableFrom(c)) {
					skipProperties.add("firstEntry");
					skipProperties.add("lastEntry");
				}
			}
			ModelSkip classSkip = c.getAnnotation(ModelSkip.class);
			if(classSkip!=null && classSkip.value()!=null) {
				for(String prop: classSkip.value()) {
					skipProperties.add(prop);
				}
			}
			for(MetaProperty mp: mps) {
				if(!skipProperties.contains(mp.getName()) && ! Closeable.class.isAssignableFrom(mp.getType())) {
					//now check for skip annotations
					if(mp instanceof MetaBeanProperty) {
						MetaBeanProperty mbp = ((MetaBeanProperty)mp);
						if(mbp.getField()!=null) {
							ModelSkip skipAnn = mbp.getField().field.getAnnotation(ModelSkip.class);
							if(skipAnn!=null) {
								continue;
							}
							if(mbp.getField().isStatic()){
								continue;
							}
						}
						MetaMethod getter = mbp.getGetter();
						if(getter instanceof CachedMethod) {
							CachedMethod cm = (CachedMethod)getter;
							ModelSkip skipAnn = cm.getCachedMethod().getAnnotation(ModelSkip.class);
							if(skipAnn!=null) {
								continue;
							}
							if(cm.isStatic()){
								continue;
							}
							if(!cm.getCachedMethod().getDeclaringClass().equals(c)) {
								//we may have an overridden method here
								try {
									Method override = c.getDeclaredMethod(cm.getCachedMethod().getName(), cm.getCachedMethod().getParameterTypes());
									if(override.getAnnotation(ModelSkip.class) !=null) {
										continue;
									}
								} catch (NoSuchMethodException | SecurityException e) {
								}
							}
						}
						if(mbp.getGetter()==null && mbp.getField()==null) {
							continue;
						}
					}
					sortedProps.put(mp.getName(), mp);
				}
			}
			properties = sortedProps.values().toArray(new MetaProperty[0]);
			orderedPropertiesCache.putIfAbsent(c, properties);
		}		
		return properties;
	}
}

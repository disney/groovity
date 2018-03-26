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
package com.disney.groovity.model;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import org.codehaus.groovy.reflection.CachedField;

import com.disney.groovity.GroovityObjectConverter;
import com.disney.groovity.compile.SkipStatistics;
import com.disney.groovity.util.MetaPropertyLookup;

import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import groovy.lang.MetaBeanProperty;
import groovy.lang.MetaProperty;
import groovy.lang.Writable;

/**
 * A model is a type of object that can be expressed in map/list form or serialized to JSON or XML
 * 
 * @author Alex Vigdor
 *
 */
public interface Model extends Externalizable{
	
	/**
	 * Create a deep copy of this model; this default implementation expects that 
	 * put and each are implemented in a symmetrical fashion
	 * 
	 * @return
	 */
	public default Model copy(){
		return (Model) copy(this);
	}
	/**
	 * Create a deep copy of an object
	 * @param o
	 * @return
	 */
	public static Object copy(final Object o) {
		return new ModelCopier().copy(o);
	}
	
	/**
	 * Call a consumer once per field in the data model with two arguments, name and value
	 * 
	 * @param consumer
	 */
	@SkipStatistics
	public default void each(final ModelConsumer consumer) {
		each(this, consumer);
	}
	/**
	 * Iterate through the properties of an object and pass them to a ModelConsumer as name + value.
	 * Relies on Groovy MetaProperties
	 * 
	 * @param o
	 * @param consumer
	 */
	@SuppressWarnings("unchecked")
	public static void each(final Object o, final ModelConsumer consumer) {
		if(o==null) {
			return;
		}
		if(o instanceof String || o instanceof Number) {
			consumer.call("",o);
			return;
		}
		final MetaProperty[] mps = MetaPropertyLookup.getOrderedGettableProperties(o);
		for(int i=0; i<mps.length; i++) {
			MetaProperty mp = mps[i];
			consumer.call(mp.getName(), mp.getProperty(o));
		}
		if(o instanceof Collection) {
			int num = 1;
			for(Object item: (Collection<Object>) o) {
				consumer.call("_item".concat(String.valueOf(num)), item);
			}
		}
		else if(o instanceof Map) {
			((Map<String,Object>)o).forEach(consumer);
		}
	}
	
	/**
	 * Allow natural use of Models in Groovy by overriding each() to iterate through properties
	 * 
	 * @param c
	 */
	public default void each(@SuppressWarnings("rawtypes") Closure c) {
		each((k, v)->{ c.call(k,v); });
	}
	
	/**
	 * Update a field value by name; default implementation sets a property if found in the meta class
	 * 
	 * @param name
	 * @param value
	 * @return true if this model consumed the property value, false if not
	 */
	@SkipStatistics
	public default boolean put(String name, Object value) {
		return put(this, name, value);
	}
	/**
	 * Update the property of an object by name; relies on Groovy MetaProperties to located fields,
	 * and GroovyObjectConverter to perform type conversions.
	 * 	
	 * @param o
	 * @param name
	 * @param value
	 * @return true if the property value applied to this object, false if not
	 */
	@SuppressWarnings("unchecked")
	public static boolean put(final Object o, final String name, Object value) {
		if(o==null) {
			return false;
		}
		if(o instanceof Collection) {
			if(name.startsWith("_item")) {
				((Collection<Object>)o).add(value);
				return true;
			}
		}
		boolean didSet = false;
		final MetaProperty mp = MetaPropertyLookup.getSettableMetaProperty(o, name);
		if(mp!=null) {
			try {
				if(mp instanceof MetaBeanProperty) {
					MetaBeanProperty mbp = (MetaBeanProperty) mp;
					CachedField cf = mbp.getField();
					Type type = mp.getType();
					if(cf!=null) {
						type = cf.field.getGenericType();
					}
					if(type!=null) {
						value = GroovityObjectConverter.convert(value, type);
					}
				}
				mp.setProperty(o, value);
			}
			catch(RuntimeException e) {
				String message = "Error setting property "+name+" on "+o.getClass().getName();
				if(value!=null) {
					message += " to value type "+value.getClass().getName();
				}
				throw new RuntimeException(message,e);
			}
			didSet = true;
		}
		if(!didSet) {
			if(o instanceof Map) {
				((Map<String,Object>)o).put(name, value);
				didSet=true;
			}
		}
		return didSet;
	}
	
	/**
	 * Fill this model with values from the given map
	 * 
	 * @param data
	 */
	public default Model putAll(Map<String,Object> data) {
		if(data!=null) {
			data.forEach(this::put);
		}
		return this;
	}
	
	/**
	 * Produce a loosely structured Map or List from a Model
	 * @return
	 * @throws Exception
	 */
	public default Object map() throws Exception {
		return Model.mapObject(this);
	}
	
	/**
	 * Produce a loosely structure Map or List from a Model, applying one or more ModelFilters
	 * to modify the model as it is collected.
	 * 
	 * @param filters
	 * @return
	 * @throws Exception
	 */
	public default Object map(ModelFilter... filters) throws Exception {
		return Model.mapObject(this, filters);
	}
	
	/**
	 * Produce a loosely structure Map or List from a Model, applying one or more ModelFilters
	 * to modify the model as it is collected.
	 * 
	 * @param filters
	 * @return
	 * @throws Exception
	 */
	public default Object map(Collection<ModelFilter> filters) throws Exception {
		return Model.mapObject(this, filters);
	}
	
	/**
	 * Produce a loosely structured Map or List from any object
	 * 
	 * @param m
	 * @return
	 * @throws Exception
	 */
	public static Object mapObject(Object m) throws Exception {
		ModelCollector collector = new ModelCollector();
		collector.visit(m);
		return collector.getCollected();
	}
	
	/**
	 * Produce a loosely structured Map or List from any object, applying one or more ModelFilters
	 * to modify the model as it is collected.
	 * 
	 * @param m
	 * @param filters
	 * @return
	 * @throws Exception
	 */
	public static Object mapObject(Object m, Collection<ModelFilter> filters) throws Exception {
		return mapObject(m,filters.toArray(new ModelFilter[filters.size()]));
	}
	
	/**
	 * Produce a loosely structured Map or List from any object, applying one or more ModelFilters
	 * to modify the model as it is collected.
	 * 
	 * @param m
	 * @param filters
	 * @return
	 * @throws Exception
	 */
	public static Object mapObject(Object m, ModelFilter... filters) throws Exception {
		ModelCollector collector = new ModelCollector();
		collector.setFilters(filters);
		collector.visit(m);
		return collector.getCollected();
	}
	
	/**
	 * Serialize a model to a JSON string
	 * @return
	 * @throws Exception
	 */
	public default String toJsonString() throws Exception{
		return ModelJsonWriter.toJsonString(this, "\t");
	}
	
	@SuppressWarnings("unchecked")
	public default void readExternal(ObjectInput oi) throws IOException{
		String json = oi.readUTF();
		Object data = new JsonSlurper().parseText(json);
		if(data instanceof Collection){
			((Collection<Object>)this).addAll((Collection<Object>)data);
		}
		else{
			putAll((Map<String,Object>)data);
		}
	}

	public default void writeExternal(ObjectOutput ou) throws IOException{
		try {
			ou.writeUTF(ModelJsonWriter.toJsonString(this, ""));
		} catch(IOException ie){
			throw ie;
		} catch (Exception e) {
			throw new IOException("Error externalizing Model", e);
		}
	}

}

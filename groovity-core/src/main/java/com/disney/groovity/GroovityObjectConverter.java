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
package com.disney.groovity;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import com.disney.groovity.model.Model;
import com.disney.groovity.model.ModelConsumer;
/**
 * Static facade to centralize advanced type coercion used by ArgsResolver and the Taggable.resolve() method;
 * this includes autoboxing to and from list and array types, and conversion to numeric and boolean types as well.
 * Falls back on groovy's DefaultTypeTransformation for basic type conversions.
 *
 * @author Alex Vigdor
 */
public class GroovityObjectConverter {
	private static final Logger log = Logger.getLogger(GroovityObjectConverter.class.getName());
	@SuppressWarnings("rawtypes")
	private static class ArrayIterable implements Iterable{
		final private Object arr;
		final private int len;
		private ArrayIterable(Object arr) {
			this.arr=arr;
			this.len = Array.getLength(arr);
		}

		@Override
		public Iterator iterator() {
			return new Iterator<Object>() {
				int pointer = 0;
				
				@Override
				public boolean hasNext() {
					return pointer < len;
				}

				@Override
				public Object next() {
					return Array.get(arr, pointer++);
				}
			};
		}
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object convertIterable(Iterable in, Class out, Type[] typeArgs) {
		//return coerced array
		if(out.isArray()){
			Class componentType = out.getComponentType();
			ArrayList<Object> outObjs = new ArrayList<Object>();
			for(Object i: in){
				Object val = convert(i,componentType);
				outObjs.add(val);
			}
			Object outArray = Array.newInstance(componentType, outObjs.size());
			for(int i=0;i<outObjs.size();i++){
				Object val = outObjs.get(i);
				Array.set(outArray, i, val);
			}
			return outArray;
		}
		if(List.class.isAssignableFrom(out)) {
			List<Object> outObjs = null;
			if(!out.isInterface()) {
				try {
					outObjs = (List<Object>) out.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
				}
			}
			if(outObjs == null) {
				outObjs = new ArrayList<>();
			}
			Type componentType = Object.class;
			if(typeArgs!=null && typeArgs.length>0) {
				componentType = typeArgs[0];
				//System.out.println("CONVERTING ITERABLE TO GENERIC LIST "+componentType);
			}
			for(Object i: in){
				Object val = convert(i,componentType);
				outObjs.add(val);
			}
			return outObjs;
		}
		//return first non-null value
		for(Object i: in){
			Object val = convert(i,out);
			if(val!=null){
				return val;
			}
		}
		return null;
	}
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Object convert(Object in, Type type){
		if(in==null){
			return null;
		}
		Class<?> out;
		Type[] typeArguments = null;
		if(type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			out = (Class<?>)pt.getRawType();
			typeArguments = pt.getActualTypeArguments();
		}
		else {
			out = (Class<?>) type;
		}
		if(out.equals(Object.class)){
			//special case, no conversion at all
			return in;
		}
		if(type.equals(in.getClass())) {
			return in;
		}
		if(typeArguments==null && out.isAssignableFrom(in.getClass())) {
			return in;
		}
		if(in.getClass().isArray()){
			return convertIterable(new ArrayIterable(in), out, typeArguments);
		}
		if(List.class.isAssignableFrom(in.getClass())){
			List inList = (List) in;
			return convertIterable(inList, out, typeArguments);
		}
		if(out.equals(String.class)) {
			return in.toString();
		}
		if(Date.class.isAssignableFrom(out)) {
			if(!(in instanceof Number)) {
				in = convert(in,Long.class);
				if(in==null) {
					return null;
				}
			}
			return new Date(((Number)in).longValue());
		}
		if(out.isArray()){
			Object val = convert(in,out.getComponentType());
			if(val!=null){
				//if we're expecting an array back but the input is not an array, let's wrap it
				Object[] ra = (Object[]) Array.newInstance(out.getComponentType(), 1);
				ra[0]=val;
				return ra;
			}
			return Array.newInstance(out.getComponentType(), 0);
		}
		if(out.isEnum()) {
			Enum val = Enum.valueOf((Class<Enum>) out, in.toString());
			return val;
		}
		if(Number.class.isAssignableFrom(out)){
			if(Number.class.isAssignableFrom(in.getClass())) {
				return DefaultTypeTransformation.castToType(in, out);
			}
			String ps = getParseable(in);
			if(ps==null){
				return null;
			}
			//groovy can only convert single digit number strings for some odd reason, so we have to roll our own
			if(Integer.class.isAssignableFrom(out)){
				return Integer.parseInt(ps);
			}
			if(Long.class.isAssignableFrom(out)){
				return Long.parseLong(ps);
			}
			if(Float.class.isAssignableFrom(out)){
				return Float.parseFloat(ps);
			}
			if(Double.class.isAssignableFrom(out)){
				return Double.parseDouble(ps);
			}
		}
		if(out.isPrimitive()){
			if(Number.class.isAssignableFrom(in.getClass())) {
				return DefaultTypeTransformation.castToType(in, out);
			}
			String ps = getParseable(in);
			if(ps==null){
				return null;
			}
			if(out.equals(Integer.TYPE)){
				return Integer.parseInt(ps);
			}
			if(out.equals(Long.TYPE)){
				return Long.parseLong(ps);
			}
			if(out.equals(Float.TYPE)){
				return Float.parseFloat(ps);
			}
			if(out.equals(Double.TYPE)){
				return Double.parseDouble(ps);
			}
		}
		if(Boolean.class.equals(out) || out == Boolean.TYPE){
			Class ic = in.getClass();
			if(!Boolean.class.equals(ic) && ic != Boolean.TYPE){
				String ps = getParseable(in);
				if(ps!=null){
					return Boolean.parseBoolean(ps);
				}
			}
		}
		if(Map.class.isAssignableFrom(out)) {
			Map<Object,Object> result = null;
			if(!out.isInterface()){
				try {
					result = (Map<Object,Object>) out.newInstance();
				} catch (Exception e) {
				} 
			}
			if(result==null) {
				result = new LinkedHashMap<>();
			}
			Type kType;
			Type vType;
			if(typeArguments!=null) {
				kType = typeArguments[0];
				vType = typeArguments[1];
			}
			else {
				kType = Object.class;
				vType = Object.class;
			}
			Map<Object,Object> finalResult = result;
			ModelConsumer consumer = (k,v)->{
				finalResult.put(convert(k,kType), convert(v, vType));
			};
			consume(in, consumer);
			return finalResult;
		}
		if(!out.isPrimitive() && !out.isInterface()) {
			try {
				if(in instanceof CharSequence) {
					//treat empty string as null
					if(((CharSequence)in).length()==0) {
						return null;
					}
					//special handling for string inputs, look for string-based constructors
					try {
						Constructor<?> c = out.getConstructor(String.class);
						return c.newInstance(in.toString());
					}
					catch(Exception e) {}
				}
				Constructor<?> c = out.getConstructor();
				Object o = c.newInstance();
				if(o instanceof Model) {
					consume(in, ((Model)o)::put);
				}
				else {
					consume(in,(k,v)->{
						Model.put(o, k, v);
					});
				}
				//System.out.println("USED MODEL TO CONVERT "+in.getClass().getName()+" to "+out.getName());
				return o;
			}
			catch(Exception e) { 
				log.log(Level.WARNING, "Error converting "+in.getClass().getName()+" to "+out.getName(), e);
			}
		}
		return DefaultTypeTransformation.castToType(in,out);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void consume(Object in, ModelConsumer consumer) {
		if(in instanceof Map) {
			((Map)in).forEach(consumer);
		}
		else if(in instanceof Model) {
			((Model)in).each(consumer);
		}
		else {
			Model.each(in, consumer);
		}
	}
	
	private static String getParseable(Object in){
		if(in==null){
			return null;
		}
		String inStr = in.toString();
		if(inStr.length()==0 || "null".equals(inStr)){
			return null;
		}
		return inStr;
	}
	
}

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * Defines a general API with abstract visit methods for the 5 primary modeled types;
 * Object and Object Field, List and List Member, and Null.  A default entry-point visit() method
 * provides general purpose logic for routing to the other 5 visit methods based on the type of argument.
 * 
 * @author Alex Vigdor
 *
 */
public interface ModelVisitor extends ModelConsumer{

	void visitNull() throws Exception;

	@SuppressWarnings("rawtypes")
	void visitList(Iterable iter) throws Exception;
	
	void visitListMember(Object obj) throws Exception;

	void visitObject(Object obj) throws Exception;

	void visitObjectField(String name, Object value) throws Exception;
	
	public default void call(String key, Object value) {
		try {
			visitObjectField(key, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public default Iterable<?> toIterableIfPossible(Object obj) {
		if(obj==null) {
			return null;
		}
		if(obj instanceof Throwable) {
			return null;
		}
		if(obj instanceof Iterable) {
			return (Iterable<?>) obj;
		}
		if(obj.getClass().isArray()) {
			Class<?> ct = obj.getClass().getComponentType();
			if(ct.isPrimitive()) {
				if(ct.equals(long.class)) {
					List<Long> ll =new ArrayList<>();
					for(long l : ((long[]) obj)) {
						ll.add(l);
					}
					return ll;
				}
				if(ct.equals(int.class)) {
					List<Integer> ll =new ArrayList<>();
					for(int l : ((int[]) obj)) {
						ll.add(l);
					}
					return ll;
				}
				if(ct.equals(double.class)) {
					List<Double> ll =new ArrayList<>();
					for(double l : ((double[]) obj)) {
						ll.add(l);
					}
					return ll;
				}
				if(ct.equals(float.class)) {
					List<Float> ll =new ArrayList<>();
					for(float l : ((float[]) obj)) {
						ll.add(l);
					}
					return ll;
				}
				if(ct.equals(short.class)) {
					List<Short> ll =new ArrayList<>();
					for(short l : ((short[]) obj)) {
						ll.add(l);
					}
					return ll;
				}
				if(ct.equals(boolean.class)) {
					List<Boolean> ll =new ArrayList<>();
					for(boolean l : ((boolean[]) obj)) {
						ll.add(l);
					}
					return ll;
				}
				//intentionally don't handle char[], those should be treated as strings
			}
			else {
				return Arrays.asList((Object[])obj);
			}
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public default void visit(Object obj) throws Exception {
		if(obj == null) {
			visitNull();
			return;
		}
		Iterable i = toIterableIfPossible(obj);
		if(i!=null) {
			visitList(i);
		}
		else {
			if(obj.getClass().isArray() && obj.getClass().getComponentType().equals(char.class)) {
				obj = new String((char[])obj);
			}
			visitObject(obj);
		}
	}
}
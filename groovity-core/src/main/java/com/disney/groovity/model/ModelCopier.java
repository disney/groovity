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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
/**
 * Utility class for recursively deep-copying Models, Maps, Collections and Arrays, using an IdentityHashMap to copy circular references
 * without going into stack overflow
 * 
 * @author Alex Vigdor
 */
public class ModelCopier {
	final IdentityHashMap<Object,Object> copied = new IdentityHashMap<>();
	
	private final Object makeNew(Class<?> c) {
		try {
			return c.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public final Object copy(final Object o) {
		if(o==null) {
			return null;
		}
		final Class c = o.getClass();
		if(o instanceof Model) {
			final Object precopy = copied.get(o);
			if(precopy != null) {
				return precopy;
			}
			final Model m = (Model) o;
			final Model to =  (Model) makeNew(c);
			copied.put(o, to);
			m.each((k,v)->{
				to.put(k, copy(v));
			});
			return to;
		}
		if(o instanceof Map) {
			if(o == Collections.EMPTY_MAP) {
				return o;
			}
			final Object precopy = copied.get(o);
			if(precopy != null) {
				return precopy;
			}
			final Map<Object,Object> from = (Map)o;
			final Map to =  (Map) makeNew(c);
			copied.put(o, to);
			for(java.util.Map.Entry<Object, Object> e: from.entrySet()) {
				to.put(e.getKey(), copy(e.getValue()));
			}
			return to;
		}
		if(o instanceof Collection) {
			if(o == Collections.EMPTY_LIST || o == Collections.EMPTY_SET) {
				return o;
			}
			final Object precopy = copied.get(o);
			if(precopy != null) {
				return precopy;
			}
			final Collection from = (Collection) o;
			final Collection to =  (Collection) makeNew(c);
			copied.put(o, to);
			for(Object f: from) {
				to.add(copy(f));
			}
			return to;
		}
		if(c.isArray()) {
			final int len =  Array.getLength(o);
			if(len == 0) {
				return o;
			}
			final Object precopy = copied.get(o);
			if(precopy != null) {
				return precopy;
			}
			final Class ct = c.getComponentType();
			if(ct.isPrimitive()) {
				final Object to = Array.newInstance(ct,len);
				System.arraycopy(o, 0, to, 0, len);
				copied.put(o, to);
				return to;
			}
			else {
				final Object[] oa = (Object[]) o;
				final Object[] to = (Object[]) Array.newInstance(ct, len);
				copied.put(o, to);
				for(int i=0; i < len; i++) {
					to[i] = copy(oa[i]);
				}
				return to;
			}
		}
		return o;
	}
}

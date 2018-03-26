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

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import groovy.lang.Closure;
import groovy.lang.Writable;
/**
 * A ModelVisitor that devolves a model into Maps and Lists
 *
 * @author Alex Vigdor
 */
public class ModelCollector extends ModelWalker {
	static final Object NULL_OBJECT = new Object();
	protected ArrayDeque<Object> collected = new ArrayDeque<>();

	public ModelCollector() {
	}

	public void visitNull() throws Exception {
		collected.push(NULL_OBJECT);
	}
	
	@SuppressWarnings("rawtypes") 
	public void visitList(Iterable iter) throws Exception {
		List copy = new ArrayList();
		collected.push(copy);
		super.visitList(iter);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" }) 
	public void visitListMember(Object obj) throws Exception{
		super.visitListMember(obj);
		Object tval = collected.pop();
		if(tval == NULL_OBJECT){
			tval = null;
		}
		List l = (List) collected.peek();
		l.add(tval);
	}

	public void visitObject(Object obj) throws Exception{
		if(obj == null
			|| obj instanceof CharSequence
			|| obj instanceof Number 
			|| obj instanceof Boolean 
			|| obj instanceof Date
			|| obj instanceof Closure 
			|| obj instanceof Future
			|| obj instanceof URI 
			|| obj instanceof URL 
			|| obj.getClass().isEnum()
			|| obj instanceof File
			|| obj instanceof byte[]
			|| (obj instanceof Writable  && !(obj instanceof Model))) {
				collected.push(obj);
		}
		else {
			@SuppressWarnings("rawtypes")
			Map copy = new LinkedHashMap();
			collected.push(copy);
			super.visitObject(obj);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void visitObjectField(String name, Object value) throws Exception {
		super.visitObjectField(name, value);
		Object tval = collected.pop();
		if(tval == NULL_OBJECT){
			tval = null;
		}
		Object obj = collected.peek();
		if(obj instanceof Model){
			((Model)obj).put(name, tval);
		}
		else if(obj instanceof Map){
			((Map)obj).put(name, tval);
		}
	}

	public Object getCollected(){
		if(collected.isEmpty()) {
			return null;
		}
		Object val = collected.peek();
		if(val == NULL_OBJECT){
			return null;
		}
		return val;
	}
}

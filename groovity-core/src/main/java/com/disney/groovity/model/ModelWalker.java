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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Model Walker meant to support both serialization and transformation methods; instance contains computational state and should not be
 * considered threadsafe
 * 
 * @author Alex Vigdor
 *
 */
public abstract class ModelWalker implements ModelVisitor {
	private static final Object KNOWN_IDENTITY  = new Object();
	private final ModelConsumer fieldHandler = ((ModelConsumer)this::handleField);
	protected ArrayList<String> fields = new ArrayList<>();
	protected List<String> readOnlyFields = (List<String>) Collections.unmodifiableList(fields);
	protected ArrayDeque<AtomicInteger> positions  = new ArrayDeque<>();
	protected Collection<AtomicInteger> readOnlyPositions = (Collection<AtomicInteger>) Collections.unmodifiableCollection(positions);
	protected IdentityHashMap<Object, Object> objects = new IdentityHashMap<>();
	protected ArrayDeque<Object> objectStack = new ArrayDeque<>();
	protected Map<String, Object> virtualFields;
	private ModelVisitor filterVisitor = null;
	protected int lastPos = 0;
	
	
	public List<String> getFields(){
		return readOnlyFields;
	}
	
	public Collection<AtomicInteger> getPositions(){
		return readOnlyPositions;
	}
	
	public Object getCurrentObject() {
		return objectStack.peek();
	}

	@Override
	public void visitNull() throws Exception {

	}

	@Override
	public void visitList(@SuppressWarnings("rawtypes") Iterable iter) throws Exception {
		if(objects.put(iter,KNOWN_IDENTITY) !=null) {
			//avoid processing circular references!!!
			return;
		}
		AtomicInteger pos = new AtomicInteger(0);
		positions.push(pos);
		try {
			for(Object o : iter) {
				visitListMember(o);
				pos.incrementAndGet();
			}
		}
		finally {
			lastPos = positions.pop().get();
			objects.remove(iter);
		}
	}

	@Override
	public void visitObject(Object obj) throws Exception{
		@SuppressWarnings("rawtypes")
		Class oc  = obj.getClass();
		Package p = oc.getPackage();
		if(p!=null) {
			String pn = p.getName();
			if(pn.startsWith("java.lang") || pn.startsWith("groovy.lang")) {
				//don't devolve basic objects
				return;
			}
		}
		if(objects.put(obj,KNOWN_IDENTITY) !=null) {
			//avoid processing circular references!!!
			return;
		}
		AtomicInteger pos = new AtomicInteger(0);
		positions.push(pos);
		objectStack.push(obj);
		try {
			if(virtualFields !=null ) {
				Map<String,Object> pushFields = virtualFields;
				virtualFields = null;
				pushFields.forEach(fieldHandler);
			}
			fieldHandler.visitObjectFields(obj);
		}
		finally {
			lastPos = positions.pop().get();
			objects.remove(obj);
			objectStack.pop();
		}
	}
	
	public void addVirtualField(String name, Object value) {
		if(virtualFields==null) {
			virtualFields = new LinkedHashMap<>();
		}
		virtualFields.put(name,value);
	}

	protected void handleField(String name, Object value){
		if(filterVisitor!=null) {
			filterVisitor.call(name, value);
		}
		else {
			try {
				visitObjectField(name, value);
			} catch (Exception e) {
				throw new RuntimeException("Error handling field "+name+" on "+getCurrentObject().getClass().getName()+" with value "+value, e);
			}
		}
	}

	@Override
	public void visitObjectField(String name, Object value) throws Exception {
		fields.add(name);
		try {
			visit(value);
			positions.peek().incrementAndGet();
		}
		finally {
			fields.remove(fields.size()-1);
		}
	}

	@Override
	public void visitListMember(Object obj) throws Exception{
		visit(obj);
	}
	
	public void visit(Object o) throws Exception {
		if(filterVisitor!=null) {
			filterVisitor.visit(o);
		}
		else {
			ModelVisitor.super.visit(o);
		}
	}

	public void setFilters(ModelFilter[] filters) {
		if(filters==null || filters.length==0) {
			this.filterVisitor = this;
			return;
		}
		this.filterVisitor = ModelFilter.filteredVisitor(this, filters);
	}
}

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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.codehaus.groovy.runtime.InvokerHelper;

import groovy.lang.Closure;

/**
 * Provide a full java API for a model filter that can intercept all 5 methods of a ModelVisitor,
 * but can also be used as a functional/lambda API for specifically filtering just object fields.
 * 
 * Provide a number of static methods for constructing common convenient filters; these generally
 * take either a set of field path patterns (e.g. "image.meta.*" or "doc.title") or java classes
 * to target specific members of a model.
 * 
 * @author Alex Vigdor
 *
 */
@FunctionalInterface
public interface ModelFilter {
	/**
	 * Filter a list type, by default just pass through to the visitor
	 * 
	 * @param list
	 * @param visitor
	 * @throws Exception
	 */
	public default void filterList(@SuppressWarnings("rawtypes") Iterable list, ModelVisitor visitor) throws Exception {
		visitor.visitList(list);
	}
	
	/**
	 * Filter a list member, by default just pass through to the visitor
	 * @param member
	 * @param visitor
	 * @throws Exception
	 */
	public default void filterListMember(Object member, ModelVisitor visitor) throws Exception {
		visitor.visitListMember(member);
	}
	
	/**
	 * Filter a null value, by default just pass through to the visitor
	 * @param visitor
	 * @throws Exception
	 */
	public default void filterNull(ModelVisitor visitor) throws Exception {
		visitor.visitNull();
	}
	
	/**
	 * Filter an object, by default just pass it through to the visitor
	 * @param obj
	 * @param visitor
	 * @throws Exception
	 */
	public default void filterObject(Object obj, ModelVisitor visitor) throws Exception {
		visitor.visitObject(obj);
	}
	
	/**
	 * Filter an object field; this is the functional interface method and has no default implementation
	 * 
	 * @param name
	 * @param value
	 * @param visitor
	 * @throws Exception
	 */
	public void filterObjectField(String name, Object value, ModelVisitor visitor) throws Exception;
	
	/**
	 * Construct a FilterVisitor by applying one or more ModelFilters to another ModelVisitor;
	 * the filters are stacked and executed by depth in the order declared
	 * 
	 * @param visitor
	 * @param filters
	 * @return
	 */
	public static ModelVisitor filteredVisitor(ModelVisitor visitor, final ModelFilter... filters) {
		for(int i=filters.length-1;i>=0;i--) {
			visitor = new FilteredVisitor(filters[i], visitor);
		}
		return visitor;
	}
	
	/**
	 * Construct a custom ModelFilter from a Groovy Closure; the closure will be passed three arguments
	 * for every field in the model
	 * <ul>
	 * <li>k = the field name</li>
	 * <li>v = the field value</li>
	 * <li>c = the ModelVisitor that would like to visitObjectField</li>
	 * </ul>
	 * 
	 * an identity transformation simply passes through would be
	 * <pre>
	 * { k, v, c -&gt; 
	 * 	c(k, v)
	 * }
	 * </pre>
	 * @param cl
	 * @return
	 */
	public static ModelFilter custom(@SuppressWarnings("rawtypes") Closure cl){
		return (k, v, c) -> { cl.call(k, v, c); };
	}
	
	/**
	 * A ModelVisitor that applies a filter to another visitor
	 */
	public static class FilteredVisitor implements ModelVisitor{
		final ModelFilter filter;
		final ModelVisitor dest;
		
		public FilteredVisitor(ModelFilter filter, ModelVisitor dest) {
			this.filter=filter;
			this.dest=dest;
		}

		@Override
		public void visitNull() throws Exception {
			filter.filterNull(dest);
		}

		@Override
		public void visitList(@SuppressWarnings("rawtypes") Iterable iter) throws Exception {
			filter.filterList(iter, dest);
		}

		@Override
		public void visitListMember(Object obj) throws Exception {
			filter.filterListMember(obj, dest);
		}

		@Override
		public void visitObject(Object obj) throws Exception {
			filter.filterObject(obj, dest);
		}

		@Override
		public void visitObjectField(String name, Object value) throws Exception {
			filter.filterObjectField(name, value, dest);
		}
	}
	
	/**
	 * Suppress fields with null values from model
	 */
	public static ModelFilter NULL = new ModelFilter(){
		public void filterListMember(Object member, ModelVisitor visitor) throws Exception {
			if(member!=null) {
				visitor.visitListMember(member);
			}
		}
		public void filterObjectField(String name, Object value, ModelVisitor consumer) throws Exception {
			if(value!=null) {
				consumer.call(name,value);
			}
		}
	};
	
	/**
	 * Exclude one or more field path patterns from visitation
	 * 
	 * @param fieldPaths
	 * @return
	 */
	public static ModelFilter exclude(String... fieldPaths) {
		return new FieldMatcher(fieldPaths) {
			@Override
			protected void filteredObjectField(boolean matched, String name, Object value, ModelVisitor consumer)
					throws Exception {
				if(!matched) {
					consumer.call(name, value);
				}
			}
		};
	}
	
	/**
	 * Exclude fields or list members according to java class 
	 * 
	 * @param type
	 * @return
	 */
	public static ModelFilter exclude(Class<?> type) {
		return new ModelFilter() {
			public void filterListMember(Object member, ModelVisitor visitor) throws Exception {
				if(member==null || !type.isAssignableFrom(member.getClass())) {
					visitor.visitListMember(member);
				}
			}
			@Override
			public void filterObjectField(String name, Object value, ModelVisitor consumer)
					throws Exception {
				if(value==null || !type.isAssignableFrom(value.getClass())) {
					consumer.call(name,value);
				}
			}
		};
	}
	
	/**
	 * Exclude fields of a specific java class
	 * 
	 * @param type
	 * @param fields
	 * @return
	 */
	public static ModelFilter exclude(Class<?> type, String... fields) {
		return new TypeFieldMatcher(type, fields) {
			@Override
			protected void filteredObjectField(boolean matched, String name, Object value, ModelVisitor consumer)
					throws Exception {
				if(!matched) {
					consumer.call(name, value);
				}
			}
		};
	}
	/**
	 * Include fields matching a set of field path patterns, filtering out fields that do not match
	 * 
	 * @param fieldPaths
	 * @return
	 */
	public static ModelFilter include(String... fieldPaths) {
		HashSet<String> recursivePaths = new HashSet<>();
		StringBuilder builder = new StringBuilder();
		for(int j=0; j<fieldPaths.length; j++) {
			String[] comps = fieldPaths[j].split("\\.");
			builder.setLength(0);
			for(int i = 0 ; i < comps.length ; i++) {
				if(i>0) {
					builder.append(".");
				}
				builder.append(comps[i]);
				recursivePaths.add(builder.toString());
			}
		}
		return new FieldMatcher(recursivePaths.toArray(new String[0])) {
			@Override
			protected void filteredObjectField(boolean matched, String name, Object value, ModelVisitor consumer)
					throws Exception {
				if(matched) {
					consumer.call(name, value);
				}
			}
		};
	}
	
	/**
	 * Include fields of a specific java class, filtering out fields that do not match
	 * 
	 * @param type
	 * @param fields
	 * @return
	 */
	public static ModelFilter include(Class<?> type, String... fields) {
		return new TypeFieldMatcher(type, fields) {
			@Override
			protected void filteredObjectField(boolean matched, String name, Object value, ModelVisitor consumer)
					throws Exception {
				if(matched) {
					consumer.call(name, value);
				}
			}
		};
	}
	
	/**
	 * Transform the value of a field by path pattern
	 * 
	 * @param fieldPath
	 * @param transformer
	 * @return
	 */
	public static ModelFilter transform(String fieldPath, Function<Object, Object> transformer) {
		return new FieldMatcher(new String[] { fieldPath }) {
			@Override
			protected void filteredObjectField(boolean matched, String name, Object value, ModelVisitor consumer)
					throws Exception {
				if(matched) {
					consumer.call(name, transformer.apply(value));
				}
				else {
					consumer.call(name, value);
				}
			}
		};
	}
	
	/**
	 * Transform the value of a field of a specific java class
	 * 
	 * @param type
	 * @param field
	 * @param transformer
	 * @return
	 */
	public static ModelFilter transform(Class<?> type, String field, Function<Object, Object> transformer) {
		return new TypeFieldMatcher(type, new String[] { field }) {
			@Override
			protected void filteredObjectField(boolean matched, String name, Object value, ModelVisitor consumer)
					throws Exception {
				if(matched) {
					consumer.call(name, transformer.apply(value));
				}
				else {
					consumer.call(name, value);
				}
			}
		};
	}
	
	/**
	 * Transform a specific java class
	 * @param type
	 * @param transformer
	 * @return
	 */
	public static <T> ModelFilter transform(Class<T> type, Function<T, Object> transformer) {
		return new TypeMatcher<T>(type) {
			@Override
			protected void filteredObject(ModelWalker mw, T value, ModelVisitor consumer)
					throws Exception {
				consumer.visitObject(transformer.apply(value));
			}
		};
	}

	/**
	 * Invoke a method with arguments on an object according to its path pattern;
	 * the result is added to a named virtual field, or if no virtual field name is provided
	 * the result of the method call replaces the object in the model
	 * 
	 * @param objectPath
	 * @param methodName
	 * @param virtualFieldName
	 * @param args
	 * @return
	 */
	public static ModelFilter invoke(String objectPath, String methodName, String virtualFieldName, Object... args) {
		return new ObjectMatcher(new String[] { objectPath==null ? "" : objectPath }) {
			@Override
			protected void filteredObject(ModelWalker walker, boolean matched, Object value, ModelVisitor consumer) throws Exception {
				if(matched) {
					Object result = InvokerHelper.invokeMethod(value, methodName, args);
					if(virtualFieldName!=null && !virtualFieldName.isEmpty()) {
						if(walker != null) {
							walker.addVirtualField(virtualFieldName, result);
						}
					}
					else {
						//no virtual field specified, instead the result will replace the object
						value = result;
					}
				}
				consumer.visitObject(value);
			}
		};
	}
	
	/**
	 * Invoke a method with arguments on an object according to its java class;
	 * the result is added to a named virtual field, or if no virtual field name is provided
	 * the result of the method call replaces the object in the model
	 * 
	 * @param type
	 * @param methodName
	 * @param virtualFieldName
	 * @param args
	 * @return
	 */
	public static <T> ModelFilter invoke(Class<T> type, String methodName, String virtualFieldName, Object... args) {
		return new TypeMatcher<T>(type) {
			@Override
			protected void filteredObject(ModelWalker walker, T value, ModelVisitor consumer) throws Exception {
				Object result = InvokerHelper.invokeMethod(value, methodName, args);
				if(virtualFieldName!=null && !virtualFieldName.isEmpty()) {
					if(walker != null) {
						walker.addVirtualField(virtualFieldName, result);
					}
					consumer.visitObject(value);
				}
				else {
					//no virtual field specified, instead the result will replace the object
					consumer.visitObject(result);
				}
				
			}
		};
	}
	
	/**
	 * Add a new virtual field at a specific virtual field path
	 * 
	 * @param virtualFieldPath
	 * @param valueCreator function that takes the parent object of the virtual field path and returns the virtual field value
	 * @return
	 */
	public static ModelFilter add(String virtualFieldPath, Function<Object, Object> valueCreator) {
		if(virtualFieldPath==null || virtualFieldPath.isEmpty()) {
			throw new IllegalArgumentException("Cannot add virtual field with no name");
		}
		String objectPath = "";
		final String virtualFieldName;
		int dot = virtualFieldPath.lastIndexOf(".");
		if(dot>0) {
			objectPath = virtualFieldPath.substring(0, dot);
			virtualFieldName = virtualFieldPath.substring(dot+1);
			if(virtualFieldName.isEmpty()) {
				throw new IllegalArgumentException("Cannot add virtual field with no name");
			}
		}
		else {
			virtualFieldName = virtualFieldPath;
		}
		return new ObjectMatcher(new String[] { objectPath }) {
			@Override
			protected void filteredObject(ModelWalker walker, boolean matched, Object value, ModelVisitor consumer) throws Exception {
				if(matched && walker!=null) {
					Object result = valueCreator.apply(value);
					walker.addVirtualField(virtualFieldName, result);
				}
				consumer.visitObject(value);
			}
		};
	}
	
	/**
	 * Add a new virtual field to a specific java class
	 * 
	 * @param type
	 * @param virtualFieldName
	 * @param valueCreator function that takes an object of the specified type and returns the virtual field value
	 * @return
	 */
	public static <T> ModelFilter add(Class<T> type, String virtualFieldName, Function<T, Object> valueCreator) {
		return new TypeMatcher<T>(type) {
			@Override
			protected void filteredObject(ModelWalker walker, T value, ModelVisitor consumer) throws Exception {
				if(walker!=null) {
					Object result = valueCreator.apply(value);
					walker.addVirtualField(virtualFieldName, result);
				}
				consumer.visitObject(value);
			}
		};
	}

	/**
	 * Copy a field at a specific path pattern into a new virtual field, transforming the value
	 * 
	 * @param fieldPath
	 * @param newFieldName
	 * @param transformer
	 * @return
	 */
	public static ModelFilter copy(String fieldPath, String newFieldName, Function<Object, Object> transformer) {
		return new FieldMatcher(new String[] { fieldPath }) {
			@Override
			protected void filteredObjectField(boolean matched, String name, Object value, ModelVisitor consumer)
					throws Exception {
				if(matched) {
					consumer.call(newFieldName, transformer.apply(value));
				}
				consumer.call(name, value);
			}
		};
	}

	/**
	 * Copy a field of a specific java clas into a new virtual field, transforming the value
	 * 
	 * @param type
	 * @param field
	 * @param newFieldName
	 * @param transformer
	 * @return
	 */
	public static ModelFilter copy(Class<?> type, String field, String newFieldName, Function<Object, Object> transformer) {
		return new TypeFieldMatcher(type, new String[] { field }) {
			@Override
			protected void filteredObjectField(boolean matched, String name, Object value, ModelVisitor consumer)
					throws Exception {
				if(matched) {
					consumer.call(newFieldName, transformer.apply(value));
				}
				consumer.call(name, value);
			}
		};
	}
	
	/**
	 * Rename a field according to its path in the model
	 * 
	 * @param fieldPath
	 * @param newFieldName
	 * @return
	 */
	public static ModelFilter rename(String fieldPath, String newFieldName) {
		return new FieldMatcher(new String[] { fieldPath }) {
			@Override
			protected void filteredObjectField(boolean matched, String name, Object value, ModelVisitor consumer)
					throws Exception {
				if(matched) {
					consumer.call(newFieldName, value);
				}
				else {
					consumer.call(name, value);
				}
			}
		};
	}
	
	/**
	 * Rename a field of a java class
	 * 
	 * @param type
	 * @param field
	 * @param newFieldName
	 * @return
	 */
	public static ModelFilter rename(Class<?> type, String field, String newFieldName) {
		return new TypeFieldMatcher(type, new String[] { field }) {
			@Override
			protected void filteredObjectField(boolean matched, String name, Object value, ModelVisitor consumer)
					throws Exception {
				if(matched) {
					consumer.call(newFieldName, value);
				}
				else {
					consumer.call(name, value);
				}
			}
		};
	}
	
	/**
	 * Make the fields of objects that match specific field paths appear to be fields of the parent object,
	 * effectively removing a level of depth from the hierarchy
	 * 
	 * @param fieldPaths
	 * @return
	 */
	public static ModelFilter collapse(String... fieldPaths) {
		return new FieldMatcher(fieldPaths) {
			@Override
			protected void filteredObjectField(boolean matched, String name, Object value, ModelVisitor consumer)
					throws Exception {
				if(matched) {
					consumer.visitObjectFields(value);
				}
				else {
					consumer.call(name, value);
				}
			}
		};
	}
	
	/**
	 * Make the children of one or more fields of a java type appear to be direct fields on the type, 
	 * effectively removing a level of depth from the hierarchy
	 * 
	 * @param type
	 * @param fields
	 * @return
	 */
	public static ModelFilter collapse(Class<?> type, String... fields) {
		return new TypeFieldMatcher(type, fields) {
			@Override
			protected void filteredObjectField(boolean matched, String name, Object value, ModelVisitor consumer)
					throws Exception {
				if(matched) {
					consumer.visitObjectFields(value);
				}
				else {
					consumer.call(name, value);
				}
			}
		};
	}
	
	/**
	 * Promote the value of a field targeted by path to replace its parent object in the model
	 * 
	 * @param fieldPath
	 * @return
	 */
	public static ModelFilter promote(String fieldPath) {
		int li = fieldPath.lastIndexOf(".");
		if(li<1) {
			throw new IllegalArgumentException("Field to be promoted must be nested ");
		}
		String fpath = fieldPath.substring(0,li);
		final String promoteField = fieldPath.substring(li+1);
		return new FieldMatcher(new String[] {fpath}) {
			@Override
			protected void filteredObjectField(boolean matched, String name, Object value, ModelVisitor consumer)
					throws Exception {
				if(matched) {
					((ModelConsumer)(k,v)->{
						if(promoteField.equals(k)) {
							consumer.call(name, v);
						}
					}).visitObjectFields(value);
				}
				else {
					consumer.call(name, value);
				}
			}
		};
	}
	
	/**
	 * Promote a field of a specific java class to replace that class in the model
	 * 
	 * @param type
	 * @param field
	 * @return
	 */
	public static <T> ModelFilter promote(Class<T> type, String field) {
		return new TypeMatcher<T>(type) {
			@Override
			protected void filteredObject(ModelWalker mw, T value, ModelVisitor consumer)
					throws Exception {
				((ModelConsumer)(k,v)->{
					if(field.equals(k)) {
						try {
							consumer.visitObject(v);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				}).visitObjectFields(value);
			}
		};
	}
	
	
	/**
	 * FieldMatcher is a specialized base class for ModelFilters that filters object fields with 
	 * knowledge of whether those fields match field path specs like "*", "fieldName", or "refA.refB.*" 
	 * that corresponds to nested objects and fields in the model.
	 * 
	 */
	public abstract static class FieldMatcher implements ModelFilter{
		final String[][] fieldSpecs;
		
		public FieldMatcher(String[] fieldPaths) {
			final int fl = fieldPaths.length;
			fieldSpecs = new String[fl][];
			for(int i=0; i<fl; i++) {
				String fieldPath = fieldPaths[i];
				fieldSpecs[i] = fieldPath.split("\\.");
			}
		}
		
		public void filterObjectField(String name, Object value, ModelVisitor consumer) throws Exception {
			boolean matched = false;
			List<String> fieldStack = null;
			ModelConsumer fieldFinder = consumer;
			while(fieldFinder!=null) {
				if(fieldFinder instanceof ModelWalker) {
					fieldStack = ((ModelWalker)fieldFinder).getFields();
					break;
				}
				if(fieldFinder instanceof FilteredVisitor) {
					fieldFinder = ((FilteredVisitor)fieldFinder).dest;
				}
				else {
					break;
				}
			}
			final int fsn = fieldStack==null ? 0 : fieldStack.size();
			for(int i=0; i<fieldSpecs.length; i++) {
				String[] fieldSpec = fieldSpecs[i];
				int lfs = fieldSpec.length-1;
				if(fsn==lfs) {
					String lastElem = fieldSpec[lfs];
					if(lastElem.equals(name) || lastElem.equals("*")) {
						if(fieldStack!=null) {
							boolean valid = true;
							for(int j=0;j<fsn;j++) {
								String compField = fieldStack.get(j);
								String specField = fieldSpec[j];
								if(!compField.equals(specField) && !specField.equals("*")) {
									valid = false;
									break;
								}
							}
							if(valid) {
								matched = true;
								break;
							}
						}
						else if(lfs==0) {
							matched = true;
							break;
						}
					}
				}
			}
			filteredObjectField(matched, name, value, consumer);
		}
		
		protected abstract void filteredObjectField(boolean matched, String name, Object value, ModelVisitor consumer) throws Exception;
		
	}

	/**
	 * ObjectMatcher is a specialized base class for ModelFilters that filters objects  with 
	 * knowledge of whether those objects position in the model hierarchy match field path specs like "", "*", "fieldName", or "refA.refB.*".
	 *
	 */
	public abstract static class ObjectMatcher implements ModelFilter{
		final String[][] pathSpecs;

		public ObjectMatcher(String[] paths) {
			final int fl = paths.length;
			pathSpecs = new String[fl][];
			for(int i=0; i<fl; i++) {
				String objectPath = paths[i];
				String[] parts = objectPath.split("\\.");
				if(parts.length==1 && parts[0].isEmpty()) {
					parts = new String[] {};
				}
				pathSpecs[i] = parts;
			}
		}

		public void filterObjectField(String name, Object value, ModelVisitor consumer) throws Exception {
			consumer.visitObjectField(name, value);
		}

		public void filterObject(Object value, ModelVisitor consumer) throws Exception {
			boolean matched = false;
			ModelWalker walker = null;
			ModelConsumer fieldFinder = consumer;
			while(fieldFinder!=null) {
				if(fieldFinder instanceof ModelWalker) {
					walker = ((ModelWalker)fieldFinder);
					break;
				}
				if(fieldFinder instanceof FilteredVisitor) {
					fieldFinder = ((FilteredVisitor)fieldFinder).dest;
				}
				else {
					break;
				}
			}
			List<String> fieldStack = null;
			if(walker!=null) {
				fieldStack = walker.getFields();
			}
			final int fsn = fieldStack==null ? 0 : fieldStack.size();
			for(int i=0; i<pathSpecs.length; i++) {
				String[] pathSpec = pathSpecs[i];
				int lfs = pathSpec.length;
				if(fsn==lfs) {
					if(fieldStack!=null) {
						boolean valid = true;
						for(int j=0;j<fsn;j++) {
							String compField = fieldStack.get(j);
							String specField = pathSpec[j];
							if(!compField.equals(specField) && !specField.equals("*")) {
								valid = false;
								break;
							}
						}
						if(valid) {
							matched = true;
							break;
						}
					}
					else if(lfs==0) {
						matched = true;
						break;
					}
				}
			}
			filteredObject(walker, matched, value, consumer);
		}

		protected abstract void filteredObject(ModelWalker walker, boolean matched, Object value, ModelVisitor consumer) throws Exception;
	}
	
	/**
	 * TypeMatcher is a specialized base class for ModelFilters that filters objects based on type
	 *
	 */
	public abstract static class TypeMatcher<T> implements ModelFilter{
		final Class<T> type;

		public TypeMatcher(Class<T> type) {
			this.type = type;
		}

		public void filterObjectField(String name, Object value, ModelVisitor consumer) throws Exception {
			consumer.visitObjectField(name, value);
		}

		public void filterObject(Object value, ModelVisitor consumer) throws Exception {
			boolean matched = type.isInstance(value);
			if(matched) {
				ModelWalker walker = null;
				ModelConsumer fieldFinder = consumer;
				while(fieldFinder!=null) {
					if(fieldFinder instanceof ModelWalker) {
						walker = ((ModelWalker)fieldFinder);
						break;
					}
					if(fieldFinder instanceof FilteredVisitor) {
						fieldFinder = ((FilteredVisitor)fieldFinder).dest;
					}
					else {
						break;
					}
				}
				filteredObject(walker, type.cast(value), consumer);
			}
			else {
				consumer.visitObject(value);
			}
		}

		protected abstract void filteredObject(ModelWalker walker, T value, ModelVisitor consumer) throws Exception;
	}
	
	/**
	 * TypeFieldMatcher is a specialized base class for ModelFilters that filter fields of objects based on type
	 */
	public abstract static class TypeFieldMatcher implements ModelFilter{
		final Class<?> type;
		final Set<String> fields;

		public TypeFieldMatcher(Class<?> type, String[] fields) {
			this.fields = new HashSet<>(Arrays.asList(fields));
			this.type = type;
		}

		public void filterObjectField(String name, Object value, ModelVisitor consumer) throws Exception {
			boolean matched = false;
			Object currentObject = null;
			ModelConsumer objectFinder = consumer;
			while(objectFinder!=null) {
				if(objectFinder instanceof ModelWalker) {
					currentObject = ((ModelWalker)objectFinder).getCurrentObject();
					break;
				}
				if(objectFinder instanceof FilteredVisitor) {
					objectFinder = ((FilteredVisitor)objectFinder).dest;
				}
				else {
					break;
				}
			}
			if(currentObject!=null && type.isInstance(currentObject)) {
				matched = fields.contains(name);
				filteredObjectField(matched, name, value, consumer);
			}
			else {
				consumer.visitObjectField(name, value);
			}
		}
		
		protected abstract void filteredObjectField(boolean matched, String name, Object value, ModelVisitor consumer) throws Exception;
		
	}
}

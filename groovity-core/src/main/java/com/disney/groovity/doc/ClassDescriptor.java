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
package com.disney.groovity.doc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import groovy.lang.GroovyObject;

/**
 * Offers a groovity-friendly abstraction of reflecting; both javabean properies and public fields
 * get rolled together, and property getter/setters are removed from the method list
 * 
 * @author Alex Vigdor
 */
public class ClassDescriptor {
	private List<TypedName> properties;
	private List<Method> methods;
	private Class type;
	private Type superType;
	private Type[] interfaces;
	Map<String,Permutations> namePermutations;

	public ClassDescriptor(Class clazz){
		this.type=clazz;
		if(clazz.getSuperclass()!=null && !clazz.getSuperclass().equals(Object.class)) {
			this.superType=clazz.getGenericSuperclass();
		}
		ArrayList<Type> interfaceTypes = new ArrayList<>();
		for(Type it: clazz.getGenericInterfaces()) {
			if(GroovyObject.class.equals(it)) {
				continue;
			}
			if(it instanceof Class) {
				if(((Class)it).getSimpleName().startsWith("Trait$")) {
					continue;
				}
			}
			interfaceTypes.add(it);
		}
		this.interfaces = interfaceTypes.toArray(new Type[0]);
		properties = new ArrayList<ClassDescriptor.TypedName>();
		namePermutations = new HashMap<>();
		Field[] fields = clazz.getDeclaredFields();
		if(fields!=null){
			for(Field field:fields){
				if(Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())){
					Permutations perm = getPermutations(field.getName());
					perm.propertyType = field.getType();
				}
			}
		}
		methods = new ArrayList<Method>();
		Method[] dms = clazz.getDeclaredMethods();
		for(Method m:dms){
			if(!m.isSynthetic() && !m.isBridge()){
				//check if this is getter/setter
				String mname = m.getName();
				if(m.getParameterTypes().length == 0 && mname.startsWith("is") && mname.length()>2 && Boolean.class.equals(m.getReturnType())){
					String sname = mname.substring(2, 3).toLowerCase().concat(mname.substring(3));
					Permutations perm = getPermutations(sname);
					perm.getterType=Boolean.class;
					perm.getterMethods.add(m);
				}
				else if(m.getParameterTypes().length == 0 && mname.startsWith("get") && mname.length()>3 && (m.getGenericReturnType()!=null)){
					String sname = mname.substring(3, 4).toLowerCase().concat(mname.substring(4));
					Permutations perm = getPermutations(sname);
					perm.getterType=m.getGenericReturnType();
					perm.getterMethods.add(m);
				}	
				else if(m.getParameterTypes().length == 1 && mname.startsWith("set") && mname.length()>3){
					String sname = mname.substring(3, 4).toLowerCase().concat(mname.substring(4));
					Permutations perm = getPermutations(sname);
					perm.setterType = m.getGenericParameterTypes()[0];
					perm.setterMethods.add(m);
				}	
				else{
					methods.add(m);
				}
					
			}
		}
		for(Entry<String, Permutations> entry: namePermutations.entrySet()) {
			String name = entry.getKey();
			Permutations p = entry.getValue();
			if(p.getterType==null) {
				if(p.propertyType==null || p.setterMethods.size()>1) {
					methods.addAll(p.setterMethods);
				}
				else {
					properties.add(new TypedName(name, p.propertyType));
					if(p.setterType !=null && !p.setterType.equals(p.propertyType)) {
						methods.addAll(p.setterMethods);
					}
				}
			}
			else {
				if(p.getterMethods.size()==1 && (p.setterType==null || (p.setterMethods.size()==1 && p.setterType.equals(p.getterType))) ){
					properties.add(new TypedName(name, p.getterType));
				}
				else {
					methods.addAll(p.setterMethods);
					methods.addAll(p.getterMethods);
				}
			}
		}
	}
	
	protected Permutations getPermutations(String name) {
		Permutations perm = namePermutations.get(name);
		if(perm==null) {
			perm = new Permutations();
			namePermutations.put(name,perm);
		}
		return perm;
	}
	
	private class Permutations{
		Type getterType;
		Type setterType;
		Type propertyType;
		List<Method> getterMethods = new ArrayList<>();
		List<Method> setterMethods = new ArrayList<>();
	}
	public Class getType(){
		return type;
	}
	public Type[] getInterfaces(){
		return interfaces;
	}
	public Type getSuperType(){
		return superType;
	}
	
	public List<TypedName> getProperties(){
		return properties;
	}
	
	public List<Method> getMethods(){
		return methods;
	}

	public static class TypedName{
		public final String name;
		public final Type type;
		
		public TypedName(String name, Type type){
			this.name=name;
			this.type=type;
		}
		
		public int hashCode(){
			return name.hashCode()+type.hashCode();
		}
		
		public boolean equals(Object o){
			if(o instanceof TypedName){
				TypedName t=(TypedName) o;
				return t.name.equals(name) && t.type.equals(type);
			}
			return false;
		}
	}
}

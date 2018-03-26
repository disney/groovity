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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import groovy.lang.Closure;

/**
 * ArgsResolver provides argument defaulting, validation and coercion for groovity scripts.  Arguments
 * might come from different sources depending on the ArgsLookup used - for example using the groovity run mojo
 * will trigger interactive arg prompts on the command line, whereas groovity servlet looks up arguments from the 
 * servlet request.
 *   
 * A map of argument definitions is used to define rules for defaulting, validation and coercion; the framework 
 * acquires this map automatically if a script defines a static variable named "args".
 * 
 * Each key in the args map represents the variable name of an expected argument; at the end of resolution 
 * every variable name will be assigned a value in the binding (even if that value is null), or an exception 
 * will be thrown if the argument for that variable cannot be found, validated, coerced or defaulted.
 * 
 * The value of an argument definition can be one of the following:
 * 
 * 1) A literal default value that is used to fill in for missing arguments, or whose Class is used to coerce a presented argument value
 * 2) A Class which is used to coerce a presented argument, throwing an exception if the arg is missing or cannot be converted
 * 3) Null, indicating an optional parameter whose value should default to null
 * 4) A Closure whose return value is used to populate the variable; the presented argument is passed into the closure to allow
 * arbitrary logic for validation and conversion
 * 
 * Classes and default values may specify array or list types, in which case presented arguments are autoboxed into arrays or lists
 * 
 * @author Alex Vigdor
 *
 */
public class ArgsResolver {
	ArgDef[] argDefs;
	private String name;
	
	public ArgsResolver(){
	}
	
	public ArgsResolver(Map<String,Object> argsDef, String name){
		setArgsDef(argsDef);
		this.name=name;
	}
	
	private Object convert(String name, Object in, @SuppressWarnings("rawtypes") Class out){
		try{
			return GroovityObjectConverter.convert(in, out);
		}
		catch(Throwable th){
			throw new ArgsException(name,th,in,out);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void resolve(Map binding, ArgsLookup lookup) {
		for(int a=0;a<argDefs.length;a++){
			ArgDef ad = argDefs[a];
			String varName = ad.name;
			Optional<Object> oo = ad.optional;
			Object pb = lookup.lookup(binding, varName,oo);
			if(oo.isPresent()){
				Object valueSpec = oo.get();
				if(valueSpec instanceof Closure){
					Closure cl = (Closure) valueSpec;
					//we'll overwrite the binding value with whatever the closure returns, passing in any existing value
					binding.put(varName,cl.call(pb));
				}
				else if(valueSpec instanceof Class){
					if(pb==null){
						//missing required arg
						throw new ArgsException(varName);
					}
					//perform a type conversion
					binding.put(varName,convert(varName,pb,(Class)valueSpec));
				}
				else if(pb!=null){
					//perform type conversion on existing binding value
					Object converted = null;
					if(valueSpec!=null){
						if(List.class.isAssignableFrom(valueSpec.getClass())){
							//we convert to an array type using default value as template
							List l = (List)valueSpec;
							Class convertClass = Object.class;
							for(int i=0;i<l.size();i++){
								Object t = l.get(i);
								if(t!=null){
									//this will be our template for the array type
									convertClass = t.getClass();
									break;
								}
							}
							convertClass = Array.newInstance(convertClass, 0).getClass();
							converted = convert(varName,pb,convertClass);
							ArrayList nl = new ArrayList();
							for(int i=0;i<Array.getLength(converted);i++){
								nl.add(Array.get(converted,i));
							}
							converted=nl;
						}
						else{
							converted = convert(varName,pb,valueSpec.getClass());
						}
					}
					if(converted==null){
						converted = valueSpec;
					}
					binding.put(varName,converted);
				}
				else{
					//place default object in binding
					binding.put(varName,valueSpec);
				}
			}
			else if(!binding.containsKey(varName)){
				//place raw unconverted value or null in the binding
				binding.put(varName,pb);
			}
		}
	}
	
	public void setArgsDef(Map<String,Object> argsDef){
		ArrayList<ArgDef> adefs = new ArrayList<ArgDef>();
		for(Entry<String,Object> e: argsDef.entrySet()){
			adefs.add(new ArgDef(e.getKey(), Optional.ofNullable(e.getValue())));
		}
		this.argDefs = adefs.toArray(new ArgDef[0]);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	private class ArgDef{
		private final String name;
		private final Optional<Object> optional;
		
		private ArgDef(final String name, final Optional<Object> optional){
			this.name=name;
			this.optional=optional;
		}
	}
}

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

import java.util.Map;
import java.util.Optional;
/**
 * A base class for retrieving script arguments that pulls them from the binding; subclasses provide
 * argument resolution from system properties or interactive console prompts for command line use.
 * ArgsLookups support constructor and lookup chaining.  Groovity servlet ships with an implementation
 * for pulling args from servlet requests.
 * 
 * @author Alex Vigdor
 *
 */
public class ArgsLookup {
	protected ArgsLookup chained;
	
	public ArgsLookup(){
		
	}
	
	public ArgsLookup(ArgsLookup chained){
		this.chained=chained;
	}
	
	public Object lookup(@SuppressWarnings("rawtypes") Map binding, String name, Optional<Object> defaultValue){
		Object r = doLookup(binding, name,defaultValue);
		if(r!=null){
			return r;
		}
		if(chained!=null){
			return chained.lookup(binding, name, defaultValue);
		}
		return null;
	}
	protected Object doLookup(@SuppressWarnings("rawtypes") Map binding, String name, Optional<Object> defaultValue){
		return binding.get(name);
	}

	public ArgsLookup getChained() {
		return chained;
	}

	public void setChained(ArgsLookup chained) {
		this.chained = chained;
	}
	
	public void chainLast(ArgsLookup chained){
		if(this.chained==null){
			this.chained=chained;
		}
		else{
			this.chained.chainLast(chained);
		}
	}
	
	public void removeLast(){
		if(this.chained!=null){
			if(this.chained.chained==null){
				this.chained=null;
			}
			else{
				this.chained.removeLast();
			}
		}
	}
	
	public static class SystemArgsLookup extends ArgsLookup{
		public SystemArgsLookup(){
			super();
		}
		public SystemArgsLookup(ArgsLookup chained){
			super(chained);
		}
		protected Object doLookup(@SuppressWarnings("rawtypes") Map binding, String name, Optional<Object> defaultValue){
			return System.getProperty(name);
		}
	}
	public static class ConsoleArgsLookup extends ArgsLookup{
		public ConsoleArgsLookup(){
			super();
		}
		public ConsoleArgsLookup(ArgsLookup chained){
			super(chained);
		}
		protected Object doLookup(@SuppressWarnings("rawtypes") Map binding, String name, Optional<Object> defaultValue){
			if(System.console()==null){
				return null;
			}
			String def = "";
			boolean password = false;
			if(defaultValue.isPresent()){
				Object d = defaultValue.get();
				if(char[].class.equals(d)){
					password = true;
				}
				def = d.toString();
			}
			if(password){
				char[] pc = System.console().readPassword("Enter a value for '"+name+"' : ");
				if(pc!=null && pc.length>0){
					return pc;
				}
				return null;
			}
			String pa = System.console().readLine("Enter a value for '"+name+"' ["+def+"] : ");
			if(pa!=null && pa.length()>0){
				return pa;
			}
			return null;
		}
	}
}

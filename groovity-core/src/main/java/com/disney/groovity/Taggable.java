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

import groovy.lang.Closure;

import java.util.Map;
import com.disney.groovity.util.ScriptHelper;

/**
 * API for a global function that can be used in groovity script code OR template code, e.g.
 * <p> 
 * <pre>
 * load(path:"/some/path")
 * 
 * &lt;~ &lt;g:load path="/some/path"/&gt; ~&gt;
 * </pre>
 * The name of the tag is taken by converting the first character of the classname to lower case
 * <p>
 * This interface is automatically added to script classes that have the @Tag annotation and a tag() method
 * 
 * @author Alex Vigdor
 *
 */
public interface Taggable extends GroovityConstants {
	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) throws Exception;
	/**
	 * This optional method allows Groovity to provide Taggables with access to its various components like
	 * scriptHelper, httpClient, etc.
	 * @param groovity
	 */
	public default void setGroovity(Groovity groovity){}
	/**
	 * tags that require one-time setup logic can override the init method, which will be called
	 * after the groovity has been set
	 */
	public default void init(){}
	/**
	 * tags that need to release or clean up resources may override the destroy method
	 */
	public default void destroy(){}
	/**
	 * Resolve an attribute, which involves looking it up by name, executing it and getting the return
	 * value if the attribute value is a closure
	 * 
	 * @param attributes
	 * @param name
	 * @return
	 */
	public default Object resolve(@SuppressWarnings("rawtypes") Map attributes, String name){
		return resolve(attributes, name, Object.class);
	}
	/**
	 * Resolve an attribute, which involves looking it up by name, executing it and getting the return
	 * value if the attribute value is a closure, and transforming the value to the specified type
	 * 
	 * @param attributes
	 * @param name
	 * @param type
	 * @return
	 */
	public default <T> T resolve(@SuppressWarnings("rawtypes") Map attributes, String name, Class<T> type){
		Object o = attributes.get(name);
		return resolve(o,type);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public default <T> T resolve(Object o, Class<T> type){
		if(o instanceof Closure && !Closure.class.isAssignableFrom(type)){
			o = ((Closure)o).call();
		}
		return (T) GroovityObjectConverter.convert(o, type);
	}
	
	public default ScriptHelper getScriptHelper(@SuppressWarnings("rawtypes") Closure body){
		return ScriptHelper.get(body);
	}
	
	public default void bind(@SuppressWarnings("rawtypes") Closure body, String variableName, Object value){
		getScriptHelper(body).getBinding().setVariable(variableName, value);
	}
	
	public default void bind(ScriptHelper scriptHelper, String variableName, Object value){
		scriptHelper.getBinding().setVariable(variableName, value);
	}
	
	public default Object get(@SuppressWarnings("rawtypes") Closure body, String variableName){
		return getScriptHelper(body).getBinding().getVariables().get(variableName);
	}
	public default Object get(ScriptHelper scriptHelper, String variableName){
		return scriptHelper.getBinding().getVariables().get(variableName);
	}
	public default void unbind(@SuppressWarnings("rawtypes") Closure body, String variableName){
		getScriptHelper(body).getBinding().getVariables().remove(variableName);
	}
	public default void unbind(ScriptHelper scriptHelper, String variableName){
		scriptHelper.getBinding().getVariables().remove(variableName);
	}
}

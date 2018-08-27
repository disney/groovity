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

import java.lang.reflect.Modifier;
import groovy.lang.Script;
/**
 * Interface for components that would like to be able to monitor all the active scripts in a groovity,
 * e.g. for introspection or registration of scripts with external frameworks
 *
 * @author Alex Vigdor
 */
public interface GroovityObserver {
	public void scriptStart(Groovity groovity, String scriptPath, Class<Script> scriptClass);
	public void scriptDestroy(Groovity groovity, String scriptPath, Class<Script> scriptClass);
	public void destroy(Groovity groovity);

	/**
	 * Sub-interface for observers that only want to react to scripts that have a non-null value in a static field with a given name
	 */
	public interface Field extends GroovityObserver{
		public String getField();
		public void scriptStart(Groovity groovity, String scriptPath, Class<Script> scriptClass, Object fieldValue);
		public void scriptDestroy(Groovity groovity, String scriptPath, Class<Script> scriptClass, Object fieldValue);
		@Override
		public default void scriptStart(Groovity groovity, String scriptPath, Class<Script> scriptClass) {
			Object value = null;
			try {
				java.lang.reflect.Field outputField = scriptClass.getDeclaredField(getField());
				if(Modifier.isStatic(outputField.getModifiers())){
					outputField.setAccessible(true);
					value = outputField.get(null);
				}
			}
			catch(NoSuchFieldException | IllegalAccessException e){
			}
			if(value!=null) {
				scriptStart(groovity, scriptPath, scriptClass, value);
			}
		}
		@Override
		public default void scriptDestroy(Groovity groovity, String scriptPath, Class<Script> scriptClass) {
			Object value = null;
			try {
				java.lang.reflect.Field outputField = scriptClass.getDeclaredField(getField());
				if(Modifier.isStatic(outputField.getModifiers())){
					outputField.setAccessible(true);
					value = outputField.get(null);
				}
			}
			catch(NoSuchFieldException | IllegalAccessException e){
			}
			if(value!=null) {
				scriptDestroy(groovity, scriptPath, scriptClass, value);
			}
		}
	}
}

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

import java.util.function.BiConsumer;


/**
 * Functional interface for a ModelConsumer; a model consumer gets passed to a Model's
 * each() function and is called back with each field in the model.  A consumer can also be 
 * used to receive the output of a ModelFilter in a map() operation
 * 
 * @author Alex Vigdor
 *
 */
@FunctionalInterface
public interface ModelConsumer extends BiConsumer<Object,Object>{
	public void call(String key, Object value);
	
	default void accept(Object key, Object value) {
		call(key.toString(),value);
	}
	
	default void visitObjectFields(Object obj) {
		if(obj instanceof Model) {
			((Model)obj).each( this );
			return;
		}
		Model.each(obj, this);
	}
}

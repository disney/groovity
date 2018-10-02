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
package com.disney.groovity.data.service;

import com.disney.groovity.data.Pointer;
import com.disney.groovity.data.Referent;
import com.disney.groovity.model.ModelFilter;
import com.disney.groovity.model.ModelFilter.ObjectMatcher;
import com.disney.groovity.model.ModelVisitor;
import com.disney.groovity.model.ModelWalker;

import groovy.lang.Script;
/**
 * Offer Pointer expansion as a ModelFilter
 * 
 * @author Alex Vigdor
 *
 */
public class DataFilter{
	
	public static ModelFilter expand(Script factory, String... objectPaths) throws Exception {
		return new ObjectMatcher( objectPaths ) {
			@Override
			protected void filteredObject(ModelWalker walker, boolean matched, Object value, ModelVisitor consumer) throws Exception {
				if(matched) {
					if(value instanceof Pointer) {
						Pointer pointer = (Pointer) value;
						boolean known = (boolean) factory.invokeMethod("isKnownType", pointer.getType());
						if(!known) {
							Referent cur = walker.getContextObject(Referent.class);
							if(cur != null) {
								Pointer parent = cur.getPointer();
								//check for lifecycle suffix
								String t = parent.getType();
								int u = t.indexOf("_");
								if(u>0) {
									String nt = pointer.getType().concat(t.substring(u));
									known = (boolean) factory.invokeMethod("isKnownType", nt);
									if(known) {
										pointer = new Pointer(nt,pointer.getId());
									}
								}
							}
						}
						if(known) {
							value = factory.invokeMethod("call", pointer);
						}
					}
				}
				consumer.visitObject(value);
			}
		};
	}

}

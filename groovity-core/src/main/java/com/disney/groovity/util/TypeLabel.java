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
package com.disney.groovity.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
/**
 * Utility for constructing labels for types including generic parameters
 *
 * @author Alex Vigdor
 */
public class TypeLabel {

	public static String get(Type type) {
		StringBuilder builder = new StringBuilder();
		build(type, builder);
		return builder.toString();
	}

	public static void build(Type type, StringBuilder builder){
		if(type instanceof ParameterizedType){
			ParameterizedType pType = ((ParameterizedType)type);
			build(pType.getRawType(), builder);
			Type[] args = pType.getActualTypeArguments();
			if(args.length>0){
				builder.append("<");
				for(int i=0;i<args.length;i++){
					if(i>0){
						builder.append(", ");
					}
					build(args[i], builder);
				}
				builder.append(">");
			}
		}
		else if(type instanceof Class){
			@SuppressWarnings("rawtypes")
			Class cType = ((Class)type);
			builder.append(cType.getSimpleName());
		}
	}

}

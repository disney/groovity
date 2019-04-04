/*******************************************************************************
 * © 2019 Disney | ABC Television Group
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
package com.disney.groovity.tags;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.disney.groovity.ArgsException;
import com.disney.groovity.GroovityObjectConverter;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Tag;

import groovy.lang.Closure;
/**
 * For each named attribute, if there is a value already bound with a given name, convert it to match the class of the prebound value if not-null; 
 * If there is no bound value, apply the prebound value to the binding as a default value, unless it is a class value, in which case an exception is thrown as the prebind class condition failed
 * <p>
 * prebind();
 * 
 *	<p>Sample
 *	<pre>
 *	prebind(debug: false, timeout: 60, flags: ['green','blue'], transaction: null, out: Writer.class)
 *	</pre>	
 * @author Alex Vigdor
 */
@Tag(
	info = "For each named attribute, if there is a value already bound with a given name, convert it to match the class of the prebound value if not-null; If there is no bound value, apply the prebound value to the binding as a default value, unless it is a class value, in which case an exception is thrown as the prebind class condition failed",
	sample="prebind(debug: false, timeout: 60, flags: ['green','blue'], transaction: null, out: Writer.class)"
)
public class Prebind implements Taggable {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object tag(Map attributes, Closure body) throws Exception {
		Map variables = getScriptHelper(body).getBinding().getVariables();
		attributes.forEach((k,v)->{
			Object bv = variables.get(k);
			if(v instanceof Closure) {
				Closure c = (Closure) v;
				v = c.call(bv);
			}
			if(v instanceof Class) {
				if(bv == null) {
					throw new ArgsException(k.toString());
				}
				v = convert(k.toString(), bv, (Class) v);
			}
			else if(bv!=null && v!=null){
				//perform type conversion on existing binding value
				Object converted = null;
				if(List.class.isAssignableFrom(v.getClass())){
					//we convert to an array type using default value as template
					List l = (List)v;
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
					converted = convert(k.toString(),bv,convertClass);
					ArrayList nl = new ArrayList();
					for(int i=0;i<Array.getLength(converted);i++){
						nl.add(Array.get(converted,i));
					}
					converted=nl;
				}
				else{
					converted = convert(k.toString(),bv,v.getClass());
				}
				if(converted!=null){
					v=converted;
				}
			}
			else if(v == null && bv != null) {
				v = bv;
			}
			variables.put(k, v);
		});
		return body.call();
	}

	private Object convert(String name, Object in, @SuppressWarnings("rawtypes") Class out){
		try{
			return GroovityObjectConverter.convert(in, out);
		}
		catch(Throwable th){
			throw new ArgsException(name,th,in,out);
		}
	}
}

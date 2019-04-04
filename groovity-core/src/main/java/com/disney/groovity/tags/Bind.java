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

import java.util.Map;

import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Tag;

import groovy.lang.Closure;
/**
 * Apply all attributes to the binding as variables
 * <p>
 * bind();
 *	
 *	<p>Sample
 *	<pre>
 *	bind( a: 'b', c: 123)
 *	</pre>	
 * @author Alex Vigdor
 */
@Tag(
	info = "Apply all attributes to the binding as variables",
	sample="bind( a: 'b', c: 123)"
)
public class Bind implements Taggable {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object tag(Map attributes, Closure body) throws Exception {
		Map variables = getScriptHelper(body).getBinding().getVariables();
		attributes.forEach((k,v)->{
			if(v instanceof Closure) {
				v = ((Closure)v).call();
			}
			variables.put(k, v);
		});
		return body.call();
	}

}

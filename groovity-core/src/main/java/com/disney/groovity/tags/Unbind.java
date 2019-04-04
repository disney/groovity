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

import java.util.LinkedHashMap;
import java.util.Map;

import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Tag;

import groovy.lang.Binding;
import groovy.lang.Closure;
/**
 * Apply all attributes to the binding as variables while calling the body; restore binding completely to its prior state
 * once the tag is closed
 * <p>
 * unbind(<ul>
 * </ul>{
 *	<blockquote>// script that can access attributes as bound variables</blockquote>
 * 	});
 * 
 * <p><b>returns</b> the return value of the tag body
 *	
 *	<p>Sample
 *	<pre>
 *	answer = unbind( a: 123, b: 456) { c = a * b }
 *	</pre>	
 * @author Alex Vigdor
 */
@Tag(
	info = "Apply all attributes to the binding as variables while calling the body; restore binding completely to its prior state once the tag is closed",
	sample="answer = unbind( a: 123, b: 456) { c = a * b }",
	returns="the return value of the tag body",
	body="script that can access attributes as bound variables"
)
public class Unbind implements Taggable {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object tag(Map attributes, Closure body) throws Exception {
		// Whatever happens in this body, stays in this body
		Binding binding = getScriptHelper(body).getBinding();
		Map variables = binding.getVariables();
		LinkedHashMap snapshot = new LinkedHashMap<>(variables);
		try {
			attributes.forEach((k,v)->{
				if(v instanceof Closure) {
					v = ((Closure)v).call();
				}
				variables.put(k, v);
			});
			return body.call();
		}
		finally {
			variables.clear();
			variables.putAll(snapshot);
		}
	}

}

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

import groovy.lang.Closure;
/**
 * Apply all attributes to the binding as variables while calling the body; restore the prior value or unbound state
 * of all attributes once the tag is closed
 * <p>
 * rebind(<ul>
 * </ul>{
 *	<blockquote>// script that can access attributes as bound variables</blockquote>
 * 	});
 * 
 * <p><b>returns</b> the return value of the tag body
 *	
 *	<p>Sample
 *	<pre>
 *	answer = rebind( a: 123, b: 456) { a * b }
 *	</pre>	
 * @author Alex Vigdor
 */
@Tag(
	info = "Apply all attributes to the binding as variables while calling the body; restore the prior value or unbound state of all attributes once the tag is closed",
	sample="answer = rebind( a: 123, b: 456) { a * b }",
	returns="the return value of the tag body",
	body="script that can access attributes as bound variables"
)
public class Rebind implements Taggable {
	static final Object NOT_BOUND = new Object();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object tag(Map attributes, Closure body) throws Exception {
		Map holder = new LinkedHashMap();
		Map variables = getScriptHelper(body).getBinding().getVariables();
		attributes.forEach((k,v)->{
			if(variables.containsKey(k)) {
				holder.put(k, variables.get(k));
			}
			else {
				holder.put(k, NOT_BOUND);
			}
			if(v instanceof Closure) {
				v = ((Closure)v).call();
			}
			variables.put(k, v);
		});
		try {
			return body.call();
		}
		finally {
			holder.forEach((k, v)->{
				if(v == NOT_BOUND) {
					variables.remove(k);
				}
				else {
					variables.put(k, v);
				}
			});
		}
	}

}

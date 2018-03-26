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
package com.disney.groovity.tags;

import groovy.lang.Binding;
import groovy.lang.Closure;

import java.util.Map;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
/**
 * The tag for removing a variable from the binding
 * <p>
 * remove( <ul>	
 *	<li><b>var</b>: 
 *	the variable name to be removed from the binding,</li>
 *	</ul>{});
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;g:remove var=&quot;myVar&quot;/&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
		info = "The tag for removing a variable from the binding",
		sample="<g:remove var=\"myVar\"/>",
		attrs = { 
			@Attr(
					name = GroovityConstants.VAR, 
					info="the variable name to be removed from the binding",
					required = true
			) 
		} 
	)
public class Remove implements Taggable {
	
	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) {
		Object in = attributes.get(VAR);
		if(in != null) {			
			if(!(in instanceof String)){
				throw new RuntimeException("Remove tag's attribute var has to be a string");
			}
			Binding binding = getScriptHelper(body).getBinding();
			return binding.getVariables().remove((String)in);
		}
		else{
			throw new RuntimeException("Remove tag requires attribute var.");
		}
	}

}

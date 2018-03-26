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

import groovy.lang.Closure;
import groovy.lang.Writable;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.Map;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
/**
 * Sets the value of a variable in the binding
 * <p>
 * set( <ul>	
 *	<li><i>var</i>: 
 *	the name of the variable to bind the value,</li>	
 *	<li><i>value</i>: 
 *	an expression whose result is bound to "var",</li>
 *	</ul>{
 *	<blockquote>// if value attribute is not specified, the body is used to generate the value of the bound variable</blockquote>
 * 	});
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;~ &lt;g:set var=&quot;foo&quot; value=&quot;${3*3}&quot;/&gt; ~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
		info = "Sets the value of a variable in the binding",
		body = "if value attribute is not specified, the body is used to generate the value of the bound variable",
		sample="<~ <g:set var=\"foo\" value=\"${3*3}\"/> ~>",
		attrs = { 
			@Attr(
					name = GroovityConstants.VAR, 
					info="the name of the variable to bind the value",
					required = false
			),
			@Attr(
					name = GroovityConstants.VALUE, 
					info="an expression whose result is bound to \"var\"",
					required = false
			)			
		} 
	)
public class Set implements Taggable {

	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) throws IOException{
		Object var = attributes.get(VAR);
		Object value = null;
		if(attributes.containsKey(VALUE)){
			value = resolve(attributes,VALUE);
		}
		else{
			//use closure
			Object oldOut = get(body,OUT);
			CharArrayWriter writer = new CharArrayWriter();
			bind(body,OUT, writer);
			try{
				Object rval = body.call();
				if(writer.size()==0) {
					if(rval instanceof Writable) {
						((Writable)rval).writeTo(writer);
					}
					else {
						value = rval;
					}
				}
			}
			finally{
				bind(body,OUT, oldOut);
			}
			if(value==null) {
				value = writer.toString();
			}
		}
		if(var!=null){
			bind(body,var.toString(), value);
		}
		return value;
	}

}

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

import java.util.Map;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
/**
 * The tag catches any Throwable that occurs in its body and optionally exposes it. Used for error handling.
 * <p>
 * catch( <ul>	
 *	<li><i>var</i>: 
 *	the variable name to hold the Throwable object if thrown in the body,</li>
 *	</ul>{
 *	<blockquote>// Code to execute</blockquote>
 * 	});
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;~&lt;g:catch var=&quot;catchException&quot;&gt;
 *		&lt;g:set var=&quot;myVar&quot; value=&quot;${3/0}&quot;/&gt;
 *	&lt;/g:catch&gt;
 *	&lt;g:if test = &quot;${catchException != null}&quot;&gt;
 *		${catchException}${catchException.message}
 *	&lt;/g:if&gt;~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
		info = "The tag catches any Throwable that occurs in its body and optionally exposes it. Used for error handling.",
		body = "Code to execute",
		sample="<~<g:catch var=\"catchException\">\n"+
				   "\t<g:set var=\"myVar\" value=\"${3/0}\"/>\n"+
				"</g:catch>\n"+
				"<g:if test = \"${catchException != null}\">\n\t${catchException}${catchException.message}\n</g:if>~>",
		attrs = { 
			@Attr(
					name = GroovityConstants.VAR, 
					info="the variable name to hold the Throwable object if thrown in the body",
					required = false
			) 
		} 
	)
public class Catch implements Taggable {

	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) {
		Exception ex = null;
		try{
			body.call();
		}catch(Exception e){	
			ex=e;
		}	
		Object in = resolve(attributes,VAR);
		if(in != null) {	
			bind(body,in.toString(),ex);	
		}			
		return ex;
	}

}

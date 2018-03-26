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

import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
/**
 * The tag renders its body unless test condition is true. Does the opposite of what the if tag does.
 * <p>
 * unless( <ul>	
 *	<li><b>test</b>: 
 *	an expression that evaluates to true or false,</li>
 *	</ul>{
 *	<blockquote>// The body to execute when the test expression evaluates to false</blockquote>
 * 	});
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;~ &lt;g:unless test=&quot;${num % 2 == 0}&quot;&gt;
 *		${num},
 *	&lt;/g:unless&gt; ~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
		info = "The tag renders its body unless test condition is true. Does the opposite of what the if tag does.",
		body = "The body to execute when the test expression evaluates to false",
		sample="<~ <g:unless test=\"${num % 2 == 0}\">\n" + 
				"\t${num},\n" + 
				"</g:unless> ~>",
		attrs = { 
			@Attr(
					name = "test", 
					info="an expression that evaluates to true or false",
					required = true
			) 
		} 
	)
public class Unless implements Taggable {

	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) {
		Object test = resolve(attributes,"test");
		if(!(test instanceof Boolean)){
			test = DefaultTypeTransformation.castToBoolean(test);
		}
		if(!((Boolean)test).booleanValue()){
			return body.call();
		}
		return null;
	}

}

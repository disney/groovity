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
 * Logical if tag controls whether the body expression is executed
 * <p>
 * if( <ul>	
 *	<li><b>test</b>: 
 *	an expression that evaluates to true or false,</li>
 *	</ul>{
 *	<blockquote>// Code to execute when the test expression evaluates to true</blockquote>
 * 	});
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;g:if test=&quot;${i % 2 == 0}&quot;&gt;
 *		${i},
 *	&lt;/g:if&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */ 
@Tag(
	info = "Logical if tag controls whether the body expression is executed",
	body = "Code to execute when the test expression evaluates to true",
	sample="<g:if test=\"${i % 2 == 0}\">\n" + 
			"\t${i},\n" + 
			"</g:if>",
	attrs = { 
		@Attr(
				name = "test", 
				info="an expression that evaluates to true or false",
				required = true
		) 
	} 
)
public class If implements Taggable{

	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) {
		Object test = resolve(attributes,"test");
		if(!(test instanceof Boolean)){
			test = DefaultTypeTransformation.castToBoolean(test);
		}
		try{
			if(((Boolean)test).booleanValue()){
				return body.call();
			}
		}
		finally{
			bind(body,"lastIfTagEvaluationResult", test);
		}
		return null;
	}

}

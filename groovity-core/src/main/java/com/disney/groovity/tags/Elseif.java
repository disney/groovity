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
 * Logical elseif tag controls execution flow; must be preceded by If tag
 * <p>
 * elseif( <ul>	
 *	<li><b>test</b>: 
 *	an expression that evaluates to true or false,</li>
 *	</ul>{
 *	<blockquote>// Code to execute when the test expression evaluates to true</blockquote>
 * 	});
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;~&lt;g:each var=&quot;i&quot; in=&quot;${0..10}&quot;&gt;
 *		&lt;g:if test=&quot;${i % 2 == 0}&quot;&gt;
 *			${i}.
 *		&lt;/g:if&gt;
 *		&lt;g:elseif test=&quot;${i % 3 == 0}&quot;&gt;
 *			${i}:
 *		&lt;/g:elseif&gt;
 *		&lt;g:else&gt;
 *			${i},
 *		&lt;/g:else&gt;
 *	&lt;/g:each&gt;~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
		info = "Logical elseif tag controls execution flow; must be preceded by If tag",
		body = "Code to execute when the test expression evaluates to true",
		sample=	"<~<g:each var=\"i\" in=\"${0..10}\">\n" + 
				"	<g:if test=\"${i % 2 == 0}\">\n" + 
				"		${i}.\n" + 
				"	</g:if>\n" + 
				"	<g:elseif test=\"${i % 3 == 0}\">\n" + 
				"		${i}:\n" + 
				"	</g:elseif>\n" + 
				"	<g:else>\n" + 
				"		${i},\n" + 
				"	</g:else>\n" + 
				"</g:each>~>",
		attrs = { 
			@Attr(
					name = "test", 
					info="an expression that evaluates to true or false",
					required = true
			) 
		} 
	)
public class Elseif implements Taggable{

	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) {
		Boolean wasIfTrue = null;
		try{
			wasIfTrue = (Boolean) get(body,"lastIfTagEvaluationResult");
		}
		catch(Exception e){}
		if(wasIfTrue==null){
			throw new RuntimeException("elseif{} must be preceded by if{}");
		}
		if(!(wasIfTrue).booleanValue()){
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
		}
		return null;
	}

}

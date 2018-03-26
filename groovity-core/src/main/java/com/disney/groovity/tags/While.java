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

import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
/**
 * Executes a condition in a loop until the condition returns false
 * <p>
 * while( <ul>	
 *	<li><b>test</b>: 
 *	an expression that evaluates to true or false,</li>	
 *	<li><i>max</i>: 
 *	maximum number of iterations,</li>
 *	</ul>{
 *	<blockquote>// the body to execute per loop iteration</blockquote>
 * 	});
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;~ &lt;g:set var=&quot;i&quot; value=&quot;${0}&quot; /&gt;
 *	&lt;g:while test=&quot;${i++ &lt; 5}&quot;&gt;
 *		${i},
 *	&lt;/g:while&gt; ~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */ 
@Tag(
	info = "Executes a condition in a loop until the condition returns false",
	body = "the body to execute per loop iteration",
	sample="<~ " + 
			"<g:set var=\"i\" value=\"${0}\" />\n" + 
			"<g:while test=\"${i++ < 5}\">\n" + 
			"	${i},\n" + 
			"</g:while> ~>",
	attrs = { 
		@Attr(
				name = "test", 
				info="an expression that evaluates to true or false",
				required = true
		), 
		@Attr(
				name = "max", 
				info="maximum number of iterations",
				required = false
		),		
	} 
)
public class While implements Taggable {

	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) {
		Object test = attributes.get("test");
		if(!(test instanceof Closure)){
			throw new RuntimeException("While tag requires boolean expression");
		}
		Closure tc = (Closure) test;
		Object val = tc.call();
		if(!(val instanceof Boolean)){
			throw new RuntimeException("While tag requires expression to return boolean, not "+val);
		}
		Object max = resolve(attributes,"max");
		int imax = -1;
		if(max!=null){
			if(max instanceof Number){
				imax = ((Number)max).intValue();
			}
			else{
				imax = Integer.parseInt(max.toString());
			}
		}
		int count = 0;
		while(((Boolean)val).booleanValue()){
			if(count==imax){
				break;
			}
			body.call();
			val = tc.call();
			if(!(val instanceof Boolean)){
				throw new RuntimeException("While tag requires expression to return boolean, not "+val);
			}
			count++;
		}
		return null;
	}

}

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

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.util.ScriptHelper;
import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
/**
 * The tag used to iterate over each element of the specified object.
 * <p>
 * each( <ul>	
 *	<li><b>in</b>: 
 *	the object to iterate over,</li>	
 *	<li><i>max</i>: 
 *	the maximum number of iterations,</li>	
 *	<li><i>var</i>: 
 *	the name for what is otherwise 'it' in the loop context,</li>	
 *	<li><i>pos</i>: 
 *	the name of a variable used to store the current position in the loop (starting from 0),</li>
 *	</ul>{
 *	<blockquote>// Code to execute per loop iteration.</blockquote>
 * 	});
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;~&lt;g:set var=&quot;max&quot; value=&quot;7&quot;/&gt;
 *	&lt;g:each in=&quot;${0..10}&quot; max=&quot;${max}&quot;&gt;
 *		${it},
 *	&lt;/g:each&gt;~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
		info = "The tag used to iterate over each element of the specified object.",
		body = "Code to execute per loop iteration.",
		sample="<~<g:set var=\"max\" value=\"7\"/>\n" + 
				"<g:each in=\"${0..10}\" max=\"${max}\">\n"
				+ "\t${it},\n"
				+ "</g:each>~>",
				
		attrs = {
			@Attr(
					name = "in", 
					info="the object to iterate over",
					required = true
			),
			@Attr(
					name = "max", 
					info="the maximum number of iterations",
					required = false
			),			
			@Attr(
					name = GroovityConstants.VAR, 
					info="the name for what is otherwise \'it\' in the loop context",
					required = false
			),
			@Attr(
					name = "pos", 
					info="the name of a variable used to store the current position in the loop (starting from 0)",
					required = false
			) 
		} 
	)
public class Each implements Taggable {

	@SuppressWarnings({"rawtypes","unchecked"})
	public Object tag(Map attributes, Closure body) {
		Object in = resolve(attributes,"in");
		if(in==null){
			//for null let's be graceful and act like a zero-length iteration, saves coding null checks
			return null;
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
		Object pos = resolve(attributes,"pos");
		if(in instanceof Map){
			in = ((Map)in).entrySet();
		}
		if(in instanceof Enumeration){
			in = Collections.list((Enumeration)in);
		}
		if(in.getClass().isArray()){
			in = Arrays.asList((Object[])in);
		}
		if(!(in instanceof Iterable)){
			throw new RuntimeException("Each tag requires an iterable 'in' attribute, not "+in+" of type "+in.getClass().getName());
		}
		Object var = attributes.get(VAR);
		String varName = var ==null?null:var.toString();
		String posName = pos==null?null:pos.toString();
		Object oldVar = null;
		Object oldPos = null;
		ScriptHelper context = getScriptHelper(body);
		if(varName!=null){
			oldVar = get(context, varName);
		}
		if(posName!=null){
			oldPos = get(context, posName);
		}
		int count = 0;
		for(Object it: ((Iterable)in)){
			if(count==imax){
				break;
			}
			if(varName!=null){
				bind(context,varName,it);
			}
			if(posName!=null){
				bind(context,posName, count);
			}
			body.call(it);
			count++;
		}
		if(oldVar!=null){
			bind(context,varName,oldVar);
		}
		else if(varName!=null){
			unbind(context,varName);
		}
		if(oldPos!=null){
			bind(context,posName,oldPos);
		}
		else if(posName!=null){
			unbind(context,posName);
		}
		return null;
	}

}

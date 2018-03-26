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

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
/**
 * Create a closure in the binding that can be called later, e.g. a re-useable page fragment or function
 * <p>
 * closure( <ul>	
 *	<li><b>var</b>: 
 *	the name of the variable to bind the closure,</li>	
 *	<li><i>args</i>: 
 *	one or a list of argument names that can be passed to the closure and isolated to the closure's scope ,</li>
 *	</ul>{
 *	<blockquote>// the code or template that will execute when the closure is called</blockquote>
 * 	});
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;~&lt;g:closure var=&quot;hello&quot; args=&quot;${['first','last']}&quot;&gt;
 *		Hello ${first} ${last}!
 *	&lt;/g:closure&gt;
 *	${hello('John','Doe')}~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
		info = "Create a closure in the binding that can be called later, e.g. a re-useable page fragment or function",
		body = "the code or template that will execute when the closure is called",
		sample="<~<g:closure var=\"hello\" args=\"${['first','last']}\">\n" + 
				"\tHello ${first} ${last}!\n" + 
				"</g:closure>\n" + 
				"${hello('John','Doe')}~>",
		attrs = { 
			@Attr(
					name = GroovityConstants.VAR, 
					info="the name of the variable to bind the closure",
					required = true
			),
			@Attr(
					name = "args", 
					info="one or a list of argument names that can be passed to the closure and isolated to the closure's scope ",
					required = false
			)			
		} 
	)
public class Closure implements Taggable{

	@SuppressWarnings({"rawtypes","unchecked"})
	public Object tag(Map attributes, final groovy.lang.Closure body) throws Exception {
		Object var = attributes.get(VAR);
		Object args = attributes.get(ARGS);
		if(args instanceof groovy.lang.Closure){
			args = ((groovy.lang.Closure)args).call();
		}
		final List argNames;
		if(args==null){
			argNames = new ArrayList<String>();
		}
		else if(args instanceof List){
			argNames = (List)args;
		}
		else{
			argNames = Arrays.asList(args);
		}
		groovy.lang.Closure theClosure = new groovy.lang.Closure(body.getOwner(),body.getDelegate()){
			public Object doCall(Object[] args){
				groovy.lang.Closure myBody = (groovy.lang.Closure) body.clone();
				myBody.setDelegate(new GroovyObjectSupport() {
					HashMap argMap = new HashMap();
					public void setProperty(String name, Object value){
						if(argNames.contains(name)){
							argMap.put(name, value);
						}
						else{
							throw new MissingPropertyException(name);
						}
					}
					public Object getProperty(String name){
						if(argNames.contains(name)){
							return argMap.get(name);
						}
						else{
							throw new MissingPropertyException(name);
						}
					}
				});
				myBody.setResolveStrategy(DELEGATE_FIRST);
				//here we assign runtime args as properties of the closure
				for(int i=0;i<argNames.size();i++){
					Object arg = null;
					if(args!=null && i<args.length){
						arg = args[i];
					}
					//System.out.println("Setting property "+argNames.get(i).toString()+" to "+arg);
					
					myBody.setProperty(argNames.get(i).toString(), arg);
				}
				return myBody.call();
			}
		};
		if(var!=null){
			bind(body,var.toString(),theClosure);
		}
		return theClosure;
	}

}

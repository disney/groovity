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
import groovy.lang.Script;

import java.util.Map;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
/**
 * Creates a groovy script object from a given path to the script
 * <p>
 * load( <ul>	
 *	<li><b>path</b>: 
 *	the path to a groovy script,</li>	
 *	<li><i>var</i>: 
 *	the name of the variable to bind the script object,</li>
 *	</ul>{});
 *	
 *	<p><b>returns</b> The script object
 *	
 *	<p>Sample
 *	<pre>
 *	myLib = load(path:&quot;/library&quot;);
 *	</pre>	
 * 
 * @author Alex Vigdor
 */ 
@Tag(
		info = "Creates a groovy script object from a given path to the script",
		sample="myLib = load(path:\"/library\");",
		returns="The script object",
		attrs = { 
			@Attr(
					name = "path", 
					info="the path to a groovy script",
					required = true
			),
			@Attr(
					name = GroovityConstants.VAR, 
					info="the name of the variable to bind the script object",
					required = false
			) 			
		} 
	)
public class Load implements Taggable {

	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) {
		String path = resolve(attributes, "path", String.class);
		if(path==null){
			throw new RuntimeException("Load tag requires path attribute");
		}
		String var = resolve(attributes, VAR, String.class);
		try {
			Script script = getScriptHelper(body).load(path);
			if(var!=null){
				bind(body,var, script);
			}
			return script;
		} catch (Exception e) {
			throw new RuntimeException("Error in load "+path,e);
		}
	}

}

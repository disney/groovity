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

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.util.ScriptHelper;
/**
 * Execute a script located at a given path.  The script will share the binding of the calling script.
 * <p>
 * run( <ul>	
 *	<li><b>path</b>: 
 *	the path to a .grvt file,</li>	
 *	<li><i>var</i>: 
 *	the name of the variable to bind the return value of the run script,</li>
 *	</ul>{});
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;g:run path=&quot;/navigation&quot; /&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
		info = "Execute a script located at a given path.  The script will share the binding of the calling script.",
		sample="<g:run path=\"/navigation\" />",
		attrs = { 
			@Attr(
					name = "path", 
					info="the path to a .grvt file",
					required = true
			),
			@Attr(
					name = GroovityConstants.VAR, 
					info="the name of the variable to bind the return value of the run script",
					required = false
			) 
		} 
	)
public class Run implements Taggable {

	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) throws Exception {
		Object path = attributes.get("path");
		if(path==null){
			throw new RuntimeException("Run tag requires path attribute");
		}
		Object var = attributes.get(VAR);
		String p = path.toString();
		Object rval = getScriptHelper(body).run(p);
		if(var!=null){
			bind(body,var.toString(), rval);
		}
		return rval;
	}

}

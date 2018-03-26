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

import org.apache.http.client.utils.URIBuilder;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;

/**
 * Used to add parameters to http calls or URIs, automatically performing URL encoding on the name and value
 * <p>
 * param( <ul>	
 *	<li><b>name</b>: 
 *	the name of the url parameter to add,</li>	
 *	<li><b>value</b>: 
 *	the value of the url parameter,</li>	
 *	<li><i>replace</i>: 
 *	true or false; whether to replace existing parameters with the same name, defaults to false,</li>
 *	</ul>{});
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;~ &lt;g:http url=&quot;http://localhost:28197/somepath&quot;&gt;
 *		&lt;g:param name=&quot;inverse&quot; value=&quot;true&quot;/&gt;
 *	&lt;/g:http&gt; ~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
		info = "Used to add parameters to http calls or URIs, automatically performing URL encoding on the name and value",
		sample="<~ <g:http url=\"http://localhost:28197/somepath\">\n" + 
				"\t<g:param name=\"inverse\" value=\"true\"/>\n" + 
				"</g:http> ~>",
		attrs = { 
			@Attr(
					name = "name", 
					info="the name of the url parameter to add",
					required = true
			),
			@Attr(
					name = GroovityConstants.VALUE, 
					info="the value of the url parameter",
					required = true
			), 
			@Attr(
					name="replace",
					info="true or false; whether to replace existing parameters with the same name, defaults to false",
					required = false
			)
			
		} 
	)
public class Param implements Taggable{

	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) {
		Object name = resolve(attributes,NAME);
		Object value = resolve(attributes,VALUE);
		if(name==null){
			throw new IllegalArgumentException("g:param tag requires name attribute");
		}
		if(value==null){
			throw new IllegalArgumentException("g:param tag requires value attribute");
		}
		boolean replace = false;
		Object rep = resolve(attributes,"replace");
		if(rep!=null && (rep == Boolean.TRUE || rep.toString().equalsIgnoreCase("true"))){
			replace = true;
		}
		URIBuilder builder = (URIBuilder) get(body,Uri.CURRENT_URI_BUILDER);
		if(builder==null){
			throw new RuntimeException("g:param tag must be called inside a g:http, g:uri or g:ws tag");
		}
		if(replace){
			builder.setParameter(name.toString(), value.toString());
		}
		else{
			builder.addParameter(name.toString(), value.toString());
		}
		return null;
	}
	
}

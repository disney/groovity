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

import java.util.ArrayList;
import java.util.Map;

import org.apache.http.message.BasicHeader;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
/**
 * add a header to an http request
 * <p>
 * header( <ul>	
 *	<li><b>name</b>: 
 *	the name of the header to add,</li>	
 *	<li><b>value</b>: 
 *	the value of the header to set,</li>
 *	</ul>{});
 *	
 *	<p>Sample
 *	<pre>
 *	import org.apache.http.util.EntityUtils;
 *	resp = http(url:&quot;http://google.com&quot;,{
 *		header(name:&quot;User-Agent&quot;,value:&quot;Lynx/2.8.8dev.3 libwww-FM/2.14 SSL-MM/1.4.1&quot;)
 *		handler({
 *			EntityUtils.toString(httpResponse.entity).replaceAll(&quot;(?i)Google&quot;,&quot;Elgoop&quot;)
 *		})
 *	})
 *	&lt;~ ${resp} ~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
		info = "add a header to an http request",
		sample="import org.apache.http.util.EntityUtils;\n" + 
				"resp = http(url:\"http://google.com\",{\n" + 
				"	header(name:\"User-Agent\",value:\"Lynx/2.8.8dev.3 libwww-FM/2.14 SSL-MM/1.4.1\")\n" + 
				"	handler({\n" + 
				"		EntityUtils.toString(httpResponse.entity).replaceAll(\"(?i)Google\",\"Elgoop\")\n" + 
				"	})\n" + 
				"})\n" + 
				"<~ ${resp} ~>",
		attrs = { 
			@Attr(
				name = "name", 
				info="the name of the header to add",
				required = true
			),
			@Attr(
				name = GroovityConstants.VALUE, 
				info="the value of the header to set",
				required = true
			)
		} 
	)
public class Header implements Taggable{
	public static String CURRENT_LIST_FOR_HEADERS = INTERNAL_BINDING_PREFIX+"headersAccumulator";
	@SuppressWarnings({"rawtypes","unchecked"})
	public Object tag(Map attributes, Closure body) {
		Object name = resolve(attributes,"name");
		Object value = resolve(attributes,VALUE);
		if(name==null){
			throw new IllegalArgumentException("g:header tag requires name attribute");
		}
		if(value==null){
			throw new IllegalArgumentException("g:header tag requires value attribute");
		}
		ArrayList<org.apache.http.Header> headers =  (ArrayList<org.apache.http.Header>) get(body,CURRENT_LIST_FOR_HEADERS);
		if(headers!=null){
			headers.add(new BasicHeader(name.toString(), value.toString()));
		}
		
		return null;
	}
	
}

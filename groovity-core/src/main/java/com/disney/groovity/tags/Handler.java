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

import groovy.lang.Binding;
import groovy.lang.Closure;

import java.util.Map;
import java.util.function.Function;

import org.apache.http.HttpResponse;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
/**
 * Define a custom handler for an HTTP response or WebSocket message
 * <p>
 * handler( <ul>	
 *	<li><i>var</i>: 
 *	Variable name for the org.apache.http.HttpResponse, defaults to 'httpResponse',</li>
 *	</ul>{
 *	<blockquote>// code that will handle a org.apache.http.HttpResponse or websocket message</blockquote>
 * 	});
 *	
 *	<p>Sample
 *	<pre>
 *	import org.apache.http.util.EntityUtils;
 *	resp = http(url:&quot;http://google.com&quot;,{
 *		handler({
 *			EntityUtils.toString(httpResponse.entity).replaceAll(&quot;(?i)Google&quot;,&quot;Elgoop&quot;)
 *		})
 *	})
 *	&lt;~ ${resp} ~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(	info="Define a custom handler for an HTTP response or WebSocket message", 
		body="code that will handle a org.apache.http.HttpResponse or websocket message",
		sample="import org.apache.http.util.EntityUtils;\n" + 
				"resp = http(url:\"http://google.com\",{\n" + 
				"	handler({\n" + 
				"		EntityUtils.toString(httpResponse.entity).replaceAll(\"(?i)Google\",\"Elgoop\")\n" + 
				"	})\n" + 
				"})\n" + 
				"<~ ${resp} ~>",
		attrs={
			@Attr(name=GroovityConstants.VAR,info="Variable name for the org.apache.http.HttpResponse, defaults to 'httpResponse'")
})
public class Handler implements Taggable {
	public static final String HANDLER_BINDING = INTERNAL_BINDING_PREFIX+"CURRENT_HANDLER";
	
	@SuppressWarnings("rawtypes")
	public Object tag(final Map attributes, final Closure body) throws Exception {
		final Object var = resolve(attributes,VAR);
		bind(body,HANDLER_BINDING, (Function) (arg -> {
			final Binding callBinding = new Binding();
			final String varName = var!=null?var.toString():
				(arg instanceof HttpResponse ? "httpResponse" : "message");
			callBinding.setVariable(varName, arg);
			body.setDelegate(callBinding);
			body.setResolveStrategy(Closure.DELEGATE_FIRST);
			return body.call(arg);
		}));
		return null;
	}

}

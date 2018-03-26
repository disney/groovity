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
import groovy.lang.Writable;
import groovy.xml.XmlUtil;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.util.Whitespace;
/**
 * Truncate a string to a maximum length; algorithm will strip HTML tags, trim and choose the first whitespace before the max position as the truncation point
 * <p>
 * truncate( <ul>	
 *	<li><b>max</b>: 
 *	maximum number of characters to output,</li>	
 *	<li><i>value</i>: 
 *	text to truncate, if not specified value should be in tag body,</li>	
 *	<li><i>var</i>: 
 *	variable to store truncated text,</li>	
 *	<li><i>suffix</i>: 
 *	string to append to end of truncated text,</li>
 *	</ul>{
 *	<blockquote>// Optionally the text to truncate; alternative to using value attribute</blockquote>
 * 	});
 *	
 *	<p><b>returns</b> The truncated text as a string
 *
 *	<p>Sample
 *	<pre>
 *	&lt;~ &lt;g:truncate var=&quot;temp&quot; value=&quot;${someText}&quot; max=&quot;14&quot;/&gt; ~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */ 	 
@Tag(
		info = "Truncate a string to a maximum length; algorithm will strip HTML tags, trim and choose the first whitespace before the max position as the truncation point",
		body = "Optionally the text to truncate; alternative to using value attribute",
		sample="<~ <g:truncate var=\"temp\" value=\"${someText}\" max=\"14\"/> ~>",
		returns="The truncated text as a string",
		attrs = { 
			@Attr(
					name = "max", 
					info="maximum number of characters to output",
					required = true
			),
			@Attr(
					name = GroovityConstants.VALUE, 
					info="text to truncate, if not specified value should be in tag body",
					required = false
			),
			@Attr(
					name = GroovityConstants.VAR, 
					info="variable to store truncated text",
					required = false
			),
			@Attr(
					name = "suffix", 
					info="string to append to end of truncated text",
					required = false
			)			
		} 
	)
public class Truncate implements Taggable {
	Pattern tagMatcher = Pattern.compile("(?s)</?\\w[^>]*>");
	Pattern entityMatcher = Pattern.compile("&(amp|lt|gt|quot|apos);");
	
	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) throws IOException{
		Object max = resolve(attributes,"max");
		if(max==null){
			throw new RuntimeException("g:truncate tag requires max attribute");
		}
		int imax;
		if(max instanceof Number){
			imax = ((Number)max).intValue();
		}
		else{
			imax = Integer.parseInt(max.toString());
		}
		Object var = resolve(attributes,VAR);
		Object value = resolve(attributes,VALUE);
		Object oldOut = get(body,OUT);
		if(value==null){
			CharArrayWriter writer = new CharArrayWriter();
			bind(body,OUT, writer);
			try{
				Object rval = body.call();
				if(writer.size()==0) {
					if(rval instanceof Writable){
						((Writable)rval).writeTo(writer);
					}
					else if(rval instanceof CharSequence) {
						writer.append((CharSequence)rval);
					}
				}
			}
			finally{
				bind(body,OUT,oldOut);
			}
			value = writer.toString();
		}
		Object suffix = resolve(attributes,"suffix");
		//first, strip HTML tags
		String valueStr = tagMatcher.matcher(value.toString()).replaceAll("");
		//second, unescape XML,
		Matcher matcher = entityMatcher.matcher(valueStr);
		StringBuffer vout = new StringBuffer();
		while(matcher.find()){
			String e = matcher.group(1);
			if("amp".equals(e)){
				matcher.appendReplacement(vout, "&");
			}
			else if("lt".equals(e)){
				matcher.appendReplacement(vout, "<");
			}
			else if("gt".equals(e)){
				matcher.appendReplacement(vout, ">");
			}
			else if("quot".equals(e)){
				matcher.appendReplacement(vout, "\"");
			}
			else if("apos".equals(e)){
				matcher.appendReplacement(vout, "'");
			}
		}
		matcher.appendTail(vout);
		valueStr = vout.toString();
		//third trim
		valueStr = valueStr.trim();
		//fourth check length
		if(valueStr.length() > imax){
			int cut = imax;
			for(int pos = cut;pos>0;pos--){
				if(Whitespace.isWhitespace(valueStr.charAt(pos))){
					cut = pos;
					break;
				}
			}
			valueStr = valueStr.substring(0,cut);
			if(suffix!=null){
				valueStr = valueStr.concat(suffix.toString());
			}
		}
		//finally re-escape xml
		valueStr = XmlUtil.escapeXml(valueStr);
		//determine output
		if(var!=null){
			bind(body,var.toString(), valueStr);
		}
		return valueStr;
	}
}

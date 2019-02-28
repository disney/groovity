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

import java.io.CharArrayWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Formatter;
import java.util.Map;

import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.model.ModelFilter;
import com.disney.groovity.model.ModelWalker;
import com.disney.groovity.model.ModelJsonWriter;
import com.disney.groovity.model.ModelTemplateWriter;
import com.disney.groovity.model.ModelXmlWriter;
import com.disney.groovity.util.HtmlEscapingWriter;
import com.disney.groovity.util.JsonEscapingWriter;
import com.disney.groovity.util.XmlEscapingWriter;

import groovy.lang.Closure;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.MetaProperty;
import groovy.lang.Writable;

/**
 * Serializes value to out in json or string format
 * <p>
 * write( <ul>	
 *	<li><i>value</i>: 
 *	The object or collection to be written,</li>	
 * 	<li><i>var</i>: 
 *	The variable name to store the destination of the write operation, as specified by 'to',</li>	
 *	<li><i>format</i>: 
 *	'xml' or 'json' for writing structured data (json is default), '~' to use templates found in the value or injected by ModelFilters, or a java String Format to serialize the value, or body can be used to generate raw characters,</li>	
 *	<li><i>pretty</i>: 
 *	pretty print json or xml,</li>	
 *	<li><i>null</i>: 
 *	value to display for null, defaults to empty string,</li>	
 *	<li><i>escape</i>: 
 *	escape the output as one of (xml|html|json),</li>	
 *	<li><i>to</i>: 
 *	a writer to print the output to, defaults to binding out, or can specify String.class or empty string to write to a string,</li>
 *	<li><i>filter</i>: 
 *	A ModelFilter or collection of ModelFilter objects to be used to transform the value during json or xml serialization,</li>
 *	<li><i>root</i>: 
 *	custom root element name for XML output</li>
 *  <li><i>namespaces</i>: 
 *	custom mapping of namespace prefixes to namespace URIs</li>
 *	</ul>{
 *	<blockquote>// code that writes output that will be printed if no value is specified</blockquote>
 * 	});
 *	
 *	<p><b>returns</b> the writer specified as to, or a String if to is empty string
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;~ &lt;g:write value=&quot;${['a','b','c']}&quot; pretty=&quot;true&quot; escape=&quot;json&quot; /&gt; ~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */ 	
@Tag(
		info="Serializes value to out in json or string format",
		body="code that writes output that will be printed if no value is specified",
		sample="<~ <g:write value=\"${['a','b','c']}\" pretty=\"true\" escape=\"json\" /> ~>",
		returns="the writer specified as to, or a String if to is empty string",
		attrs={ 
				@Attr(name=GroovityConstants.VALUE,required=false,info="The object or collection to be written"),
				@Attr(name=GroovityConstants.VAR,required=false,info="The variable name to store the destination of the write operation, as specified by 'to'"),
				@Attr(name="format",required=false,info="'xml' or 'json' for writing structured data (json is default), '~' to use templates found in the value or injected by ModelFilters, or a java String Format to serialize the value, or body can be used to generate raw characters"),
				@Attr(name="pretty",required=false,info="pretty print json or xml"),
				@Attr(name="null",required=false,info="value to display for null, defaults to empty string"),
				@Attr(name="escape",required=false,info="escape the output as one of (xml|html|json)"),
				@Attr(name="to",required=false,info="a writer to print the output to, defaults to binding out, or can specify String.class or empty string to write to a string"),
				@Attr(name="filter",required=false,info="A ModelFilter or collection of ModelFilter objects to be used to transform the value during json or xml serialization"),
				@Attr(name="root",required=false,info="custom root element name for XML output"),
				@Attr(name="namespaces",required=false,info="custom mapping of namespace prefixes to namespace URIs")
		}
		)
public class Write implements Taggable{
	
	@SuppressWarnings({ "rawtypes", "resource" })
	public Object tag(Map attributes, Closure body) throws Exception {
		boolean valueDeclared = attributes.containsKey(VALUE);
		Object value = null;
		if(valueDeclared){
			value = resolve(attributes,VALUE);
			if(value==null){
				value = resolve(attributes,"null");
			}
		}
		Object format = resolve(attributes,"format");
		Object escape = resolve(attributes,"escape");
		Object pretty = resolve(attributes,"pretty");
		if(pretty!=null && !(pretty instanceof Boolean)){
			pretty = Boolean.valueOf(pretty.toString());
		}
		Object to = resolve(attributes,"to");
		boolean returnTo = true;
		if(to==null){
			returnTo = false;
			//fall back on body out
			to=get(body,OUT);
		}
		if(to==null){
			throw new IllegalArgumentException("write tag requires valid Writer or String.class for 'to'");
		}
		Object filter = resolve(attributes,"filter");
		Writer writer;
		boolean returnString = false;
		if(to instanceof Writer) {
			writer = (Writer) to;
		}
		else if(to.equals(String.class) || to instanceof CharSequence) {
			writer = new CharArrayWriter();
			returnString = true;
		}
		else {
			throw new IllegalArgumentException("write tag requires valid Writer or String.class for 'to', unrecognized option "+to);
		}
		Writer returnWriter = writer;
		if(escape!=null){
			String esc = escape.toString();
			if(esc.equalsIgnoreCase("xml")){
				writer = new XmlEscapingWriter(writer);
			}
			else if(esc.equalsIgnoreCase("json")){
				writer = new JsonEscapingWriter(writer);
			}
			else if(esc.equalsIgnoreCase("html")){
				writer = new HtmlEscapingWriter(writer);
			}
			else{
				throw new IllegalArgumentException("Unrecognized escape value "+esc+", try xml or json");
			}
		}
		if(value == null){
			Object oldOut = get(body,OUT);
			bind(body,OUT, writer);
			try {
				value = body.call();
			}
			finally {
				bind(body,OUT, oldOut);
			}
		}
		if(value!=null){
			if(format!=null && !"json".equals(format) && !"xml".equals(format) && !"~".equals(format)){
				//we don't want to close the formatter because it's not our job to close the writer
				Formatter formatter = new Formatter(writer);
				if(value instanceof Collection){
					formatter.format(format.toString(), ((Collection)value).toArray());
				}
				else{
					formatter.format(format.toString(), value);
				}
			}
			else{
				if(value instanceof CharSequence){
					writer.append((CharSequence)value);
				}
				else if((filter==null) && (value instanceof Writable) && (value!=body.getOwner())){
					((Writable)value).writeTo(writer);
				}
				else{
					ModelWalker mw;
					if(format==null) {
						Object response = get(body,"response");
						if(response!=null) {
							MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(response.getClass());
							MetaProperty mp = mc.hasProperty(response, "contentType");
							if(mp!=null) {
								String ct = (String) mp.getProperty(response);
								if(ct!=null && ct.contains("xml")) {
									format="xml";
								}
							}
						}
					}
					if("xml".equals(format)) {
						if(pretty!=null && ((Boolean)pretty).booleanValue()){
							mw = new ModelXmlWriter(writer,"\t");
							//prevent secondary pretty, just write it pretty up front!
							pretty = Boolean.FALSE;
						}
						else{
							mw = new ModelXmlWriter(writer);
						}
						String root = resolve(attributes,"root",String.class);
						if(root!=null) {
							((ModelXmlWriter)mw).setRootElementName(root);
						}
						@SuppressWarnings("unchecked")
						Map<String,String> prefixes = resolve(attributes,"namespaces",Map.class);
						if(prefixes!=null) {
							((ModelXmlWriter)mw).setNamespacePrefixes(prefixes);
						}
					}
					else if("~".equals(format)) {
						mw = new ModelTemplateWriter(writer);
					}
					else {
						if(pretty!=null && ((Boolean)pretty).booleanValue()){
							mw = new ModelJsonWriter(writer,"\t");
							//prevent secondary pretty, just write it pretty up front!
							pretty = Boolean.FALSE;
						}
						else{
							mw = new ModelJsonWriter(writer);
						}
					}
					
					if(filter!=null) {
						ModelFilter[] mfa = null;
						if(filter instanceof Collection) {
							Collection src = (Collection) filter;
							mfa = new ModelFilter[src.size()];
							int pos = 0;
							for(Object mf: src) {
								mfa[pos++] = (ModelFilter) DefaultTypeTransformation.castToType(mf,ModelFilter.class);
							}
						}
						else if(filter.getClass().isArray() && filter.getClass().getComponentType().equals(ModelFilter.class)) {
							mfa = (ModelFilter[]) filter;
						}
						else if(filter instanceof ModelFilter) {
							mfa = new ModelFilter[] {(ModelFilter)filter};
						}
						else {
							mfa = new ModelFilter[] { (ModelFilter) DefaultTypeTransformation.castToType(filter,ModelFilter.class)};
						}
						mw.setFilters(mfa);
					}
					mw.visit(value);
				}
			}
		}
		if(!returnTo) {
			return null;
		}
		Object rval = returnString ? returnWriter.toString() : returnWriter;
		if(attributes.get(VAR) != null) {			
			bind(body,attributes.get(VAR).toString(),rval);
		}
		return rval;
	}
	
}

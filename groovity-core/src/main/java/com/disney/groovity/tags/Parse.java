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

import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.MetaProperty;
import groovy.lang.Writable;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.sax.SAXSource;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.model.Model;
import com.disney.groovity.util.XmlParser;
/**
 * Will parse JSON or XML from a variety of sources, sometimes type can be inferred and when not use the type parameter
 * <p>
 * parse( <ul>	
 *	<li><i>var</i>: 
 *	The variable name to store the parsed object,</li>	
 *	<li><i>value</i>: 
 *	the value to parse (e.g. http request or response, stream, reader, string, etc), otherwise body of tag is parsed,</li>	
 *	<li><i>format</i>: 
 *	specify json or xml, defaults to JSON unless the value has an XML content type or starts with &lt;,</li>	
 *	<li><i>to</i>: 
 *	java class name or concrete object to parse data into, defaults to Map or List,</li>
 *	</ul>{
 *	<blockquote>// Optional, code that produces raw xml or json that should be parsed, use value attribute to parse from a variable</blockquote>
 * 	});
 *	
 *	<p><b>returns</b> the parsed object
 *	
 *	<p>Sample
 *	<pre>
 *	ip = http(url:&quot;http://jsonip.com&quot;,timeout:5,{
 *		handler({ parse(value:httpResponse).ip })
 *	})
 *	&lt;~${ip}~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(	info="Will parse JSON or XML from a variety of sources, sometimes type can be inferred and when not use the type parameter",
		body="Optional, code that produces raw xml or json that should be parsed, use value attribute to parse from a variable",
		sample="ip = http(url:\"http://jsonip.com\",timeout:5,{\n" + 
				"	handler({ parse(value:httpResponse).ip })\n" + 
				"})\n" + 
				"<~${ip}~>",
		returns="the parsed object",
		attrs={ 
			@Attr(name=GroovityConstants.VAR, info = "The variable name to store the parsed object", required=false),
			@Attr(name=GroovityConstants.VALUE, info = "the value to parse (e.g. http request or response, stream, reader, string, etc), otherwise body of tag is parsed",required=false),
			@Attr(name="format", info="specify json or xml, defaults to JSON unless the value has an XML content type or starts with <", required=false),
			@Attr(name="to", info="java class name or concrete object to parse data into, defaults to Map or List", required = false),
		}
)
public class Parse implements Taggable {
	private static final Pattern charsetPattern = Pattern.compile("(?i)(?<=charset=)([^;,\\r\\n]+)");
	//TODO wire this in so we can clean out contexts for classes that get compiled away at runtime to avoid leakage
	private static final ConcurrentHashMap<Class<?>, JAXBContext> jaxbContextCache = new ConcurrentHashMap<>();
	
	public void init(){
	}
	
	private static JAXBContext getJAXBContext(Class<?> c) throws JAXBException {
		JAXBContext jc = jaxbContextCache.get(c);
		if(jc==null) {
			jc = JAXBContext.newInstance(c);
			JAXBContext oc = jaxbContextCache.putIfAbsent(c, jc);
			if(oc!=null) {
				return oc;
			}
		}
		return jc;
	}
	
	@SuppressWarnings({ "rawtypes" })
	public Object tag(Map attributes, Closure body) throws Exception {
		Object var = resolve(attributes,VAR);
		String format = resolve(attributes,"format", String.class);
		Object value = resolve(attributes,VALUE);
		Object target = resolve(attributes,"to");
		if(target instanceof Class) {
			if(!Object.class.equals(target)) {
				target = ((Class)target).newInstance();
			}
		}
		if(target == null) {
			target = Object.class;
		}
		if(value==null){
			//grab value from body if it didn't come from the attribute
			Object oldOut = get(body,OUT);
			CharArrayWriter writer = new CharArrayWriter();
			bind(body,OUT, writer);
			try{
				value = body.call();
				if(writer.size()==0) {
					if(value instanceof Writable){
						((Writable)value).writeTo(writer);
					}
				}
				if(writer.size()>0) {
					value = writer.toString();
				}
			}
			finally{
				bind(body,OUT, oldOut);
			}
		}
		Object result = parse(value, format, target);
		if(var!=null){
			bind(body,var.toString(), result);
		}
		return result;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object parse(Object value, String format, final Object target) throws Exception{
		Reader reader = null;
		Object result = target;
		if(value instanceof Reader){
			reader=((Reader)value);
		}
		else if(value instanceof InputStream){
			reader = new BufferedReader(new InputStreamReader((InputStream)value,"UTF-8"));
		}
		else if(value instanceof File){
			reader = new FileReader((File)value);
		}
		else if(value instanceof byte[]){
			String str = new String(((byte[])value),"UTF-8").trim();
			reader = new StringReader(str);
			if(format==null){
				if(str.startsWith("<")){
					format="xml";
				}
			}
		}
		else if(value instanceof HttpResponse){
			HttpResponse response = (HttpResponse) value;
			HttpEntity entity = response.getEntity();
			if(entity!=null){
				org.apache.http.Header ct = entity.getContentType();
				String charset = "UTF-8";
				if(ct!=null){
					String contentType = ct.getValue();
					Matcher charMatcher = charsetPattern.matcher(contentType);
					if(charMatcher.find()){
						charset = charMatcher.group(1);
					}
					if(format==null){
						if(contentType.contains("xml")){
							format="xml";
						}
					}
				}
				reader = new BufferedReader(new InputStreamReader(entity.getContent(),charset));
			}
		}
		else if(value instanceof Map) {
			if(target.equals(Object.class)) {
				result = Model.copy(value);
			}
			else {
				((Map)value).forEach((k,v)->{ Model.put(target, k.toString(), v); });
			}
		}
		else if(value!=null){
			//check for http request
			MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(value.getClass());
			MetaProperty mp = mc.hasProperty(value, "reader");
			if(mp!=null && Reader.class.isAssignableFrom(mp.getType())) {
				reader = (Reader) mp.getProperty(value);
				if(format==null) {
					MetaProperty ctp = mc.hasProperty(value, "contentType");
					if(ctp!=null) {
						String ct = (String) ctp.getProperty(value);
						if(ct!=null && ct.contains("xml")) {
							format="xml";
						}
					}
				}
			}
			else {
				String toParse = value.toString().trim();
				reader = new StringReader(toParse);
				if(format==null){
					if(toParse.startsWith("<")){
						format="xml";
					}
				}
			}
		}
		if(reader!=null){
			if(format!=null && "xml".equalsIgnoreCase(format.toString())){
				boolean usejaxb=false;
				if(!target.equals(Object.class)) {
					if(target.getClass().isAnnotationPresent(XmlRootElement.class)) {
						usejaxb=true;
					}
				}
				if(usejaxb) {
					JAXBContext context = getJAXBContext(target.getClass());
					Unmarshaller um = context.createUnmarshaller();
					XMLReader xreader = XmlParser.borrowXMLReader();
					try {
						result = um.unmarshal(new SAXSource(xreader, new InputSource(reader)));
					}
					finally {
						XmlParser.returnXMLReader(xreader);
						reader.close();
					}
				}
				else {
					try {
						Object converted = XmlParser.parseXML(reader);
						if(!target.equals(Object.class)) {
							if(target instanceof Model) {
								Model.each(converted, ((Model)target)::put);
							}
							else {
								Model.each(converted, (k,v)->{
									Model.put(target, k, v);
								});
							}
						}
						else {
							result = converted;
						}
					}
					finally{
						reader.close();
					}
				}
			}
			else{
				try{
					Object parsed = new JsonSlurper().parse(reader);
					if(!target.equals(Object.class)) {
						if(target instanceof Model) {
							Model.each(parsed, ((Model)target)::put);
						}
						else {
							Model.each(parsed, (k,v)->{
								Model.put(target, k, v);
							});
						}
					}
					else {
						result = parsed;
					}
				}
				finally{
					reader.close();
				}
			}
		}
		return result;
	}
	
}

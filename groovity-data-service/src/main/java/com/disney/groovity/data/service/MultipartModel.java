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
package com.disney.groovity.data.service;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.servlet.http.Part;

import org.apache.http.entity.ContentType;

import com.disney.groovity.data.Attachment;
import com.disney.groovity.data.AttachmentCollector;
import com.disney.groovity.model.Model;
import com.disney.groovity.model.ModelWalker;
import com.disney.groovity.util.MetaPropertyLookup;

import groovy.lang.MetaProperty;
/**
 * Copy a collection of form parts onto a model to update it; can be used to convert file uploads to attachments
 * 
 * @author Alex Vigdor
 *
 */
public class MultipartModel {
	private static Object merge(Object a, Object b) {
		Attachment f = null;
		Object other = null;
		if(b instanceof Attachment) {
			f = (Attachment) b;
			other = a;
		}
		else if(!(b instanceof Map)) {
			//simple values can't be merged
			return b;
		}
		else if(a instanceof Attachment) {
			f = (Attachment) a;
			other = b;
		}
		if(f!=null) {
			if(other instanceof Model) {
				((Model)other).each(f::put);
			}
			else {
				Model.each(other, f::put);
			}
			return f;
		}
		else {
			LinkedHashMap<String, Object> dest = new LinkedHashMap<>();
			if(a instanceof Model) {
				((Model)a).each(dest::put);
			}
			else {
				Model.each(a, dest::put);
			}
			if(b instanceof Model) {
				((Model)b).each(dest::put);
			}
			else {
				Model.each(b, dest::put);
			}
			return dest;
		}
	}
	@SuppressWarnings("unchecked")
	public static void copy(Collection<Part> parts, Model model) throws Exception {
		//collect attachments from model to re-attach after processing form
		AttachmentCollector collector = new AttachmentCollector();
		collector.visit(model);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		Map<String, Object> modelMap;
		modelMap = (Map<String, Object>) model.map(Attachment.DESCRIBE);
		parts.forEach(part->{
			Object value;
			if(part.getSubmittedFileName()!=null) {
				//attachment
				value = new PartAttachment(part);
			}
			else {
				String charset = "UTF-8";
				if(part.getContentType()!=null) {
					ContentType ct = ContentType.parse(part.getContentType());
					if(ct.getCharset()!=null) {
						charset = ct.getCharset().name();
					}
				}
				try(InputStreamReader reader = new InputStreamReader(part.getInputStream(),charset)){
					CharArrayWriter writer = new CharArrayWriter();
					char[] buf = new char[4096];
					int c = 0;
					while((c = reader.read(buf)) !=-1) {
						writer.write(buf,0,c);
					}
					value = writer.toString();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			String[] fieldPath = part.getName().split("\\.");
			Map<String, Object> source = modelMap;
			AtomicReference<Object> destRef = new AtomicReference<>(data);
			Function<String, Object> destGet = f->{
				Object dest = destRef.get();
				if(dest instanceof Map) {
					return ((Map<String,Object>)dest).get(f);
				}
				MetaProperty prop = MetaPropertyLookup.getSettableMetaProperty(dest, f);
				if(prop==null) {
					return null;
				}
				return prop.getProperty(dest);
			};
			BiConsumer<String,Object> destPut = (f,v)->{
				Object dest = destRef.get();
				if(dest instanceof Map) {
					((Map<String,Object>)dest).put(f,v);
				}
				else if(dest instanceof Model) {
					((Model)dest).put(f, v);
				}
				else {
					Model.put(dest, f, v);
				}
			};
			int pos = 0;
			while(pos<fieldPath.length){
				Field field = new Field(fieldPath[pos]);
				int listPos = field.listPos;
				if(listPos > Integer.MIN_VALUE) {
					List<Map<String, Object>> s = null;
					if(source!=null) {
						s = (List<Map<String, Object>>) source.get(field.name);
						source = null;
						if(s!=null) {
							if(listPos < 0) {
								//indexed from end
								listPos = s.size() - listPos - 1;
							}
							else if(s.size() > listPos) {
								source = s.get(listPos);
							}
						}
					}
					if(listPos < 0) {
						listPos = - listPos - 1;
					}
					List<Object> nd =  (List<Object>) destGet.apply(field.name);
					if(nd==null) {
						if(s!=null) {
							nd = new ArrayList<>(s);
						}
						else {
							nd = new ArrayList<>();
						}
						destPut.accept(field.name, nd);
					}
					Object ndo = null;
					if(nd.size()>listPos) {
						ndo = nd.get(listPos);
					}
					while(listPos>=nd.size()) {
						nd.add(null);
					}
					if(pos == fieldPath.length-1) {
						if(ndo!=null) {
							nd.set(listPos, merge(ndo,value));
						}
						else if(source!=null) {
							nd.set(listPos, merge(source,value));
						}
						else {
							nd.set(listPos, value);
						}
					}
					else if(ndo==null) {
						LinkedHashMap <String,Object> ndm =  new LinkedHashMap<>();
						ndo = ndm;
						if(source!=null) {
							ndm.putAll(source);
						}
						nd.set(listPos, ndo);
					}
					destRef.set(ndo);
				}
				else {
					Object s = null;
					if(source!=null) {
						s = source.get(field.name);
					}
					Object nd = destGet.apply(field.name);
					if(pos == fieldPath.length-1) {
						if(nd!=null) {
							destPut.accept(field.name, merge(nd,value));
						}
						else if(s!=null) {
							destPut.accept(field.name, merge(s,value));
						}
						else {
							destPut.accept(field.name, value);
						}
					}
					else {
						source = (Map<String, Object>) s;
						if(nd==null){
							LinkedHashMap <String,Object> ndm =  new LinkedHashMap<>();
							nd = ndm;
							if(source!=null) {
								ndm.putAll( source);
							}
							destPut.accept(field.name, nd);
						}
						destRef.set(nd);
					}
				}
				pos++;
			}
		});
		if(!collector.getAttachments().isEmpty()) {
			ModelWalker walker = new ModelWalker() {
				public void visitObjectField(String name, Object value) throws Exception {
					Attachment a = convert(value);
					if(a!=null) {
						Model.put(getCurrentObject(),name,a);
					}
					else {
						super.visitObjectField(name, value);
					}
				}

				public void visitListMember(Object value) throws Exception {
					Attachment a = convert(value);
					if(a!=null) {
						((List<Object>)getCurrentObject()).set(positions.peek().get(), a);
					}
					else {
						super.visitListMember(value);
					}
				}

				private Attachment convert(Object value) {
					if(value instanceof Map) {
						Map<String,Object> map = ((Map<String,Object>)value);
						if(map.containsKey("name") && map.containsKey("contentType") && map.containsKey("length")) {
							String aname = String.valueOf(map.get("name"));
							Attachment a = collector.getAttachments().get(aname);
							if(a!=null) {
								a = (Attachment) a.copy();
								a.putAll(map);
								return a;
							}
						}
					}
					return null;
				}
			};
			walker.visit(data);
		}
		data.forEach((k,v)->{
			model.put(k, v);
		});
	}
	
	private static class Field{
		protected String name;
		protected int listPos = Integer.MIN_VALUE;
		public Field(String nameSpec) {
			int bp = nameSpec.indexOf("[");
			if(bp>0) {
				listPos = Integer.parseInt(nameSpec.substring(bp+1,nameSpec.length()-1));
				nameSpec = nameSpec.substring(0,bp);
			}
			this.name = nameSpec;
		}
	}
}

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
package com.disney.groovity;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;

/**
 * Responsible for managing a map of Taggables which can be called by name from Scripts
 * 
 * @author Alex Vigdor
 *
 */
public class Taggables implements GroovityConstants{
	private Groovity groovity;
	
	private Map<String, Taggable> groovyTags = new ConcurrentHashMap<String, Taggable>();
	
	public Taggables(){
	}
	
	public void add(Taggable... tags){
		for(Taggable tag: tags){
			if(groovity!=null){
				tag.setGroovity(groovity);
				tag.init();
			}
			Taggable oldtag = this.groovyTags.put(Introspector.decapitalize(tag.getClass().getSimpleName()), tag);
			if(oldtag!=null){
				oldtag.destroy();
			}
		}
	}
	public void init(Groovity groovity){
		this.groovity=groovity;
		Taggable[] preRegisteredTags = groovyTags.values().toArray(new Taggable[0]);
		groovyTags = new ConcurrentHashMap<String, Taggable>();
		ClassLoader tagLoader = groovity.getParentLoader();
		if(tagLoader==null){
			tagLoader = Thread.currentThread().getContextClassLoader();
		}
		for(Taggable tag: ServiceLoader.load(Taggable.class,tagLoader)){
			add(tag);
		}
		add(preRegisteredTags);
	}
	public void destroy(){
		for(Taggable tag: groovyTags.values()){
			tag.destroy();
		}
		groovyTags.clear();
	}
	public void addAll(List<Taggable> tags){
		add(tags.toArray(new Taggable[0]));
	}
	
	@SuppressWarnings("rawtypes") 
	public Object tag(final String tagName, final Map attributes, final groovy.lang.Closure body) throws Exception{
		final Taggable taggable = groovyTags.get(tagName);
		if(taggable==null){
			throw new RuntimeException("Tag not found: ".concat(tagName));
		}
		return taggable.tag(attributes, body);
	}
	
	public boolean hasTag(final String tagName){
		return groovyTags.containsKey(tagName);
	}
	
	public Taggable getTag(final String tagName) {
		final Taggable taggable = groovyTags.get(tagName);
		if(taggable==null){
			throw new RuntimeException("Tag not found: ".concat(tagName));
		}
		return taggable;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" }) 
	public List getDocs(){
		ArrayList docs = new ArrayList();
		ArrayList<Map.Entry<String,Taggable>> entries = new ArrayList<Map.Entry<String,Taggable>>();
		entries.addAll(groovyTags.entrySet());
		//alphabetize for consistency
		Collections.sort(entries, new Comparator<Map.Entry<String,Taggable>>() {
			public int compare(Entry<String, Taggable> o1,
					Entry<String, Taggable> o2) {
				return o1.getKey().compareTo(o2.getKey());
			}
		});
		for(Map.Entry<String,Taggable> entry:entries){
			Tag ti = entry.getValue().getClass().getAnnotation(Tag.class);
			if(ti!=null){
				LinkedHashMap doc = new LinkedHashMap();
				docs.add(doc);
				doc.put(NAME,entry.getKey());
				doc.put(INFO,ti.info());
				if(ti.body()!=null){
					doc.put(BODY,ti.body());
				}
				if(ti.returns()!=null){
					doc.put(RETURNS,ti.returns());
				}
				if(ti.sample()!=null){
					doc.put(SAMPLE, ti.sample());
				}
				boolean isCore = false;
				Package pkg = entry.getValue().getClass().getPackage();
				if(pkg!=null){
					isCore = GROOVITY_CORE_TAG_PACKAGE.equals(pkg.getName());
				}
				doc.put(CORE,isCore);
				Attr[] atts = ti.attrs();
				if(atts!=null){
					ArrayList attMap = new ArrayList();
					for(Attr att:atts){
						HashMap attVal = new HashMap();
						attVal.put(NAME, att.name());
						attVal.put(INFO, att.info());
						attVal.put(REQUIRED, att.required());
						attMap.add(attVal);
					}
					doc.put(ATTRS, attMap);
				}
			};
		}
		return docs;
	}
	
}

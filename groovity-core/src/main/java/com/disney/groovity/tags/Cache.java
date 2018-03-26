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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.compile.GroovityClassLoader;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.util.ScriptHelper;

import groovy.lang.Closure;
/**
 * Pull through caching for the lookup of values for one or a list of keys
 * <p>
 * cache( <ul>	
 *	<li><i>var</i>: 
 *	variable to store the returned object or map,</li>	
 *	<li><i>key</i>: 
 *	specifies a single key to be used to lookup a single value,</li>	
 *	<li><i>keys</i>: 
 *	mutually exclusive with key, specifies a collection of keys to lookup a map of keys to values,</li>	
 *	<li><i>name</i>: 
 *	specify a named cache to use,</li>	
 *	<li><i>refresh</i>: 
 *	specify number of seconds after which access triggers background refresh,</li>	
 *	<li><i>ttl</i>: 
 *	specify seconds until cache entries are evicted after being created or refreshed,</li>	
 *	<li><i>max</i>: 
 *	maximum number of items to store in this cache,</li>
 *	</ul>{
 *	<blockquote>// Code that takes takes a 'map' and fills in values for the keys</blockquote>
 * 	});
 *	
 *	<p><b>returns</b> Retrieved value for a single key, or a map of keys to values for a collection of keys
 *	
 *	<p>Sample
 *	<pre>
 *	def urls = [&quot;http://nytimes.com&quot;,&quot;http://nytimes.com/404notreal&quot;];
 *	def status = cache(keys:urls,name:&quot;httpStatusCache&quot;,ttl:1200,refresh:120,max:10000,{
 *		map.each{ entry -&gt;
 *			entry.value = http(url:entry.key,async:true,{
 *				handler({
 *					httpResponse.statusLine.statusCode;
 *				});
 *			});
 *		}
 *	});
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
	info="Pull through caching for the lookup of values for one or a list of keys",
	body="Code that takes takes a 'map' and fills in values for the keys",
	returns="Retrieved value for a single key, or a map of keys to values for a collection of keys",
	sample="def urls = [\"http://nytimes.com\",\"http://nytimes.com/404notreal\"];\n" + 
			"def status = cache(keys:urls,name:\"httpStatusCache\",ttl:1200,refresh:120,max:10000,{\n" + 
			"	map.each{ entry ->\n" + 
			"		entry.value = http(url:entry.key,async:true,{\n" + 
			"			handler({\n" + 
			"				httpResponse.statusLine.statusCode;\n" + 
			"			});\n" + 
			"		});\n" + 
			"	}\n" + 
			"});",
	attrs={
			@Attr(name=GroovityConstants.VAR,required=false,info="variable to store the returned object or map"),
			@Attr(name="key",required=false,info="specifies a single key to be used to lookup a single value"),
			@Attr(name="keys",required=false,info="mutually exclusive with key, specifies a collection of keys to lookup a map of keys to values"),
			@Attr(name="name",required=false,info="specify a named cache to use"),
			@Attr(name="refresh",required=false,info="specify number of seconds after which access triggers background refresh"),
			@Attr(name="ttl",required=false,info="specify seconds until cache entries are evicted after being created or refreshed"),
			@Attr(name="max",required=false,info="maximum number of items to store in this cache")
	}
	
)
public class Cache implements Taggable{
	
	@SuppressWarnings("rawtypes")
	protected com.disney.groovity.cache.Cache getCache(Map attributes, Closure body, int ttl, int max, boolean isLoader) throws NoSuchMethodException, SecurityException, URISyntaxException{
		Object namea = resolve(attributes,"name");
		GroovityClassLoader classLoader = getScriptHelper(body).getClassLoader();
		String name="defaultCache";
		if(namea!=null){
			name=namea.toString();
		}
		return classLoader.getCache(name, isLoader ? body : null, ttl, max);
	}
	
	@SuppressWarnings({"rawtypes","unchecked"})
	public Object tag(Map attributes, Closure body) throws Exception {
		
		Object refresha = resolve(attributes,"refresh");
		int refresh = -1;
		if(refresha!=null){
			if(refresha instanceof Number){
				refresh = ((Number)refresha).intValue();
			}
			else{
				refresh = Integer.parseInt(refresha.toString());
			}
		}
		Object ttla = resolve(attributes,"ttl");
		int ttl = -1;
		if(ttla!=null){
			if(ttla instanceof Number){
				ttl = ((Number)ttla).intValue();
			}
			else{
				ttl = Integer.parseInt(ttla.toString());
			}
		}
		Object maxa = resolve(attributes,"max");
		int max = -1;
		if(maxa!=null){
			if(maxa instanceof Number){
				max = ((Number)maxa).intValue();
			}
			else{
				max = Integer.parseInt(maxa.toString());
			}
		}
		Object keya = resolve(attributes,"key");
		Object keysa = resolve(attributes,"keys");
		Object vara = attributes.get(VAR);
		if(keya!=null && keysa!=null){
			throw new IllegalArgumentException("Cache tag requires key or keys attribute, not both");
		}
		if(keya==null && keysa==null){
			throw new IllegalArgumentException("Cache tag requires key or keys attribute");
		}
		com.disney.groovity.cache.Cache cache = getCache(attributes, body, ttl, max, true);
		if(keysa!=null){
			if(keysa.getClass().isArray()){
				keysa = Arrays.asList((Object[])keysa);
			}
			if(!(keysa instanceof Iterable)){
				throw new IllegalArgumentException("Cache keys must be Iterable");
			}
			Map<Object,Object> rval = cache.get((Iterable)keysa, refresh, ttl);
			if(vara!=null){
				bind(body,vara.toString(), rval);
			}
			return rval;
		}
		else{
			Map<Object,Object> rmap = cache.get(Arrays.asList(keya), refresh, ttl);
			Object rval = rmap.get(keya);
			if(vara!=null){
				bind(body,vara.toString(), rval);
			}
			return rval;
		}
	}

}

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

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.util.ScriptHelper;

import groovy.lang.Closure;
/**
 * Trigger a background refresh IF a value is already cached for a given key
 * <p>
 * cacheRefresh( <ul>	
 *	<li><i>key</i>: 
 *	specifies a single key to be refreshed,</li>	
 *	<li><i>keys</i>: 
 *	mutually exclusive with key, specifies a collection of keys to be refreshed in the cache,</li>	
 *	<li><i>name</i>: 
 *	specify a named cache to use,</li>
 *	</ul>{});
 *	
 *	<p>Sample
 *	<pre>
 *	cacheRefresh(name:&quot;myCache&quot;,key:&quot;myKey&quot;){}
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
	
	info="Trigger a background refresh IF a value is already cached for a given key",
	sample="cacheRefresh(name:\"myCache\",key:\"myKey\"){}",
	attrs={
			@Attr(name="key",required=false,info="specifies a single key to be refreshed"),
			@Attr(name="keys",required=false,info="mutually exclusive with key, specifies a collection of keys to be refreshed in the cache"),
			@Attr(name="name",required=false,info="specify a named cache to use")
	}
)
public class CacheRefresh extends Cache implements Taggable{
	
	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) throws Exception {	
		Object keya = resolve(attributes,"key");
		Object keysa = resolve(attributes,"keys");
		if(keya!=null && keysa!=null){
			throw new IllegalArgumentException("cacheRefresh requires key or keys attribute, not both");
		}
		if(keya==null && keysa==null){
			throw new IllegalArgumentException("cacheRefresh requires key or keys attribute");
		}
		com.disney.groovity.cache.Cache cache = getCache(attributes, body, -1, -1, false);
		if(cache!=null){
			if(keya!=null) {
				cache.refresh(keya);
			}
			if(keysa!=null) {
				if(keysa.getClass().isArray()){
					keysa = Arrays.asList((Object[])keysa);
				}
				if(!(keysa instanceof Iterable)){
					throw new IllegalArgumentException("cacheRefresh keys must be Iterable");
				}
				for(Object key: (Iterable)keysa) {
					cache.refresh(key);
				}
			}
		}
		return null;
	}
	
}

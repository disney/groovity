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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.util.ScriptHelper;

import groovy.lang.Closure;
/**
 * Remove a value from cache
 * <p>
 * cacheRemove( <ul>	
 *	<li><b>key</b>: 
 *	specifies a single key to be removed,</li>	
 *	<li><i>value</i>: 
 *	a specific value to be removed, if not present any value for the key will be removed,</li>	
 *	<li><i>name</i>: 
 *	specify a named cache to use,</li>
 *	</ul>{});
 *	
 *	<p>Sample
 *	<pre>
 *	cacheRemove(name:&quot;myCache&quot;,key:&quot;myKey&quot;,value:&quot;myValue&quot;,{});
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
	
	info="Remove a value from cache",
			sample="cacheRemove(name:\"myCache\",key:\"myKey\",value:\"myValue\",{});",
			attrs={
					@Attr(name="key",required=true,info="specifies a single key to be removed"),
					@Attr(name=GroovityConstants.VALUE,required=false,info="a specific value to be removed, if not present any value for the key will be removed"),
					@Attr(name="name",required=false,info="specify a named cache to use")
			}
)
public class CacheRemove extends Cache implements Taggable{
	private static final Logger log = Logger.getLogger(CacheRemove.class.getName());
	
	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) throws Exception {	
		Object keya = resolve(attributes,"key");
		boolean hasValue = attributes.containsKey(VALUE);
		Object valuea =null;
		if(hasValue){
			valuea = resolve(attributes,VALUE);
		}
		com.disney.groovity.cache.Cache cache = getCache(attributes, body, -1, -1, false);
		if(cache!=null){
			if(hasValue){
				cache.remove(keya, valuea);
			}
			else{
				cache.remove(keya);
			}
		}
		else if(log.isLoggable(Level.FINE)){
			log.fine("Attempt to access missing cache "+attributes.get("name"));
		}
		return valuea;
	}
	
}

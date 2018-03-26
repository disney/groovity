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
package com.disney.groovity.cache;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a single value in the cache, along with the date it was cached for enforcing TTLs
 *
 * @author Alex Vigdor
 */
public class CacheValue implements Serializable{
	private static final long serialVersionUID = 1330651206365886024L;
	private Object value;
	private long created;
	protected final AtomicBoolean pendingRefresh = new AtomicBoolean(false);
	
	public CacheValue(){
		
	}
	
	public CacheValue(Object value){
		this.value=value;
		this.created = Caches.currentCacheTime;
	}
	
	public int hashCode(){
		return  (int)(created ^ (created >>> 32)) + (value!=null?value.hashCode():-12345);
	}
	public boolean equals(Object o){
		if(o instanceof CacheValue){
			CacheValue oc = ((CacheValue)o);
			Object ov = oc.value;
			if(value==null){
				return ov==null;
			}
			if(value.equals(ov)){
				return oc.created==created;
			}
		}
		return false;
	}

	public Object getValue() {
		return value;
	}

	protected void setValue(Object value) {
		this.value = value;
	}

	public long getCreated() {
		return created;
	}

	protected void setCreated(long created) {
		this.created = created;
	}

}

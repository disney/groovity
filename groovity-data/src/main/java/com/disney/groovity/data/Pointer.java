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
package com.disney.groovity.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.disney.groovity.model.Model;
import com.disney.groovity.model.ModelConsumer;

import groovy.lang.MissingPropertyException;

/**
 * A pointer to a factory object; knowledge of type and ID is what the factory needs to resolve an object.  Applications may use their own controlled
 * vocabulary for 'rel' to describe content relationships.
 * 
 * @author Alex Vigdor
 *
 */
public class Pointer implements Model{
	private String id;
	private String type;
	private String rel;
	private Map<String,Object> meta;
	
	public Pointer(String type, String id) {
		this.type=type;
		this.id=id;
	}
	
	public Pointer(String type, String id, String rel){
		this.type=type;
		this.id=id;
		this.rel=rel;
	}
	
	public Pointer() {
		
	}

	@Override
	public String toString() {
		try {
			return toJsonString();
		} catch (Exception e) {
			return super.toString();
		}
	}

	@Override
	public void each(ModelConsumer c) {
		c.call("id",id);
		c.call("type",type);
		if(rel!=null) {
			c.call("rel",rel);
		}
		if(meta!=null) {
			meta.forEach(c);
		}
	}

	@Override
	public boolean put(String name, Object value) {
		if(Model.put(this, name, value)) {
			return true;
		}
		if(value!=null) {
			if(meta==null) {
				meta = new LinkedHashMap<>();
			}
			meta.put(name, value);
		}
		else if(meta!=null) {
			meta.remove(name);
		}
		return true;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRel() {
		return rel;
	}

	public void setRel(String rel) {
		this.rel = rel;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pointer other = (Pointer) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	
	public Object get(String property) {
		if(meta==null || !meta.containsKey(property)) {
			throw new MissingPropertyException("No property "+property+" for Pointer "+this);
		}
		return meta.get(property);
	}

	public void set(String property, Object value) {
		put(property,value);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Pointer[] from(Object o) {
		ArrayList<Pointer> pointers = new ArrayList<>();
		if(o instanceof Pointer) {
			pointers.add((Pointer)o);
		}
		else if(o instanceof Map) {
			Pointer p = new Pointer();
			p.putAll((Map<String,Object>)o);
			pointers.add(p);
		}
		else {
			if(o!=null && o.getClass().isArray()) {
				o = Arrays.asList(o);
			}
			if(o instanceof Collection) {
				for(Object om :((Collection)o)) {
					if(om instanceof Pointer) {
						pointers.add((Pointer)om);
					}
					else if(om instanceof Map) {
						Pointer p = new Pointer();
						p.putAll((Map<String,Object>)om);
						pointers.add(p);
					}
					else if(om != null) {
						Pointer p = new Pointer();
						p.setId(om.toString());
						pointers.add(p);
					}
				}
			}
			else if(o!=null){
				Pointer p = new Pointer();
				p.setId(o.toString());
				pointers.add(p);
			}
		}
		return pointers.toArray(new Pointer[0]);
	}
}

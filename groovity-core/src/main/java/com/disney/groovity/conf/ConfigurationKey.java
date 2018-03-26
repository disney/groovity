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
package com.disney.groovity.conf;

import java.util.Arrays;
/**
 * A configuration key represents a logical path that defines the configuration scope (empty path = global scope, otherwise defines the top
 * folder in the source hierarchy at which the configuration applies) as well as a specific property name to apply within that scope.
 *
 * @author Alex Vigdor
 */
public class ConfigurationKey {
	private String[] path = new String[0];
	private String property = null;
	
	public ConfigurationKey(){
		
	}
	
	public ConfigurationKey(String fromString){
		if(fromString.startsWith("/")){
			fromString = fromString.substring(1);
		}
		String[] elems = fromString.split("/");
		if(elems.length > 1){
			int nl = elems.length-1;
			path = new String[nl];
			System.arraycopy(elems, 0, path, 0, nl);
			property = elems[nl];
		}
		else{
			property = fromString;
		}
	}
	
	public String[] getPath() {
		return path;
	}
	public void setPath(String[] path) {
		this.path = path;
	}
	public String getProperty() {
		return property;
	}
	public void setProperty(String property) {
		this.property = property;
	}
	public String toString(){
		StringBuilder builder = new StringBuilder();
		if(path!=null && path.length > 0){
			builder.append("/");
			for(int i=0;i<path.length;i++){
				String elem = path[i];
				builder.append(elem).append("/");
			}
		}
		if(property!=null){
			builder.append(property);
		}
		return builder.toString();
	}
	
	public boolean equals(Object o){
		if(o instanceof ConfigurationKey){
			ConfigurationKey ck = (ConfigurationKey) o;
			if(ck.property.equals(property)){
				return Arrays.equals(ck.path, path);
			}
		}
		return false;
	}
	public int hashCode(){
		int hc = -172938465;
		if(path!=null){
			for(int i=0;i<path.length;i++){
				String elem = path[i];
				hc+=elem.hashCode();
			}
		}
		if(property!=null){
			hc+=property.hashCode();
		}
		return hc;
	}
}

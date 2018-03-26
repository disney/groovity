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
package com.disney.groovity.servlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import com.disney.groovity.servlet.uri.PathTemplate;


/**
 * Encapsulate a Jersey PathTemplate along with a set of supported HTTP methods; 
 * this is used to define the web request matching scope of a single groovity script
 *
 * @author Alex Vigdor
 */
public class PathTemplateMethods implements Comparable<PathTemplateMethods>{
	private final PathTemplate pathTemplate;
	private final Set<String> methods;
	private final String methodsAsString;
	
	@Override
	public String toString() {
		return "PathTemplateMethods [pathTemplate=" + pathTemplate + ", methods=" + methods + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((methods == null) ? 0 : methods.hashCode());
		result = prime * result + ((pathTemplate == null) ? 0 : pathTemplate.hashCode());
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
		PathTemplateMethods other = (PathTemplateMethods) obj;
		if (methods == null) {
			if (other.methods != null)
				return false;
		} else if (!methods.equals(other.methods))
			return false;
		if (pathTemplate == null) {
			if (other.pathTemplate != null)
				return false;
		} else if (!pathTemplate.equals(other.pathTemplate))
			return false;
		return true;
	}

	
	
	public PathTemplateMethods(final PathTemplate pathTemplate, final Set<String> methods){
		this.pathTemplate = pathTemplate;
		this.methods=methods;
		StringBuilder builder = new StringBuilder();
		if(methods!=null){
			ArrayList<String> mtds = new ArrayList<String>(methods);
			Collections.sort(mtds);
			for(int i=0;i<mtds.size();i++){
				if(i>0){
					builder.append(", ");
				}
				builder.append(mtds.get(i));
			}
		}
		methodsAsString = builder.toString();
	}

	public PathTemplate getPathTemplate() {
		return pathTemplate;
	}


	public Set<String> getMethods() {
		return methods;
	}

	public int compareTo(PathTemplateMethods o) {
		int c =  PathTemplate.COMPARATOR.compare(this.pathTemplate, o.pathTemplate);
		if(c==0){
			c = methodsAsString.compareTo(o.methodsAsString);
		}
		return c;
	}
	
}

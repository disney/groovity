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
package com.disney.groovity.source;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
/**
 * Implementation to allow easily wiring in custom sources by constructing a list and passing it in
 * 
 * @author Alex Vigdor
 */
public class ListGroovitySourceLocator extends AbstractGroovitySourceLocator {
	private List<GroovitySource> sourceList;

	public GroovitySource getGroovityScriptSource(final String path) throws IOException {
		for(GroovitySource source:sourceList){
			if(source.getPath().equals(path)){
				return source;
			}
		}
		return new GroovitySource() {
			
			public String getSourceCode() throws IOException {
				return "";
			}
			
			public String getPath() {
				return path;
			}
			
			public long getLastModified() {
				return -1;
			}
			
			public boolean exists() {
				return false;
			}
		};
	}

	@Override
	public Iterator<GroovitySource> iterator() {
		return sourceList.iterator();
	}

	public List<GroovitySource> getSourceList() {
		return sourceList;
	}

	public void setSourceList(List<GroovitySource> sourceList) {
		this.sourceList = sourceList;
	}

}

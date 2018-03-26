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


/**
 * A GroovySourceLocator knows how to list a complete body of groovity scripts from a source location,
 * and accepts listeners to receive runtime updates as managed source files are modified or deleted
 * 
 * @author Alex Vigdor
 *
 */
public interface GroovitySourceLocator extends Iterable<GroovitySource> {
	
	public void addSourceListener(GroovitySourceListener listener);
	
	public void removeSourceListener(GroovitySourceListener listener);
	
	public void destroy();
	
	public void init();
	
	public GroovitySource getGroovityScriptSource(String path) throws IOException;

}

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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;


/**
 * In order to load groovy scripts from the classpath, a manifest file is needed since
 * java resource loading does not support crawling.  The directory on the classpath where
 * groovy scripts are stored must be configured in and contain a trailing slash.
 * 
 * The manifest file should be called "groovity.mf" and should contain a newline delimited
 * list of paths to groovity source files (e.g. /index.grvt, /lib/util.grvt, etc.)
 * 
 * @author Alex Vigdor
 *
 */
public class ClasspathGroovitySourceLocator extends AbstractGroovitySourceLocator {
	private String directory;
	
	public ClasspathGroovitySourceLocator(){
		
	}
	
	public ClasspathGroovitySourceLocator(String directory){
		this.directory=directory;
	}

	public GroovitySource getGroovityScriptSource(String path) throws IOException {
		return new ClasspathGroovitySource(path, this.getClass().getResource(directory.concat(path.substring(1))));
	}

	@Override
	public Iterator<GroovitySource> iterator() {
		ArrayList<GroovitySource> sources = new ArrayList<GroovitySource>();
		InputStream manifest = this.getClass().getResourceAsStream(directory.concat("groovity.mf"));
		Scanner scanner = new Scanner(manifest); 
		scanner.useDelimiter("[\r\n]+");
		while(scanner.hasNext()){
			String path = scanner.next();
			try {
				String rs = directory.concat(path.substring(1));
				//System.out.println("Loading classpath groovy source for path "+path+" "+rs);
				sources.add(new ClasspathGroovitySource(path, this.getClass().getResource(rs)));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return sources.iterator();
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

}

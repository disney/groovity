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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Source locator for retrieving script sources from a local filesystem
 *
 * @author Alex Vigdor
 */
public class FileGroovitySourceLocator extends AbstractGroovitySourceLocator {
	private File directory;
	
	public FileGroovitySourceLocator(){
		
	}
	
	public FileGroovitySourceLocator(File directory){
		this.directory=directory;
	}
	
	@Override
	public Iterator<GroovitySource> iterator() {
		ArrayList<GroovitySource> files = new ArrayList<GroovitySource>();
		grabFilesRecursive(directory, files);
		return files.iterator();
	}
	
	public GroovitySource getGroovityScriptSource(String path){
		File file = new File(directory,path);
		return new FileGroovitySource(file,path);
	}
	
	private void grabFilesRecursive(File file, ArrayList<GroovitySource> out){
		if(file.isDirectory()){
			File[] files = file.listFiles();
			if(files!=null){
				for(File f: files){
					grabFilesRecursive(f, out);
				}
			}
		}
		else{
			if(file.getName().endsWith(GROOVITY_SOURCE_EXTENSION)){
				out.add(new FileGroovitySource(file, file.getPath().substring(directory.getPath().length()).replaceAll("\\\\","/")));
			}
		}
	}

	public File getDirectory() {
		return directory;
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}

}

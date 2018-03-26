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

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Represents a groovity script source located in the filesystem
 *
 * @author Alex Vigdor
 */
public class FileGroovitySource implements GroovitySource {
	private File file;
	private String path;
	private long modified;
	
	public FileGroovitySource(File file, String path){
		this.file=file;
		this.path=path;
		this.modified = file.exists()?file.lastModified():-1;
	}

	public String getPath() {
		return path;
	}

	public String getSourceCode() throws IOException {
		FileInputStream fis = new FileInputStream(file);
		try{
			InputStreamReader reader = new InputStreamReader(fis,"UTF-8");
			char[] buf = new char[4096];
			CharArrayWriter writer = new CharArrayWriter();
			int c = 0;
			while((c=reader.read(buf))!=-1){
				writer.write(buf, 0, c);
			}
			return writer.toString();
		}
		finally{
			fis.close();
		}
	}

	public long getLastModified() {
		return modified;
	}

	public boolean exists() {
		return file.exists();
	}

}

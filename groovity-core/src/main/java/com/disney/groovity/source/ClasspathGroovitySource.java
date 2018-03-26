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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Represents a groovity script located on the classpath
 *
 * @author Alex Vigdor
 */
public class ClasspathGroovitySource implements GroovitySource{
	private String path;
	private URL url;
	private long mod;
	
	public ClasspathGroovitySource(String path, URL url) throws IOException{
		this.url=url;
		this.path=path;
		URLConnection conn = url.openConnection();
		mod = conn.getLastModified();
		conn.getInputStream().close();
	}

	public String getPath() {
		return path;
	}

	public String getSourceCode() throws IOException {
		URLConnection conn = url.openConnection();
		InputStream is = conn.getInputStream();
		try{
			InputStreamReader reader = new InputStreamReader(is,"UTF-8");
			char[] buf = new char[4096];
			CharArrayWriter writer = new CharArrayWriter();
			int c = 0;
			while((c=reader.read(buf))!=-1){
				writer.write(buf, 0, c);
			}
			return writer.toString();
		}
		finally{
			is.close();
		}
	}

	public long getLastModified() {
		return mod;
	}

	public boolean exists() {
		URLConnection conn;
		try {
			conn = url.openConnection();
			conn.getInputStream().close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

}

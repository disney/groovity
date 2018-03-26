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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
/**
 * Loads configuration data from a properties File
 *
 * @author Alex Vigdor
 */
public class PropertiesFileConfigurator extends AbstractPropertiesConfigurator {
	private File file;
	private String encoding="UTF-8";

	public PropertiesFileConfigurator() {
	}
	
	public PropertiesFileConfigurator(File file) {
		this.file=file;
	}
	
	public PropertiesFileConfigurator(File file, String encoding) {
		this.file=file;
		this.encoding=encoding;
	}

	@Override
	protected Properties loadProperties() throws IOException{
		Properties props= new Properties();
		load(this.file,props);
		return props;
	}
	
	protected void load(File file, Properties props) throws IOException{
		if(file.isDirectory()){
			File[] kids = file.listFiles();
			if(kids!=null){
				for(File kid: kids){
					if(kid.isDirectory() || kid.getName().endsWith(".properties")){
						load(kid,props);
					}
				}
			}
		}
		else{
			FileReader reader = new FileReader(file);
			try{
				props.load(reader);
			}
			finally{
				reader.close();
			}
		}
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

}

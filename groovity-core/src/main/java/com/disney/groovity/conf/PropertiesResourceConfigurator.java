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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
/**
 * Loads configuration data from a properties file resource on the classpath
 *
 * @author Alex Vigdor
 */
public class PropertiesResourceConfigurator extends AbstractPropertiesConfigurator {
	private String resource;
	private String encoding = "UTF-8";

	public PropertiesResourceConfigurator() {
	}
	
	public PropertiesResourceConfigurator(String propsResource) {
		this.resource=propsResource;
	}
	
	public PropertiesResourceConfigurator(String propsResource, String encoding) {
		this.resource=propsResource;
		this.encoding=encoding;
	}

	@Override
	protected Properties loadProperties() throws IOException{
		Properties props= new Properties();
		InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
		if(is!=null){
			try{
				InputStreamReader reader = new InputStreamReader(is,encoding);
				props.load(reader);
			}
			finally{
				is.close();
			}
		}
		return props;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
}

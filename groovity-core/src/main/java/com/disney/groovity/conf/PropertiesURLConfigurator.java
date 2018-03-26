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
import java.net.URL;
import java.util.Properties;
/**
 * load properties using java's URL.openStream()
 *
 * @author Alex Vigdor
 */
public class PropertiesURLConfigurator extends AbstractPropertiesConfigurator {
	private URL url;
	private String encoding = "UTF-8";
	
	public PropertiesURLConfigurator() {
	}
	public PropertiesURLConfigurator(URL url) {
		this.url=url;
	}
	
	public PropertiesURLConfigurator(URL url, String encoding) {
		this.url=url;
		this.encoding=encoding;
	}
	@Override
	protected Properties loadProperties() throws IOException {
		Properties props= new Properties();
		InputStream is = url.openStream();
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

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

}

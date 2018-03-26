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
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
/**
 * Represents a groovity source file located at an HTTP URL
 *
 * @author Alex Vigdor
 */
public class HttpGroovitySource implements GroovitySource{
	private static Log log = LogFactory.getLog(HttpGroovitySource.class);
	private String path;
	private long modified = -1;
	private URI uri; 
	HttpGroovitySourceLocator locator;
	
	public HttpGroovitySource(URI uri, String path, long lastModified, HttpGroovitySourceLocator locator){	
		this.uri = uri;
		this.path = path;
		this.modified = lastModified;
		this.locator = locator;
	}

	public URI getUri() {
		return this.uri;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public long getLastModified() {
		return this.modified;
	}
	
	public boolean exists(){
		CloseableHttpResponse response = null;
		boolean doesExist = true;
		try {
			HttpHead headReq = new HttpHead(uri);
			CloseableHttpClient client = this.locator.getClient();
			response = client.execute(headReq);	
			try{
				StatusLine sl = response.getStatusLine();
				doesExist = (sl.getStatusCode()!=404);	
				HttpEntity entity = response.getEntity();
				if(entity!=null)
				{					
					EntityUtils.consume(entity);
				}
			}
			finally{
				response.close();
			}
				
		} catch (Exception e) {
			log.warn("Error in HttpGroovySource.exists(), presumed true to be safe", e );
		} 		
		
		return doesExist;
	}
		
	
	public String getSourceCode() throws IOException {
		return this.locator.getBody(uri);		
	}
}

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
package com.disney.groovity.servlet.cors;

import java.util.Arrays;
import java.util.List;
/**
 * Represents all the options required in order to properly process the CORS (Cross-Origin Resource Sharing) protocol,
 * including specifying allowed origins, supported headers, methods and credentials, exposed headers
 * and the maximum age of preflight requests
 *
 * @author Alex Vigdor
 */
public class CORSConfiguration {
	private static final List<String> simpleHeaders = Arrays.asList("Accept","Accept-Language","Content-Language","Origin","Content-Type");
	private boolean anyOriginAllowed = false;
	private boolean anyHeaderAllowed = false;
	private List<CORSOrigin> allowedOrigins;
	private List<String> supportedMethods;
	private List<String> supportedHeaders;
	private List<String> exposedHeaders;
	private boolean supportingCredentials;
	private int maxAgeSeconds=-1;
	private String exposedHeadersString;
	
	public List<CORSOrigin> getAllowedOrigins() {
		return allowedOrigins;
	}
	public void setAllowedOrigins(List<CORSOrigin> allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}
	public List<String> getSupportedMethods() {
		return supportedMethods;
	}
	public void setSupportedMethods(List<String> supportedMethods) {
		this.supportedMethods = supportedMethods;
	}
	public List<String> getSupportedHeaders() {
		return supportedHeaders;
	}
	public void setSupportedHeaders(List<String> supportedHeaders) {
		this.supportedHeaders = supportedHeaders;
	}
	public List<String> getExposedHeaders() {
		return exposedHeaders;
	}
	public void setExposedHeaders(List<String> exposedHeaders) {
		this.exposedHeaders = exposedHeaders;
		this.exposedHeadersString=null;
	}
	public String getExposedHeadersString(){
		if(exposedHeadersString==null){
			StringBuilder builder = new StringBuilder();
			for(int i=0;i<exposedHeaders.size();i++){
				if(i>0){
					builder.append(", ");
				}
				builder.append(exposedHeaders.get(i));
			}
			this.exposedHeadersString = builder.toString();
		}
		return exposedHeadersString;
	}
	public boolean isSupportingCredentials() {
		return supportingCredentials;
	}
	public void setSupportingCredentials(boolean supportsCredentials) {
		this.supportingCredentials = supportsCredentials;
	}
	public int getMaxAgeSeconds() {
		return maxAgeSeconds;
	}
	public void setMaxAgeSeconds(int maxAgeSeconds) {
		this.maxAgeSeconds = maxAgeSeconds;
	}
	public boolean isAnyOriginAllowed() {
		return anyOriginAllowed;
	}
	public void setAnyOriginAllowed(boolean anyOriginAllowed) {
		this.anyOriginAllowed = anyOriginAllowed;
	}
	
	public boolean isOriginAllowed(CORSOrigin origin){
		if(anyOriginAllowed){
			return true;
		}
		for(CORSOrigin co: allowedOrigins){
			if(co.matches(origin)){
				return true;
			}
		}
		return false;
	}
	
	public boolean areHeadersSupported(List<String> requestHeaders){
		if(anyHeaderAllowed){
			//System.out.println("Any header allowed!");
			return true;
		}
		for(int r = 0; r< requestHeaders.size(); r++){
			boolean match = false;
			String rh = requestHeaders.get(r);
			for(int s=0;s<simpleHeaders.size();s++){
				String sh = simpleHeaders.get(s);
				if(sh.equalsIgnoreCase(rh)){
					match=true;
					break;
				}
			}
			if(!match){
				for(int s=0;s<supportedHeaders.size();s++){
					String sh = supportedHeaders.get(s);
					if(sh.equalsIgnoreCase(rh)){
						match=true;
						break;
					}
				}
			}
			//System.out.println("Match for "+rh+" = "+match);
			if(!match){
				return false;
			}
		}
		return true;
	}
	public boolean isAnyHeaderAllowed() {
		return anyHeaderAllowed;
	}
	public void setAnyHeaderAllowed(boolean anyHeaderAllowed) {
		this.anyHeaderAllowed = anyHeaderAllowed;
	}
}

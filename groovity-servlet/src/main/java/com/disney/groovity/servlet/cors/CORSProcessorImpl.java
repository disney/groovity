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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 * A proper implementation of the CORS protocol that sets the appropriate response
 * headers based on the request headers and the provided CORSConfiguration
 *
 * @author Alex Vigdor
 */
public class CORSProcessorImpl implements CORSProcessor {
	private static final String OPTIONS_METHOD = "OPTIONS";
	private static final String ORIGIN_HEADER = "Origin";
	private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
	private static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
	private static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
	private static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
	private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	
	private CORSConfiguration configuration;
	
	public void process(HttpServletRequest request, HttpServletResponse response) {
		//1) request MUST have an "Origin" header to trigger any CORS processing
		String requestOrigin = request.getHeader(ORIGIN_HEADER);
		//System.out.println("Origin header is "+requestOrigin+" for "+request.getMethod());
		if(requestOrigin!=null && requestOrigin.trim().length()>0){
			//2) Origin must be allowed in order to continue
			if(configuration.isOriginAllowed(new CORSOrigin(requestOrigin))){
				if(OPTIONS_METHOD.equals(request.getMethod())){
					//preflight request
					String rm = request.getHeader(ACCESS_CONTROL_REQUEST_METHOD);
					//System.out.println("OPTIONS request method "+rm);
					// 3) Target method must be supported
					if(rm!=null && configuration.getSupportedMethods().contains(rm)){
						String rh=request.getHeader(ACCESS_CONTROL_REQUEST_HEADERS);
						if(rh!=null && rh.trim().length() > 0){
							//4) If request headers are specified they must be validated
							//System.out.println("Request headers "+rh);
							if(!configuration.areHeadersSupported(Arrays.asList(rh.split(",\\s*")))){
								return;
							}
							response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, rh);
						}
						if(configuration.isSupportingCredentials()){
							response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
						}
						response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
						if(configuration.getMaxAgeSeconds()>0){
							response.setHeader(ACCESS_CONTROL_MAX_AGE, String.valueOf(configuration.getMaxAgeSeconds()));
						}
						response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, rm);
					}
				}
				else{
					//standard request
					if(configuration.isSupportingCredentials()){
						response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
					}
					response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
					if(configuration.getExposedHeaders() !=null && configuration.getExposedHeaders().size()>0){
						response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, configuration.getExposedHeadersString());
					}
				}
			}
		}
	}

	public CORSConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(CORSConfiguration configuration) {
		this.configuration = configuration;
	}

}

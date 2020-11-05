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
package com.disney.http.auth;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

/**
 * Parse or format a Basic Authorization header
 * 
 * @author Alex Vigdor
 */
public class BasicAuthorization implements AuthConstants {
	private String username;
	private String password;
	
	public BasicAuthorization(){
		
	}
	
	public BasicAuthorization(String authorizationHeader){
		if(authorizationHeader.startsWith(BASIC)){
			authorizationHeader = authorizationHeader.substring(BASIC.length()+1);
		}
		String creds;
		try {
			creds = new String(Base64.getDecoder().decode(authorizationHeader),"UTF-8");
			int pos = creds.indexOf(":");
			if(pos > 0){
				username = creds.substring(0,pos);
				password = creds.substring(pos+1);
			}
		} catch (UnsupportedEncodingException | IllegalArgumentException e) {
		}
		
		if(username==null ||password==null){
			throw new IllegalArgumentException(ERROR_MISSING_CREDENTIALS);
		}
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder(BASIC);
		try {
			sb.append(" ").append(Base64.getEncoder().encodeToString((username+":"+password).getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
		}
		return sb.toString();
	}
}

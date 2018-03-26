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
package com.disney.http.auth.server.digest;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
/**
 * A simple password digester that relies on user names and passwords stored in a Map
 *
 * @author Alex Vigdor
 */
public class MapPasswordDigester implements PasswordDigester{
	private Map<String,String> passwords;
	
	public MapPasswordDigester(){
		
	}
	
	public MapPasswordDigester(Map<String,String> passwords){
		this.passwords=passwords;
	}

	@Override
	public byte[] digest(String username, String realm) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		String password = passwords.get(username);
		if(password!=null){
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(username.getBytes("UTF-8"));
			md5.update((byte)':');
			if(realm!=null){
				md5.update(realm.getBytes("UTF-8"));
			}
			md5.update((byte)':');
			md5.update(password.getBytes("UTF-8"));
			return md5.digest();
		}
		return null;
	}

	public Map<String,String> getPasswords() {
		return passwords;
	}

	public void setPasswords(Map<String,String> passwords) {
		this.passwords = passwords;
	}
	

}

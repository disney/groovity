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
package com.disney.groovity.tags;

import java.util.Map;
import java.util.Optional;

import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;

import groovy.lang.Closure;
/**
 * Add authentication credentials to an HTTP request
 * <p>
 * credentials( <ul>	
 *	<li><b>user</b>: 
 *	http username,</li>	
 *	<li><b>pass</b>: 
 *	http password,</li>
 *	</ul>{});
 *	
 *	<p>Sample
 *	<pre>
 *	def result = http(url:someUrl,{
 *		credentials(user:'admin',pass:'mypass')
 *	}
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(info = "Add authentication credentials to an HTTP request", 
	 attrs = {
		@Attr(name = "user", required = true, info = "http username"),
		@Attr(name = "pass", required = true, info = "http password"),
	 },
	 sample="def result = http(url:someUrl,{\n" + 
	 		"	credentials(user:'admin',pass:'mypass')\n" + 
	 		"}"
)
public class Credentials implements Taggable {
	final static String CREDENTIALS_BINDING = INTERNAL_BINDING_PREFIX+"Optional.Credentials";
	
	@SuppressWarnings({"rawtypes","unchecked"})
	public static void acceptCredentials(Map variables){
		variables.put(CREDENTIALS_BINDING, Optional.empty());
	}
	
	@SuppressWarnings({"rawtypes","unchecked"})
	public static Optional<UserPass> resolveCredentials(Map variables){
		return (Optional<UserPass>)variables.remove(CREDENTIALS_BINDING);
	}

	@SuppressWarnings({"rawtypes","unchecked"})
	public Object tag(Map attributes, Closure body) throws Exception {
		Map variables = getScriptHelper(body).getBinding().getVariables();
		Optional<UserPass> creds = (Optional<UserPass>) variables.get(CREDENTIALS_BINDING);
		if(creds == null){
			throw new RuntimeException("Credentials can only be used inside an http or ws request");
		}
		String user = resolve(attributes, "user", String.class);
		Object pass = resolve(attributes, "pass");
		variables.put(CREDENTIALS_BINDING, Optional.of(new UserPass(user,pass)));
		return null;
	}
	
	public static class UserPass{
		private String user;
		private char[] pass;
		public UserPass(){}
		public UserPass(String user, Object pass){
			if(user==null){
				throw new RuntimeException("user cannot be null");
			}
			this.user = user;
			if(pass==null){
				this.pass = new char[0];
			}
			else if(pass instanceof char[]){
				this.pass=(char[])pass;
			}
			else{
				if(pass instanceof byte[]){
					pass = new String((byte[])pass);
				}
				this.pass = pass.toString().toCharArray();
			}
		}
		public String getUser() {
			return user;
		}
		public void setUser(String user) {
			this.user = user;
		}
		public char[] getPass() {
			return pass;
		}
		public void setPass(char[] pass) {
			this.pass = pass;
		}
	}

}

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
package com.disney.http.auth.server;

import java.security.Principal;
/**
 * A simple Principal implementation to identify a user who has successfully authenticated via
 * basic, digest or signature auth
 *
 * @author Alex Vigdor
 */
public final class AuthenticatedPrincipal implements java.security.Principal {
	private final String name;
	
	public AuthenticatedPrincipal(String name) {
		this.name=name;
	}
	@Override
	public String getName() {
		return name;
	}

	@Override
	public int hashCode(){
		return 963 + name.hashCode();
	}

	@Override
	public boolean equals(Object o){
		if(o==this){
			return true;
		}
		if(o instanceof Principal){
			return ((Principal)o).getName().equals(getName());
		}
		return false;
	}

	@Override
	public String toString(){
		return name;
	}
}
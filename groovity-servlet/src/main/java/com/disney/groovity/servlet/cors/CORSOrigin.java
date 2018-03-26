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

import java.net.URI;

/**
 * Specification for Origin matching in CORS requests.  A CORSOrigin MUST specify a domain name;
 * it MAY specify a scheme (if not http or https will be supported), and it MAY specify a port (otherwise any port is supported).
 * 
 * CORS Origin patterns may use wildcards to allow application running on any subdomains or ports, e.g. *.myco.co:*
 * 
 * @author Alex Vigdor
 */
public class CORSOrigin {
	private String scheme;
	private Integer port = 80;
	private String domain;
	private boolean allowingSubdomains = false;
	private boolean allowingAnyPort = false;
	
	public CORSOrigin(){
		
	}
	
	public CORSOrigin(String originSpec){
		int ps = originSpec.indexOf("://");
		if(ps>0){
			this.scheme = originSpec.substring(0,ps);
			originSpec = originSpec.substring(ps+3);
		}
		int cp = originSpec.indexOf(":");
		if(cp>0){
			String portSpec = originSpec.substring(cp+1);
			if(portSpec.equals("*")){
				allowingAnyPort=true;
			}
			else{
				this.port= Integer.parseInt(portSpec);
			}
			originSpec = originSpec.substring(0,cp);
		}
		if(originSpec.startsWith("*.")){
			this.allowingSubdomains=true;
			this.domain = originSpec.substring(2);
		}
		else{
			allowingSubdomains=false;
			this.domain = originSpec;
		}
	}

	public String getScheme() {
		return scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public boolean isAllowingSubdomains() {
		return allowingSubdomains;
	}

	public void setAllowingSubdomains(boolean allowingSubdomains) {
		this.allowingSubdomains = allowingSubdomains;
	}
	
	/**
	 * Test whether an incoming Origin header matches this Origin spec
	 * @param origin
	 * @return
	 */
	public boolean matches(CORSOrigin origin){
		if(this.scheme!=null && !this.scheme.equalsIgnoreCase(origin.getScheme())){
			return false;
		}
		if(!allowingAnyPort && this.port!=null){
			int op = origin.getPort()==-1?80:origin.getPort();
			if(op!=this.port){
				return false;
			}
		}
		if(allowingSubdomains){
			return origin.getDomain().endsWith(domain);
		}
		else{
			return origin.getDomain().equals(domain);
		}
	}

	public boolean isAllowingAnyPort() {
		return allowingAnyPort;
	}

	public void setAllowingAnyPort(boolean allowingAnyPort) {
		this.allowingAnyPort = allowingAnyPort;
	}
	
}

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
package com.disney.groovity.servlet.auth;

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.disney.groovity.servlet.GroovityScriptView;
import com.disney.groovity.servlet.GroovityScriptViewFactory;
import com.disney.http.auth.server.Verifier;
/**
 * A loader for Policy Verifiers - looks up another view by path and defers to its verifier
 * 
 * @author Alex Vigdor
 *
 */
public class ViewPolicyLoader implements Callable<Verifier>{
	private static final Log log = LogFactory.getLog(ViewPolicyLoader.class);
	private GroovityScriptViewFactory viewResolver;
	private String location;
	
	public Verifier call() throws Exception {
		GroovityScriptView view = (GroovityScriptView) getViewResolver().getViewByName(location);
		if(view!=null){
			return view.getVerifier();
		}
		log.warn("No view found for auth policy at "+location);
		return null;
	}

	public GroovityScriptViewFactory getViewResolver() {
		return viewResolver;
	}

	public void setViewResolver(GroovityScriptViewFactory viewResolver) {
		this.viewResolver = viewResolver;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
	

}

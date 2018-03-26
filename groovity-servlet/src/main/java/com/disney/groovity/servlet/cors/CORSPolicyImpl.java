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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.disney.groovity.servlet.GroovityScriptView;
import com.disney.groovity.servlet.GroovityScriptViewFactory;
/**
 * A CORSProcessor that allows one script to delegate to another to act as a CORS policy.
 *
 * @author Alex Vigdor
 */
public class CORSPolicyImpl implements CORSProcessor{
	private static final Log log = LogFactory.getLog(CORSPolicyImpl.class);
	private final GroovityScriptViewFactory viewResolver;
	private final String location;
	private final AtomicBoolean warned = new AtomicBoolean(false);
	
	public CORSPolicyImpl(GroovityScriptViewFactory viewResolver, String location){
		this.viewResolver=viewResolver;
		this.location=location;
	}

	public void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		GroovityScriptView gv = (GroovityScriptView) viewResolver.getViewByName(location);
		if(gv!=null){
			CORSProcessor cp = gv.getCORSProcessor();
			if(cp!=null){
				cp.process(request, response);
			}
			else if(warned.compareAndSet(false, true)){
				log.warn("No CORS processor found at policy location "+location);
			}
		}
		else if(warned.compareAndSet(false, true)){
			log.warn("No GroovyView found for CORS policy location "+location);
		}
	}

}

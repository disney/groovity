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
package com.disney.groovity.servlet;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.Session;

import com.disney.groovity.ArgsLookup;
import com.disney.groovity.util.WebSocket;

/**
 * An ArgsLookup implementation that resolves arguments from an HttpServletRequest bound to "request",
 * allowing a fallback to a chained lookup IF the request is not present in the binding (for code meant
 * to run in multiple contexts)
 * 
 * @author Alex Vigdor
 *
 */
public class RequestArgsLookup extends ArgsLookup {
	
	public RequestArgsLookup(){
		
	}
	public RequestArgsLookup(ArgsLookup chained){
		super(chained);
	}
	public Object lookup(@SuppressWarnings("rawtypes") Map binding, String name, Optional<Object> defaultValue){
		Object r = binding.get("request");
		if(r!=null && r instanceof HttpServletRequest){
			HttpServletRequest request = (HttpServletRequest)r;
			String[] vals = request.getParameterValues(name);
			if(vals!=null && vals.length > 0){
				if(vals.length==1){
					return vals[0];
				}
				else{
					return vals;
				}
			}
		}
		else{ 
			Object s = binding.get("socket");
			if(s!=null && s instanceof WebSocket){
				Session session = ((WebSocket) s).getSession();
				List<String> vals = session.getRequestParameterMap().get(name);
				if(vals!=null && !vals.isEmpty()){
					if(vals.size()==1){
						return vals.get(0);
					}
					return vals;
				}
			}
			else if(chained!=null){
				return chained.lookup(binding, name, defaultValue);
			}
		}
		return null;
	}
}

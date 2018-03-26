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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.disney.groovity.servlet.GroovityScriptViewFactory;

import groovy.lang.Closure;
/**
 * Responsible for converting a declared static cors map on a groovity script into a configured
 * CORSProcessor to be attached to a GroovityScriptView.
 *
 * @author Alex Vigdor
 */
public class CORSFactory {
	private GroovityScriptViewFactory viewResolver;
	
	@SuppressWarnings("rawtypes")
	public CORSProcessor createProcessor(Map<String,Object> configMap){
		CORSConfiguration config = new CORSConfiguration();
		//POLICY
		String policy = resolve(configMap, "policy", String.class);
		if(policy!=null){
			return new CORSPolicyImpl(viewResolver, policy.toString());
		}
		else{
			// ORIGINS
			List origins = resolve(configMap,"origins",List.class);
			if(origins!=null && origins.size()>0){
				List<CORSOrigin> corsOrigins = new ArrayList<CORSOrigin>();
				for(Object or: origins){
					String ors = or.toString();
					if("*".equals(ors)){
						config.setAnyOriginAllowed(true);
					}
					else{
						corsOrigins.add(new CORSOrigin(ors));
					}
				}
				config.setAllowedOrigins(corsOrigins);
			}
			else{
				config.setAnyOriginAllowed(true);
			}
			//METHODS
			List methods = resolve(configMap,"methods",List.class);
			if(methods!=null && methods.size()>0){
				List<String> corsMethods = new ArrayList<String>();
				for(Object m: methods){
					corsMethods.add(m.toString());
				}
				config.setSupportedMethods(corsMethods);
			}
			else{
				config.setSupportedMethods(Arrays.asList("GET","POST","HEAD","OPTIONS"));
			}
			//HEADERS
			List headers = resolve(configMap,"headers",List.class);
			if(headers!=null && headers.size()>0){
				List<String> corsHeaders = new ArrayList<String>();
				for(Object h: headers){
					String hs = h.toString();
					if("*".equals(hs)){
						config.setAnyHeaderAllowed(true);
					}
					else{
						corsHeaders.add(hs);
					}
				}
				config.setSupportedHeaders(corsHeaders);
			}
			else{
				config.setAnyHeaderAllowed(true);
			}
			//EXPOSED
			List exposed = resolve(configMap,"exposed",List.class);
			if(exposed!=null && exposed.size()>0){
				List<String> corsExposed = new ArrayList<String>();
				for(Object e: exposed){
					corsExposed.add(e.toString());
				}
				config.setExposedHeaders(corsExposed);
			}
			else{
				config.setExposedHeaders(new ArrayList<String>(0));
			}
			//CREDENTIALS
			Object credentials = resolve(configMap,"credentials",Object.class);
			if(credentials!=null){
				if(!(credentials instanceof Boolean)){
					credentials = Boolean.valueOf(credentials.toString());
				}
				config.setSupportingCredentials((Boolean)credentials);
			}
			//MAXAGE
			Object maxAge = resolve(configMap,"maxAge",Object.class);
			if(maxAge!=null){
				if(!(maxAge instanceof Integer)){
					maxAge = Integer.valueOf(maxAge.toString());
				}
				config.setMaxAgeSeconds((Integer)maxAge);
			}
			
			CORSProcessorImpl processor = new CORSProcessorImpl();
			processor.setConfiguration(config);
			return processor;
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> T resolve(Map map, String name, Class<T> type){
		Object o = map.get(name);
		if(o==null){
			return null;
		}
		if(o instanceof Closure){
			o = ((Closure)o).call();
			if(o==null){
				return null;
			}
		}
		if(String.class.equals(type)){
			return (T) o.toString();
		}
		if(List.class.equals(type)){
			if(!(o instanceof List) && !(o instanceof String)){
				// could be single GString value ...
				o = o.toString();
			}
			if(o instanceof String){
				o = Arrays.asList(o);
			}
		}
		return type.cast(o);
	}

	public GroovityScriptViewFactory getViewResolver() {
		return viewResolver;
	}

	public void setViewResolver(GroovityScriptViewFactory viewResolver) {
		this.viewResolver = viewResolver;
	}
}

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
package com.disney.groovity.data;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityObserver;

import groovy.lang.Script;
/**
 * An in-memory registry of known data types for rapid runtime checks
 * 
 * @author Alex Vigdor
 *
 */
public class DataTypeRegistry implements GroovityObserver {
	Set<String> knownDataTypes = ConcurrentHashMap.newKeySet();
	
	@Override
	public void scriptStart(Groovity groovity, String scriptPath, Class<Script> scriptClass) {
		String tn = getTypeName(scriptPath);
		if(tn!=null) {
			knownDataTypes.add(tn);
		}
	}

	private String getTypeName(String scriptPath) {
		if(scriptPath.startsWith("/data/types/")) {
			return scriptPath.substring(12);
		}
		return null;
	}
	
	@Override
	public void scriptDestroy(Groovity groovity, String scriptPath, Class<Script> scriptClass) {
		String tn = getTypeName(scriptPath);
		if(tn!=null) {
			knownDataTypes.remove(tn);
		}
	}
	
	public boolean isKnownType(String type) {
		return knownDataTypes.contains(type);
	}

	@Override
	public void destroy(Groovity groovity) {
	}

}

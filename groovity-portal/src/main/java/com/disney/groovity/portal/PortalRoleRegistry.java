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
package com.disney.groovity.portal;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityObserver;

import groovy.lang.Script;

/**
 * Capture and register roles declared in static roles and portal fields
 * 
 * @author Alex Vigdor
 *
 */
public class PortalRoleRegistry {
	Map<String, Collection<String>> registry = new ConcurrentHashMap<>();

	public PortalRoleRegistry(Groovity groovity) {
		groovity.addObserver(new PortalRolesObserver());
		groovity.addObserver(new RolesObserver());
	}

	public Map<String, Collection<String>> getEntries() {
		return registry;
	}

	private class PortalRolesObserver implements GroovityObserver.Field {

		@Override
		public String getField() {
			return "portal";
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public void scriptStart(Groovity groovity, String scriptPath, Class<Script> scriptClass, Object fieldValue) {
			Map portal = (Map) fieldValue;
			Object roles = portal.get("roles");
			if (roles != null && roles instanceof Collection) {
				String path = (String) portal.get("path");
				if (path == null) {
					path = scriptPath;
				}
				registry.put(path, new CopyOnWriteArrayList<>((Collection) roles));
			}
		}

		@SuppressWarnings({ "rawtypes" })
		@Override
		public void scriptDestroy(Groovity groovity, String scriptPath, Class<Script> scriptClass, Object fieldValue) {
			Map portal = (Map) fieldValue;
			String path = (String) portal.get("path");
			if (path == null) {
				path = scriptPath;
			}
			registry.remove(path);
		}

		@Override
		public void destroy(Groovity groovity) {
		}

	}

	private class RolesObserver implements GroovityObserver.Field {

		@Override
		public String getField() {
			return "roles";
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void scriptStart(Groovity groovity, String scriptPath, Class<Script> scriptClass, Object fieldValue) {
			Collection roles = (Collection) fieldValue;
			registry.put(scriptPath, new CopyOnWriteArrayList<>(roles));
		}

		@Override
		public void scriptDestroy(Groovity groovity, String scriptPath, Class<Script> scriptClass, Object fieldValue) {
			registry.remove(scriptPath);
		}

		@Override
		public void destroy(Groovity groovity) {
		}

	}
}

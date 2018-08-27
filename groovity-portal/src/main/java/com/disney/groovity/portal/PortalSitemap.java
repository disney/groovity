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
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityObserver;

import groovy.lang.Script;

/**
 * Observe a groovity in order to build and maintain a logical portal sitemap to
 * be used in rendering the UI
 * 
 * @author Alex Vigdor
 */
public class PortalSitemap {
	@SuppressWarnings("rawtypes")
	Map<String, Map> scripts = new ConcurrentHashMap<>();
	@SuppressWarnings("rawtypes")
	Map<String, Collection<Map>> folders = new ConcurrentHashMap<>();

	public PortalSitemap(Groovity groovity) {
		groovity.addObserver(new Observer());
	}

	@SuppressWarnings("rawtypes")
	public Collection<Map> getEntries() {
		return scripts.values();
	}

	@SuppressWarnings("rawtypes")
	public Map<String, Collection<Map>> getFolders() {
		return folders;
	}

	private class Observer implements GroovityObserver.Field {
		@Override
		public String getField() {
			return "portal";
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void scriptStart(Groovity groovity, String scriptPath, Class<Script> scriptClass, Object fieldValue) {
			Map portal = (Map) fieldValue;
			Map<String, String> scriptMap = new ConcurrentHashMap<>(portal);
			scriptMap.put("scriptPath", scriptPath);
			if (!scriptMap.containsKey("path")) {
				scriptMap.put("path", scriptPath);
			}
			scripts.put(scriptPath, scriptMap);
			String path = scriptMap.get("path");
			String folder = path.substring(0, path.lastIndexOf("/"));
			Collection<Map> entries = folders.get(folder);
			if (entries == null) {
				entries = new ConcurrentSkipListSet<Map>(new Comparator<Map>() {

					@Override
					public int compare(Map o1, Map o2) {
						Integer d1 = (Integer) o1.get("order");
						Integer d2 = (Integer) o2.get("order");
						if (d1 != null) {
							if (d2 == null) {
								return -1;
							}
							int diff = d1.intValue() - d2.intValue();
							if (diff != 0) {
								return diff;
							}
						} else if (d2 != null) {
							return 1;
						}
						String p1 = (String) o1.get("path");
						String p2 = (String) o2.get("path");
						return p1.compareTo(p2);
					}
				});
				folders.put(folder, entries);
			}
			entries.add(scriptMap);
		}

		@SuppressWarnings({ "rawtypes" })
		@Override
		public void scriptDestroy(Groovity groovity, String scriptPath, Class<Script> scriptClass, Object fieldValue) {
			scripts.remove(scriptPath);
			Iterator<Entry<String, Collection<Map>>> folderIterator = folders.entrySet().iterator();
			while (folderIterator.hasNext()) {
				Entry<String, Collection<Map>> folder = folderIterator.next();
				Iterator<Map> entries = folder.getValue().iterator();
				while (entries.hasNext()) {
					Map entry = entries.next();
					if (scriptPath.equals(entry.get("scriptPath"))) {
						entries.remove();
					}
				}
				if (folder.getValue().isEmpty()) {
					folderIterator.remove();
				}
			}
		}
		
		@Override
		public void destroy(Groovity groovity) {
		}

	}
}

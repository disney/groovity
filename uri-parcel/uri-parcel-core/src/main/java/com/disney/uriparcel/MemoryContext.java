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
package com.disney.uriparcel;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Utility class for managing a static in-memory data context, primarily intended to support unit tests that want
 * avoid real network calls using "mem:" URIs
 *
 * @author Alex Vigdor
 */
public class MemoryContext {
	public static final String MEMORY_PROTOCOL = "mem";
	private static final ConcurrentHashMap<URI, MemoryPayload> context = new ConcurrentHashMap<URI, MemoryPayload>();
	public static void put(URI uri,MemoryPayload value){
		checkUri(uri);
		context.put(uri, value);
	}
	public static MemoryPayload remove(URI uri){
		checkUri(uri);
		return context.remove(uri);
	}
	public static boolean contains(URI uri){
		checkUri(uri);
		return context.containsKey(uri);
	}
	public static MemoryPayload get(URI uri){
		checkUri(uri);
		return context.get(uri);
	}
	private static void checkUri(URI uri){
		if(!MEMORY_PROTOCOL.equals(uri.getScheme())){
			throw new IllegalArgumentException("MemoryContext only accepts mem: URLs, not "+uri);
		}
	}
}

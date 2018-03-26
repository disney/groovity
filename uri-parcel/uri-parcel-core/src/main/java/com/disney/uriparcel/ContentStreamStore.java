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

import java.io.IOException;
import java.util.Map;
/**
 * Core extension API for ContentStreamStores, which are components able to load and store binary streams and associated stream metadata for 
 * some URIs; stream stores are prioritized to ensure predictable fallback order, each store must respond false
 * when asked to load or store an unrecognized URI scheme to allow fallbacks to happen.
 *<p>
 * ContentStreamStores are discovered automatically using the Java ServiceLoader mechanism; in order to wire in a custom implementation
 * you should package it in a jar file with a manifest at "/META-INF/services/com.disney.uriparcel.ContentStreamStore" that lists the 
 * fully qualified class name(s) of your implementation.
 * 
 * @author Alex Vigdor
 */
public interface ContentStreamStore extends Comparable<ContentStreamStore>{
	//given a ContentStream with only a URI, return true if content is considered loaded or valid, false if unable to process
	boolean load(ContentStream uriContent, Map<?,?> config) throws IOException;
	//given a fully populated ContentStream, return true if content is considered stored at the designated URI, false if unable to process
	boolean store(ContentStream uriContent, Map<?,?> config) throws IOException;
	//determines order of attempts; more specific loaders, e.g. specific protocols,
	//should take higher priority (e.g. 100), while more general-purpose loaders 
	//like url should take lower priority (e.g. 1)
	int getPriority();
}

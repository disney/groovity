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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
/**
 * Core extension API for ContentValueHandlers, which are components able to provide serialization or deserialization 
 * between binary streams and java classes.  Handlers are prioritized for predictable fallback order.  Handlers must 
 * return false or null for unsupported classes or content streams in order to allow graceful fallback.
 * <p>
 * ContentValueHandlers are discovered automatically using the Java ServiceLoader mechanism; in order to wire in a custom implementation
 * you should package it in a jar file with a manifest at "/META-INF/services/com.disney.uriparcel.ContentValueHandler" that lists the 
 * fully qualified class name(s) of your implementation.
 * 
 * @author Alex Vigdor
 */
public interface ContentValueHandler extends Comparable<ContentValueHandler> {
	//return Object if content is considered loaded or valid, null if unable to process
	public <T> T load(InputStream stream, String contentType, Class<T> valueClass, Map<?,?> config) throws Exception;
	//return true if object was able to be stored successfully, false if unable to process
	public boolean store(OutputStream stream, String contentType, Object value, Map<?,?> config) throws Exception;
	//return true if this handler is prepared to marshal/unmarshal objects of the provided class from streams of the optional content type
	public boolean isSupported(Class<?> valueClass, String contentType);
	//determines order of attempts; more specific loaders, e.g. specific classes,
	//should take higher priority (e.g. 100), while more general-purpose loaders 
	//like json should take lower priority (e.g. 1)
	int getPriority();
}

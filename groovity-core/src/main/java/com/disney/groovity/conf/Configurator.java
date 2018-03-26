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
package com.disney.groovity.conf;

import java.util.Set;
import java.util.function.BiConsumer;
/**
 * API for a configuration provider; this is an extension point to allow plugging in of custom configurators, for example
 * that might pull from a shared database or configuration service.  Provided implementations support System properties lookup, as well
 * as properties file parsing.
 *
 * @author Alex Vigdor
 */
public interface Configurator {
	/**
	 * For each key in the provided set, implementations should pass a known value applying to the given sourcePath to the BiConsumer, or do nothing if 
	 * the key is unknown or unconfigured
	 * 
	 * @param sourcePath
	 * @param propertyNames
	 * @param propertySetter
	 */
	public void configure(String sourcePath, Set<String> propertyNames, BiConsumer<String, String> propertySetter);
	public void init();
	public void destroy();
}

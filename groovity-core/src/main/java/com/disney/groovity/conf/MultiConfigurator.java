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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
/**
 * MultiConfigurator is used to chain mutliple configurators together, for example to provide fallback lookups
 * from System properties to properties files, etc.
 *
 * @author Alex Vigdor
 */
public class MultiConfigurator implements Configurator {
	private Collection<Configurator> configurators;
	
	public MultiConfigurator(){
		
	}
	
	public MultiConfigurator(Collection<Configurator> configurators){
		this.configurators=configurators;
	}

	public void configure(String sourcePath, Set<String> propertyNames, BiConsumer<String, String> propertySetter){
		//let's make a copy so intermediate stages don't have temporary visibility
		Map<String, String> propertiesCopy = new HashMap<>();
		for(Configurator c: configurators){
			c.configure(sourcePath, propertyNames, propertiesCopy::put);
		}
		//put the copy back
		propertiesCopy.entrySet().forEach(entry -> { propertySetter.accept(entry.getKey(), entry.getValue());});
	}

	public void init() {
		for(Configurator c: configurators){
			c.init();
		}
	}

	public void destroy() {
		for(Configurator c: configurators){
			c.destroy();
		}
	}

	public Collection<Configurator> getConfigurators() {
		return configurators;
	}

	public void setConfigurators(Collection<Configurator> configurators) {
		this.configurators = configurators;
	}

}

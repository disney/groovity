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
package com.disney.groovity;

import java.util.Map;

/**
 * A simple BindingDecorator implementation that copies some default set of variables from a map and places them
 * in the binding.
 * 
 * @author Alex Vigdor
 *
 */
public class BindingMapDecorator extends BindingDecorator {
	private Map<String,Object> defaultBinding;

	public BindingMapDecorator(){
	}
	
	public BindingMapDecorator(BindingDecorator chained){
		super(chained);
	}
	
	public BindingMapDecorator(Map<String,Object> defaultBinding){
		this.defaultBinding=defaultBinding;
	}
	
	public BindingMapDecorator(Map<String,Object> defaultBinding, BindingDecorator chained){
		super(chained);
		this.defaultBinding=defaultBinding;
	}
	
	@Override
	public void decorate(Map<String,Object> binding) {
		binding.putAll(defaultBinding);
	}

	public void setDefaultBinding(Map<String,Object> defaultBinding) {
		this.defaultBinding = defaultBinding;
	}

	public Map<String,Object> getDefaultBinding() {
		return defaultBinding;
	}

}

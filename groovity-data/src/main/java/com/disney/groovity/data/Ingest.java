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

import java.util.Map;

import com.disney.groovity.compile.SkipStatistics;

/**
 * API for traits and types that have special ingest logic, typically for transforming or post-processing data 
 * retrieved from a source into the native data model.  Traits and types can also declare multiple setters for 
 * a property to accomplish this, but Ingest allows this complexity to be hidden from users of the data model.  
 * 
 * @author Alex Vigdor
 */
public interface Ingest{
	@SkipStatistics
	public boolean ingest(String key, Object val);
	
	@SkipStatistics
	public default Ingest ingest(Map<String, Object> data) {
		data.forEach(this::ingest);
		return this;
	}
}

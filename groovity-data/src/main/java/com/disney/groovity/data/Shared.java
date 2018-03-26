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


/**
 * Marker for data objects that are shared, i.e. not cloned on retrieval from the factory.  This
 * can be used to improve performance for objects intended to be used in a read-only manner, while it makes data mutations 
 * dangerous in the sense that they are visible to all callers and subject to race conditions, and yet transient since they are not persisted.  
 * 
 * In other words, only use this API for data that will not be mutated by the application, or when the application will perform an explicit clone() 
 * before attempting mutations.  If the application retrieves a SharedModel and mutates without cloning, bad things can and will happen.
 * 
 * @author Alex Vigdor
 */
public interface Shared{

}

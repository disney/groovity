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
/**
 * Centralize some constants for Grooovity
 *
 * @author Alex Vigdor
 */
public interface GroovityConstants {
	//internal use
	public static final String GROOVITY_SOURCE_EXTENSION = ".grvt";
	public static final String INTERNAL_BINDING_PREFIX = "._";
	public static final String GROOVITY_CORE_TAG_PACKAGE = "com.disney.groovity.tags";
	public static final String GROOVITY_JAR_MANIFEST = "groovity/manifest";
	
	//common terms
	public static final String OUT = "out";
	public static final String BINDING = "binding";
	
	//special static fields
	public static final String CONF = "conf";
	public static final String ARGS = "args";
	public static final String IS_GROOVITY_LIBRARY = "isGroovyLibrary";
	public static final String INIT_DEPENDENCIES = "initDependencies";
	public static final String GROOVITY_SCRIPT_HELPER_FIELD = "___GroovityScriptHelper";
	
	//built-in special method names
	public static final String LOAD = "load";
	public static final String RUN = "run";
	public static final String STREAM = "stream";
	public static final String TAG = "tag";
	public static final String INIT = "init";
	public static final String START = "start";
	public static final String DESTROY = "destroy";
	
	//common tag attributes
	public static final String VAR = "var";
	public static final String VALUE = "value";
	public static final String TIMEOUT = "timeout";
	public static final String POOL = "pool";
	public static final String MESSAGE = "message";
	public static final String URL = "url";
	public static final String CLOSE = "close";
	public static final String ERROR = "error";
	
	//Documentation terms
	public static final String NAME = "name";
	public static final String INFO = "info";
	public static final String BODY = "body";
	public static final String EXTENDS = "extends";
	public static final String IMPLEMENTS = "implements";
	public static final String RETURNS = "returns";
	public static final String SAMPLE = "sample";
	public static final String CORE = "core";
	public static final String REQUIRED = "required";
	public static final String ATTRS = "attrs";
	public static final String PATH = "path";
	public static final String FUNCTIONS = "functions";
	public static final String NULLABLE = "nullable";
	public static final String TYPE = "type";
	public static final String CLASSES = "classes";
	public static final String PROPERTIES = "properties";
	public static final String MODIFIERS = "modifiers";
	public static final String PARAMETERS = "parameters";
	public static final String METHODS = "methods";
}

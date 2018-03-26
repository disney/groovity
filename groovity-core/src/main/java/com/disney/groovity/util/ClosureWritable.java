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
package com.disney.groovity.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.Callable;

import com.disney.groovity.GroovityConstants;

import groovy.lang.Closure;
import groovy.lang.Writable;
/**
 * A closure writable is used as the underlying implementation for streaming templates
 *
 * @author Alex Vigdor
 */
public class ClosureWritable implements Callable<Object>, Writable, GroovityConstants{
	@SuppressWarnings("rawtypes")
	private final Closure closure;
	
	public ClosureWritable(@SuppressWarnings("rawtypes")final Closure closure){
		this.closure=closure;
	}
	
	public Object call(){
		closure.call();
		return null;
	}

	@SuppressWarnings({"rawtypes","unchecked"})
	public Writer writeTo(Writer writer) throws IOException {
		Map variables = ScriptHelper.get(closure).getBinding().getVariables();
		Object oldOut = variables.get(OUT);
		if(oldOut==writer){
			closure.call();
		}
		else{
			variables.put(OUT,writer);
			try{
				closure.call();
			}
			finally{
				variables.put(OUT,oldOut);
			}
		}
		return writer;
	}
	
	public String toString(){
		StringWriter writer = new StringWriter();
		try {
			writeTo(writer);
		} catch (IOException e) {
			return super.toString()+": error "+e.getMessage();
		}
		return writer.toString();
	}

	@SuppressWarnings("rawtypes")
	public Closure getClosure(){
		return closure;
	}
}

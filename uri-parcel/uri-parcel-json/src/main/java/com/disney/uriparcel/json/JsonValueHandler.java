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
package com.disney.uriparcel.json;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import com.disney.uriparcel.ContentValueHandler;
import com.disney.uriparcel.value.AbstractValueHandler;
import com.google.gson.Gson;
/**
 * general-purpose JSON serialization/deserialization powered by Gson
 *
 * @author Alex Vigdor
 */
public class JsonValueHandler extends AbstractValueHandler implements ContentValueHandler {
	Gson gson = new Gson();

	public int getPriority() {
		//since this is a multi-type loader it is lower priority
		return 10;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Object doLoad(InputStream stream, String contentType, Class valueClass, Map config) throws Exception {
		Reader reader = new BufferedReader(new InputStreamReader(stream, getCharset(contentType)));
		return gson.fromJson(reader, valueClass);
	}

	@Override
	protected void doStore(OutputStream stream, String contentType, Object value, @SuppressWarnings("rawtypes") Map config) throws Exception {
		Writer writer = new BufferedWriter(new OutputStreamWriter(stream,getCharset(contentType)));
		gson.toJson(value, writer);
	}

	@Override
	public boolean isSupported(@SuppressWarnings("rawtypes") Class valueClass, String contentType) {
		if(contentType!=null){
			return contentType.contains("json");
		}
		return true;
	}

}

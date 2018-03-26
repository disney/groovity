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
package com.disney.uriparcel.value;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;

import com.disney.uriparcel.ContentStream;
import com.disney.uriparcel.ContentValueHandler;
/**
 * Value handler for storing and retrieving java Properties
 *
 * @author Alex Vigdor
 */
public class PropertiesValueHandler extends AbstractValueHandler implements ContentValueHandler {

	public int getPriority() {
		//since this deals with a specific class it is high priority
		return 100;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Object doLoad(InputStream stream, String contentType, Class valueClass, Map config) throws Exception {
		String charset = ContentStream.getCharset(contentType);
		if(charset==null){
			charset="UTF-8";
		}
		Properties props = new Properties();
		props.load(new BufferedReader(new InputStreamReader(stream,charset)));
		return props;
	}

	@Override
	protected void doStore(OutputStream stream, String contentType, Object value, @SuppressWarnings("rawtypes") Map config) throws Exception {
		Writer writer = new OutputStreamWriter(stream,getCharset(contentType));
		((Properties)value).store(writer, null);
		writer.flush();
	}

	@Override
	public boolean isSupported(@SuppressWarnings("rawtypes") Class valueClass, String contentType) {
		return Properties.class.equals(valueClass);
	}


}

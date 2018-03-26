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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.disney.uriparcel.ContentStream;
/**
 * Simple handler for converting between streams and strings; UTF-8 is the default charset for conversion, but you may specify
 * an alternative charset in the config map for loading, or in the contentType for storing.
 *
 * @author Alex Vigdor
 */
public class StringValueHandler extends AbstractValueHandler {

	public int getPriority() {
		return 102;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Object doLoad(InputStream stream, String contentType, Class valueClass, Map config) throws Exception {
		String charset = ContentStream.getCharset(contentType);
		if(charset==null){
			charset="UTF-8";
		}
		return loadString(stream, charset);
	}

	@Override
	protected void doStore(OutputStream stream, String contentType, Object value, @SuppressWarnings("rawtypes") Map config) throws Exception {
		stream.write(((String)value).getBytes(getCharset(contentType)));
	}

	@Override
	public boolean isSupported(@SuppressWarnings("rawtypes") Class valueClass, String contentType) {
		return String.class.equals(valueClass);
	}

}

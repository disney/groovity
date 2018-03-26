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

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.disney.uriparcel.ContentValueHandler;
/**
 * Abstract base class for value handlers
 *
 * @author Alex Vigdor
 */
public abstract class AbstractValueHandler implements ContentValueHandler {
	private static final Pattern charsetPattern = Pattern.compile("(?i)(?<=charset=)([^;,\\r\\n]+)");

	@Override
	public int compareTo(ContentValueHandler o) {
		return o.getPriority()-getPriority();
	}
	
	public static String getCharset(String contentType){
		if(contentType!=null){
			Matcher charMatcher = charsetPattern.matcher(contentType);
			if(charMatcher.find()){
				return charMatcher.group(1);
			}
		}
		return "UTF-8";
	}
	
	public static byte[] loadBytes(InputStream stream) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int c;
		while((c=stream.read(buf))!=-1){
			baos.write(buf, 0, c);
		}
		return baos.toByteArray();
	}
	
	public static String loadString(InputStream stream, String charset) throws IOException{
		Reader reader = new InputStreamReader(stream,charset);
		CharArrayWriter writer = new CharArrayWriter();
		char[] buf = new char[4096];
		int c;
		while((c=reader.read(buf)) !=-1){
			writer.write(buf,0,c);
		}
		return writer.toString();
	}

	@Override
	public <T> T load(InputStream stream, String contentType, Class<T> valueClass, Map<?,?> config) throws Exception {
		if(isSupported(valueClass, contentType)){
			@SuppressWarnings("unchecked")
			T loaded = (T) doLoad(stream,contentType,valueClass,config);
			return loaded;
		}
		return null;
	}

	protected abstract Object doLoad(InputStream stream, String contentType, Class<?> valueClass, Map<?,?> config) throws Exception;

	@Override
	public boolean store(OutputStream stream, String contentType, Object value, Map<?,?> config) throws Exception {
		if(isSupported(value.getClass(), contentType)){
			doStore(stream,contentType,value,config);
			return true;
		}
		return false;
	}

	protected abstract void doStore(OutputStream stream, String contentType, Object value, Map<?,?> config) throws Exception;

	@Override
	public abstract boolean isSupported(Class<?> valueClass, String contentType);
}

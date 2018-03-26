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
package com.disney.uriparcel.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import com.disney.uriparcel.ContentStream;
import com.disney.uriparcel.ContentStreamStore;
/**
 * URL Stream Store is a catchall that supports storage and retrieval of binary streams
 * using the java URLConnection API, which may in turn support a number of protocols.
 *
 * @author Alex Vigdor
 */
public class URLStreamStore extends AbstractStreamStore implements ContentStreamStore {

	public int getPriority() {
		//since this is a generic multi-protocol implementation it will take lower priority
		return 10;
	}

	@Override
	public boolean isSupported(ContentStream stream) {
		try {
			stream.getUri().toURL();
		} catch (MalformedURLException e) {
			return false;
		}
		return true;
	}

	@Override
	public InputStream getInputStream(ContentStream uriContent, Map<?,?> config) throws IOException {
		final URL url = uriContent.getUri().toURL();
		final URLConnection conn = url.openConnection();
		uriContent.setLastModified(conn.getLastModified());
		uriContent.setContentLength(conn.getContentLengthLong());
		uriContent.setContentType(conn.getContentType());
		uriContent.seteTag(conn.getHeaderField("ETag"));
		return conn.getInputStream();
	}

	@Override
	public OutputStream getOutputStream(ContentStream uriContent, Map<?,?> config) throws IOException {
		final URL url = uriContent.getUri().toURL();
		final URLConnection conn = url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", uriContent.getContentType());
		return conn.getOutputStream();
	}
}

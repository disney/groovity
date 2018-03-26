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
import java.util.Map;

import com.disney.uriparcel.ContentStream;
/**
 * Read-only Stream store for retrieving resources from the classpath
 *
 * @author Alex Vigdor
 */
public class ClasspathStreamStore extends AbstractStreamStore {

	@Override
	public boolean isSupported(ContentStream stream) {
		return "classpath".equalsIgnoreCase(stream.getUri().getScheme());
	}

	@Override
	public int getPriority() {
		return 95;
	}

	@Override
	public InputStream getInputStream(ContentStream uriContent, Map<?,?> config) throws IOException {
		String path = uriContent.getUri().getSchemeSpecificPart();
		return uriContent.getClass().getClassLoader().getResourceAsStream(path);
	}

	@Override
	public OutputStream getOutputStream(ContentStream uriContent, Map<?,?> config) throws IOException {
		throw new UnsupportedOperationException();
	}

}

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.disney.uriparcel.ContentStream;
import com.disney.uriparcel.ContentStreamStore;
/**
 * Stream store for reading and writing from java.io.File URIs
 *
 * @author Alex Vigdor
 */
public class FileStreamStore extends AbstractStreamStore implements ContentStreamStore {

	public int getPriority() {
		//since this deals with a specific protocol it is high priority
		return 100;
	}

	@Override
	public boolean isSupported(ContentStream stream) {
		return "file".equalsIgnoreCase(stream.getUri().getScheme());
	}

	@Override
	public OutputStream getOutputStream(final ContentStream uriContent, Map<?,?> config) throws FileNotFoundException {
		final File file = new File(uriContent.getUri());
		File pf = file.getParentFile();
		if(pf!=null){
			pf.mkdirs();
		}
		return new FileOutputStream(file){
			public void close() throws IOException{
				super.close();
				file.setLastModified(uriContent.getLastModified());
			}
		};
	}


	@Override
	public InputStream getInputStream(ContentStream uriContent, Map<?,?> config) throws IOException {
		final File file = new File(uriContent.getUri());
		uriContent.setContentLength(file.length());
		uriContent.setLastModified(file.lastModified());
		return new FileInputStream(file);
	}

	
}

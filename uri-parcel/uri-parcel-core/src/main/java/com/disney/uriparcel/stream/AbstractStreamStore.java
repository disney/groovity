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
import java.util.concurrent.Callable;

import com.disney.uriparcel.ContentStream;
import com.disney.uriparcel.ContentStreamStore;
/**
 * Abstract base class for stream stores 
 *
 * @author Alex Vigdor
 */
public abstract class AbstractStreamStore implements ContentStreamStore {

	public int compareTo(ContentStreamStore o) {
		return o.getPriority()-getPriority();
	}

	@Override
	public boolean load(final ContentStream content, final Map<?,?> config) throws IOException {
		if(isSupported(content)){
			content.setContent(new Callable<InputStream>() {
				public InputStream call() throws Exception {
					return getInputStream(content, config);
				}
			});
			return true;
		}
		return false;
	}

	@Override
	public boolean store(ContentStream content, Map<?,?> config) throws IOException {
		if(isSupported(content)){
			OutputStream os = getOutputStream(content, config);
			try{
				int c = 0;
				byte[] buf = new byte[4096];
				InputStream in = content.getContent().call();
				try{
					while((c=in.read(buf))!=-1){
						os.write(buf,0,c);
					}
				}
				finally{
					in.close();
				}
			}
			catch(IOException e){
				throw e;
			}
			catch(Exception e){
				throw new IOException(e);
			}
			finally{
				os.close();
			}
			return true;
		}
		return false;
	}

	public abstract boolean isSupported(ContentStream stream);
	public abstract int getPriority();
	public abstract InputStream getInputStream(ContentStream uriContent, Map<?,?> config) throws IOException;
	public abstract OutputStream getOutputStream(ContentStream uriContent, Map<?,?> config) throws IOException;
}

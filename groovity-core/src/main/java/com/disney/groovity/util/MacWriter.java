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
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import javax.crypto.Mac;
/**
 * MacWriter converts characters to bytes, updates the provided Mac with them, and discards them as they are written
 * 
 * @author Alex Vigdor
 *
 */
public class MacWriter extends Writer {
	final Mac mac;
	final CharsetEncoder encoder;
	final ByteBuffer buffer;
	boolean used = false;
	byte[] hash;

	public MacWriter(Mac mac, CharsetEncoder encoder ) {
		this.mac = mac;
		this.encoder = encoder;
		this.buffer = ByteBuffer.allocate(1024);
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		used = true;
		CharBuffer input = CharBuffer.wrap(cbuf, off, len);
		CoderResult result = null;
		while(result == null || result.isOverflow()) {
			result = encoder.encode(input, buffer, false);
			buffer.flip();
			mac.update(buffer);
			buffer.clear();
		}
	}

	@Override
	public void flush() throws IOException {
		
	}

	@Override
	public void close() throws IOException {
		encoder.encode(CharBuffer.allocate(0),buffer,true);
		hash = mac.doFinal();
	}
	
	public byte[] getHash() {
		return hash;
	}
	
	public boolean isUsed() {
		return used;
	}

}

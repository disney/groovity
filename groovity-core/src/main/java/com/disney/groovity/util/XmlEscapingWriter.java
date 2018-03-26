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
/**
 * Simple writer that performs XML escaping without buffering
 * 
 * @author Alex Vigdor
 *
 */
public class XmlEscapingWriter extends Writer{
	final private Writer out;

	public XmlEscapingWriter(final Writer out) {
		this.out=out;
	}

	@Override
	public void write(final char[] buffer, final int offset, final int length) throws IOException {
		int start = offset;
		int p = offset;
		final int bound = offset+length;
		int c;
		while(p<bound) {
			c = buffer[p];
			if(isSpecial(c)) {
				if(p>start) {
					out.write(buffer, start, p-start);
				}
				write(c);
				start = ++p;
			}
			else {
				p++;
			}
		}
		if(start<bound) {
			out.write(buffer, start, bound-start);
		}
	}
	
	private final static boolean isSpecial(int c) {
		switch(c) {
			case '&':
			case '<':
			case '>':
			case '"':
			case '\'':
				return true;
			default:
				return false;
		}
	}

	@Override
	public final void write(int c) throws IOException {
		switch(c) {
			case '&':
				out.write("&amp;");
				break;
			case '<':
				out.write("&lt;");
				break;
			case '>':
				out.write("&gt;");
				break;
			case '"':
				out.write("&quot;");
				break;
			case '\'':
				out.write("&apos;");
				break;
			default:
				out.write(c);
		}
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

}
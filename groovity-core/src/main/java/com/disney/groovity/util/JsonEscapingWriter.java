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
import java.util.Locale;
/**
 * Simple writer that performs JSON escaping without buffering
 * 
 * @author Alex Vigdor
 *
 */
public class JsonEscapingWriter extends Writer{
	final private Writer out;

	public JsonEscapingWriter(final Writer out) {
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
				escape(c);
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
		if(c<32 || c=='"' || c=='\\' || c=='/' || c=='\u2028' || c=='\u2029') {
			return true;
		}
		return false;
	}
	
	private final void escape(int c) throws IOException {
		switch(c) {
			case '\n':
				out.write("\\n");
				break;
			case '\r':
				out.write("\\r");
				break;
			case '\t':
				out.write("\\t");
				break;
			case '\f':
				out.write("\\f");
				break;
			case '\b':
				out.write("\\b");
				break;
			case '"':
				out.write("\\\"");
				break;
			case '\\':
				out.write("\\\\");
				break;
			case '/':
				out.write("\\/");
				break;
			default:
				unicode(c);
		}
	}

	@Override
	public final void write(int c) throws IOException {
		if(isSpecial(c)) {
			escape(c);
		}
		else {
			out.write(c);
		}
	}

	private final void unicode(int c) throws IOException{
		if (c > 0xfff) {
			out.write("\\u");
		} 
		else if (c > 0xff) {
			out.write("\\u0");
		} 
		else if (c > 0xf) {
			out.write("\\u00");
		} 
		else {
			out.write("\\u000");
		}
		out.write(hex(c));
	}

	private final String hex(int codepoint) {
		return Integer.toHexString(codepoint).toUpperCase(Locale.ENGLISH);
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
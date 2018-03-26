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
package com.disney.groovity.tags;

import groovy.lang.Closure;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.util.Whitespace;

/**
 * Collapses whitespace in the output of the body
 * <p>
 * minify( <ul>
 *	</ul>{
 *	<blockquote>// the content to minify</blockquote>
 * 	});
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;~ &lt;g:minify&gt;
 *		&lt;g:each in=&quot;${model}&quot;&gt;
 *			${it}
 *		&lt;/g:each&gt;
 *	&lt;/g:minify&gt; ~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */ 
@Tag(
		info = "Collapses whitespace in the output of the body",
		body = "the content to minify",
		sample="<~ <g:minify>\n" + 
				"\t<g:each in=\"${model}\">\n" + 
				"\t\t${it}\n" + 
				"\t</g:each>\n" + 
				"</g:minify> ~>"
 
	)
public class Minify implements Taggable {

	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) {
		final Object oldOut = get(body,OUT);
		if(oldOut==null){
			throw new IllegalArgumentException("Minify requires non-null 'out'");
		}
		WhitespaceTrimmingWriter writer = new WhitespaceTrimmingWriter((Writer) oldOut);
		bind(body,OUT, writer);
		try{
			body.call();
		}
		finally{
			try {
				writer.close();
			} catch (IOException e) {}
			bind(body,OUT, oldOut);
		}	
		return null;
	}
	
	final class WhitespaceTrimmingWriter extends Writer{
		private final static int bufSize = 4096;
		private final Writer out;
		private final char[] buffer = new char[bufSize];
		private boolean inWhitespace = false;
		private boolean hasNewline = false;
		private int pos = 0;

		protected WhitespaceTrimmingWriter(Writer out) {
			this.out=out;
		}
		
		private final void writeChar(final char c) throws IOException{
			buffer[pos++]=c;
			if(pos==bufSize){
				flush();
			}
		}
		
		public final void write(final int c) throws IOException{
			if(inWhitespace){
				if(!Whitespace.isWhitespace(c)){
					//end whitespace, decide how to fill
					if(hasNewline){
						writeChar('\n');
					}
					else{
						writeChar(' ');
					}
					inWhitespace=false;
					hasNewline=false;
					writeChar((char)c);
				}
				if(!hasNewline && (c ==10 || c==13)){
					hasNewline=true;
				}
			}
			else{
				if(Whitespace.isWhitespace(c)){
					inWhitespace=true;
					if(c ==10 || c==13){
						hasNewline=true;
					}
				}
				else{
					writeChar((char)c);
				}
			}
		}

		@Override
		public final void write(final char[] cbuf, final int off, final int len) throws IOException {
			final int end = off+len;
			for(int c=off; c< end; c++){
				write(cbuf[c]);
			}
		}
		
		public final void write(String str, int off, int len) throws IOException {
			final int end = off+len;
			for(int c=off; c< end; c++){
				write(str.charAt(c));
			}
		}
		
		public final Writer append(final CharSequence csq) throws IOException{
			final int end = csq.length();
			for(int c=0; c< end; c++){
				write(csq.charAt(c));
			}
			return this;
		}
		
		public final Writer append(final CharSequence csq, final int off, final int len) throws IOException{
			final int end = off+len;
			for(int c=off; c< end; c++){
				write(csq.charAt(c));
			}
			return this;
		}
		
		
		@Override
		public final void flush() throws IOException {
			if(pos>0){
				out.write(buffer,0,pos);
				pos=0;
			}
		}

		@Override
		public final void close() throws IOException {
			flush();
		}
		
	}

}

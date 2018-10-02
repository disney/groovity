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
package com.disney.groovity.servlet;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.impl.EnglishReasonPhraseCatalog;

import com.disney.groovity.servlet.error.GroovityError;

/**
 * Custom response wrapper that leverages GroovyErrorHandlers for error
 * handling, and can buffer a configurable amount of a response before
 * committing the response, useful for being able to still generate 500 errors
 * when late exceptions occur during response generation
 *
 * @author Alex Vigdor
 */
public class GroovityServletResponseWrapper extends HttpServletResponseWrapper {
	private final GroovityScriptView groovityScriptView;
	private final HttpServletRequest request;
	private final int buffer;
	private BufferOutputStream bufferOutputStream;
	private BufferWriter bufferWriter;

	public GroovityServletResponseWrapper(HttpServletRequest request, HttpServletResponse response,
			GroovityScriptView groovityScriptView) {
		super(response);
		this.groovityScriptView = groovityScriptView;
		this.request = request;
		this.buffer = groovityScriptView.bufferSize;
	}

	// override to use custom error pages if applicable
	public void sendError(int sc) throws IOException {
		sendError(sc, null);
	}

	public void sendError(int sc, String message) throws IOException {
		HttpServletResponse response = (HttpServletResponse) getResponse();
		boolean handled = false;
		GroovityError error = groovityScriptView.getGroovyError(request);
		if (message != null) {
			error.setMessage(message);
		}
		error.setStatus(sc);
		error.setReason(EnglishReasonPhraseCatalog.INSTANCE.getReason(sc, getLocale()));
		response.setStatus(sc);
		if (groovityScriptView.errorHandlers != null) {
			try {
				handled = groovityScriptView.errorHandlers.handleError(request, response, error);
				// System.out.println("Handled sendError: "+handled);
			} catch (ServletException e) {
				throw new IOException(e);
			}
		}
		if (!handled) {
			// System.out.println("Sending error as chain did not handle "+sc+"
			// : "+message);
			response.sendError(sc, message);
		}
	}

	public void commit() throws IOException {
		if (bufferOutputStream != null) {
			ResponseMeta rm = bufferOutputStream.getResponseMeta();
			if(rm!=null && !shouldContinue(rm)) {
				return;
			}
			bufferOutputStream.commit();
		} else if (bufferWriter != null) {
			ResponseMeta rm = bufferWriter.getResponseMeta(getCharacterEncoding());
			if(rm!=null && !shouldContinue(rm)) {
				return;
			}
			bufferWriter.commit();
		}
	}

	private boolean shouldContinue(ResponseMeta rm) {
		setContentLength(rm.length);
		if(!containsHeader("ETag")) {
			String etag =  "\""+DatatypeConverter.printBase64Binary(rm.hash)+"\"";
			String inm = request.getHeader("If-None-Match");
			if(inm!=null && inm.equals(etag)){
				setStatus(304);
				return false;
			}
			setHeader("Etag", etag);
		}
		return true;
	}

	public ServletOutputStream getOutputStream() throws IOException {
		if (buffer > 0) {
			if (bufferOutputStream == null) {
				if (bufferWriter != null) {
					throw new IllegalStateException(
							"Cannot get OutputStream on response after Writer has already been acquired");
				}
				bufferOutputStream = new BufferOutputStream(getResponse().getOutputStream(), buffer);
			}
			return bufferOutputStream;
		}
		return getResponse().getOutputStream();
	}

	public PrintWriter getWriter() throws IOException {
		if (buffer > 0) {
			if (bufferWriter == null) {
				if (bufferOutputStream != null) {
					throw new IllegalStateException(
							"Cannot get Writer on response after OutputStream has already been acquired");
				}
				bufferWriter = new BufferWriter(getResponse().getWriter(), buffer);
			}
			return bufferWriter;
		}
		return getResponse().getWriter();
	}

	private static class BufferOutputStream extends ServletOutputStream {
		private final int buffer;
		private final ServletOutputStream rawStream;
		private BytesWriter baos;
		private boolean flushed = false;

		private BufferOutputStream(ServletOutputStream rawStream, int buffer) {
			this.rawStream = rawStream;
			this.baos = new BytesWriter();
			this.buffer = buffer;
		}

		@Override
		public boolean isReady() {
			return rawStream.isReady();
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			rawStream.setWriteListener(writeListener);
		}

		@Override
		public void write(int b) throws IOException {
			if (!flushed) {
				if (baos.size() + 1 > buffer) {
					commit();
				} else {
					baos.write(b);
					return;
				}
			}
			rawStream.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (!flushed) {
				if (baos.size() + len > buffer) {
					commit();
				} else {
					baos.write(b, off, len);
					return;
				}
			}
			rawStream.write(b, off, len);
		}

		@Override
		public void flush() throws IOException {
			if (flushed) {
				rawStream.flush();
			}
		}

		private ResponseMeta getResponseMeta() {
			if (baos != null) {
				return baos.process();
			}
			return null;
		}

		protected void commit() throws IOException {
			if (!flushed) {
				baos.writeTo(rawStream);
				flushed = true;
				baos = null;
			}
		}

		@Override
		public void close() throws IOException {
			rawStream.close();
		}

	}

	private static class BufferWriter extends PrintWriter {
		private static final char[] LINE_SEPARATOR = System.lineSeparator().toCharArray();
		final PrintWriter rawWriter;
		final int buffer;
		CharsWriter caw;
		private boolean flushed = false;

		public BufferWriter(PrintWriter out, int buffer) {
			super(out);
			rawWriter = out;
			caw = new CharsWriter();
			this.buffer = buffer;
		}

		@Override
		public void write(int b) {
			if (!flushed) {
				if (caw.size() + 1 > buffer) {
					commit();
				} else {
					caw.write(b);
					return;
				}
			}
			rawWriter.write(b);
		}

		@Override
		public void write(char[] cbuf, int off, int len) {
			if (!flushed) {
				if (caw.size() + len > buffer) {
					commit();
				} else {
					caw.write(cbuf, off, len);
					return;
				}
			}
			rawWriter.write(cbuf, off, len);
		}

		@Override
		public void write(char buf[]) {
			write(buf, 0, buf.length);
		}

		@Override
		public void write(String s) {
			write(s, 0, s.length());
		}

		@Override
		public void write(String s, int off, int len) {
			if (flushed) {
				rawWriter.write(s, off, len);
			} else {
				write(s.toCharArray(), off, len);
			}
		}

		@Override
		public void flush() {
			if (flushed) {
				rawWriter.flush();
			}
		}

		protected ResponseMeta getResponseMeta(String encoding) {
			if (caw != null) {
				return caw.process(encoding);
			}
			return null;
		}

		protected void commit() {
			if (!flushed) {
				try {
					caw.writeTo(rawWriter);
				} catch (IOException e) {
					setError();
				}
				flushed = true;
				caw = null;
			}
		}

		@Override
		public void close() {
			rawWriter.close();
		}

		@Override
		public void print(boolean b) {
			if (b) {
				write("true");
			} else {
				write("false");
			}
		}

		@Override
		public void print(char c) {
			write(c);
		}

		@Override
		public void print(int i) {
			write(String.valueOf(i));
		}

		@Override
		public void print(long l) {
			write(String.valueOf(l));
		}

		@Override
		public void print(float f) {
			write(String.valueOf(f));
		}

		@Override
		public void print(double d) {
			write(String.valueOf(d));
		}

		@Override
		public void print(char s[]) {
			write(s);
		}

		@Override
		public void print(String s) {
			if (s == null) {
				s = "null";
			}
			write(s);
		}

		@Override
		public void print(Object obj) {
			write(String.valueOf(obj));
		}

		@Override
		public void println() {
			write(LINE_SEPARATOR);
		}

		@Override
		public void println(boolean b) {
			print(b);
			println();
		}

		@Override
		public void println(char c) {
			print(c);
			println();
		}

		@Override
		public void println(int i) {
			print(i);
			println();
		}

		@Override
		public void println(long l) {
			print(l);
			println();
		}

		@Override
		public void println(float f) {
			print(f);
			println();
		}

		@Override
		public void println(double d) {
			print(d);
			println();
		}

		@Override
		public void println(char c[]) {
			print(c);
			println();
		}

		@Override
		public void println(String s) {
			print(s);
			println();
		}

		@Override
		public void println(Object o) {
			print(o);
			println();
		}

	}
	
	private static class CharsWriter extends CharArrayWriter{

		public ResponseMeta process(String encoding){
			CharsetEncoder encoder = Charset.forName(encoding).newEncoder();
			MessageDigest md;
			try {
				md = MessageDigest.getInstance("md5");
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
			int length = 0;
			ByteBuffer out = ByteBuffer.allocate(8192);
			CharBuffer cb = CharBuffer.wrap(this.buf, 0, this.count);
			CoderResult cr = null;
			while(cr==null || cr.isOverflow()) {
				cr = encoder.encode(cb, out, false);
				length+= out.position();
				out.flip();
				md.update(out);
				out.clear();
			}
			encoder.encode(CharBuffer.allocate(0),out,true);
			ResponseMeta rm = new ResponseMeta();
			rm.hash = md.digest();
			rm.length = length;
			return rm;
		}
	}

	private static class BytesWriter extends ByteArrayOutputStream{
		public ResponseMeta process() {
			MessageDigest md;
			try {
				md = MessageDigest.getInstance("md5");
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
			md.update(buf,0,count);
			ResponseMeta rm = new ResponseMeta();
			rm.length = this.count;
			rm.hash = md.digest();
			return rm;
		}
	}

	private static class ResponseMeta{
		int length;
		byte[] hash;
	}
}

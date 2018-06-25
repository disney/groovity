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
package com.disney.groovity.model;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.Future;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

import com.disney.groovity.util.JsonEscapingWriter;

import groovy.lang.Closure;
import groovy.lang.Writable;

/**
 * A ModelVisitor that produces a serialized JSON representation of a Model
 * 
 * @author Alex Vigdor
 *
 */
public class ModelJsonWriter extends ModelWalker{
	static TransformerFactory transformerFactory = TransformerFactory.newInstance();
	final protected Writer writer;
	final protected Writer escape;
	int indent = 0;
	final String indentChars;
	private Transformer transformer = null;
	
	public ModelJsonWriter(Writer writer) {
		this(writer,null);
	}
	
	public ModelJsonWriter(Writer out, String indentChars) {
		this.writer = out;
		this.escape = new JsonEscapingWriter(out);
		this.indentChars = indentChars;
		if(indentChars==null || indentChars.length()==0) {
			this.indent = -1;
		}
	}

	public void visitNull() throws Exception {
		writer.write("null");
	}
	
	protected void writeIndent() throws IOException {
		if(indent>0) {
			for(int i=0;i<indent;i++) {
				writer.write(indentChars);
			}
		}
	}
	
	protected void delimit() throws IOException {
		if(!positions.isEmpty() &&  positions.peek().get() >0) {
			if(indent>=0) {
				writer.write(",\n");
			}
			else {
				writer.write(",");
			}
		}
		if(indent>0) {
			writeIndent();
		}
	}
	
	public void visitObject(Object o) throws Exception {
		if(o == null) {
			writer.write("null");
		}
		else if(o instanceof CharSequence) {
			writer.write('"');
			escape.append((CharSequence) o);
			writer.write('"');
		}
		else if(o instanceof Number) {
			if(o instanceof Double) {
				Double d = (Double) o;
				if(d.isInfinite()||d.isNaN()) {
					writer.write("null");
					return;
				}
			}
			if(o instanceof Float) {
				Float f = (Float) o;
				if(f.isInfinite()||f.isNaN()) {
					writer.write("null");
					return;
				}
			}
			writer.write(o.toString());
		}
		else if(o instanceof Boolean) {
			writer.write(o.toString());
		}
		else if(o instanceof Date) {
			writer.write(String.valueOf(((Date)o).getTime()));
		}
		else if(o instanceof Writable  && ! (o instanceof Model)) {
			writer.write('"');
			((Writable)o).writeTo(escape);
			writer.write('"');
		}
		else if(o instanceof Class) {
			writer.write('"');
			escape.write(((Class<?>)o).getName());
			writer.write('"');
		}
		else if(o instanceof URI || o instanceof URL || o instanceof Throwable || o.getClass().isEnum()) {
			writer.write('"');
			escape.write(o.toString());
			writer.write('"');
		}
		else if(o instanceof File) {
			writer.write('"');
			escape.write(((File)o).getName());
			writer.write('"');
		}
		else if(o instanceof byte[]) {
			writer.write('"');
			escape.write(DatatypeConverter.printBase64Binary((byte[]) o));
			writer.write('"');
		}
		else if(o instanceof Node) {
			writer.write('"');
			if(transformer==null) {
				transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			}
			transformer.transform(new DOMSource((Node)o), new StreamResult(escape));
			writer.write('"');
		}
		else if(o instanceof JAXBElement) {
			visitObject(((JAXBElement)o).getValue());
		}
		else if(o instanceof Closure) {
			@SuppressWarnings("rawtypes")
			Closure c = (Closure) o;
			visitObject(c.call());
		}
		else if(o instanceof Future) {
			@SuppressWarnings("rawtypes")
			Future f = (Future) o;
			visitObject(f.get());
		}
		else {
			writer.write("{");
			if(indent>=0) {
				writer.write("\n");
				indent++;
			}
			super.visitObject(o);
			if(indent>=0) {
				indent--;
				writer.write("\n");
			}
			writeIndent();
			writer.write("}");
		}
	}
	
	public void visitList(@SuppressWarnings("rawtypes") Iterable o) throws Exception {
		writer.write("[");
		if(indent>=0) {
			writer.write("\n");
			indent++;
		}
		super.visitList(o);
		if(indent>=0) {
			indent--;
			writer.write("\n");
		}
		writeIndent();
		writer.write("]");
	}
	
	public void visit(Object o) throws Exception {
		super.visit(o);
	}
	
	public void visitObjectField(String name, Object value) throws Exception{
		delimit();
		writer.write('"');
		escape.write(name);
		if(indent>=0) {
			writer.write("\" : ");
		}
		else {
			writer.write("\":");
		}
		super.visitObjectField(name, value);
	}
	
	public void visitListMember(Object value) throws Exception{
		delimit();
		super.visitListMember(value);
	}
	
	public static String toJsonString(Object o, String indent) throws Exception{
		CharArrayWriter caw = new CharArrayWriter();
		new ModelJsonWriter(caw, indent).visit(o);
		return caw.toString();
	}
	
	public void escape(CharSequence seq) throws IOException {
		escape.append(seq);
	}
	
	public void print(CharSequence seq) throws IOException {
		writer.append(seq);
	}
}

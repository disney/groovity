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

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Utility for parsing XML sources in to Map/List form; this works well for
 * general purpose data access, but for more complete XML document and
 * round-trip support use JAXB instead
 * 
 * @author Alex Vigdor
 *
 */
public class XmlParser {
	private static final Queue<XMLReader> xmlReaderCache = new ArrayBlockingQueue<>(
			Runtime.getRuntime().availableProcessors() * 4);

	public static XMLReader borrowXMLReader() throws ParserConfigurationException, SAXException {
		XMLReader reader = xmlReaderCache.poll();
		if (reader == null) {
			reader = XMLReaderFactory.createXMLReader();
		}
		return reader;
	}

	public static void returnXMLReader(XMLReader reader) {
		xmlReaderCache.offer(reader);
	}

	@SuppressWarnings("rawtypes")
	public static Map parseXML(Reader reader) throws IOException, ParserConfigurationException, SAXException {
		ArrayDeque<Map> parseStack = new ArrayDeque<>();
		XMLReader xreader = borrowXMLReader();

		try {
			xreader.setContentHandler(new DefaultHandler2() {
				CharArrayWriter writer;

				@SuppressWarnings("unchecked")
				@Override
				public void startElement(String uri, String localName, String qName, Attributes atts)
						throws SAXException {
					textBoundary();
					Map elem = new LinkedHashMap();
					if (atts.getLength() > 0) {
						for (int i = 0; i < atts.getLength(); i++) {
							String name = atts.getLocalName(i);
							if (!name.equals("")) {
								String av = atts.getValue(i);
								elem.put(name, av);
							}
						}
					}
					parseStack.push(elem);
				}

				@Override
				public void endElement(String uri, String localName, String qqName) throws SAXException {
					textBoundary();
					if (parseStack.size() > 1) {
						Map last = parseStack.pop();
						Object toAdd = last;
						if (last.size() == 0) {
							toAdd = null;
						} else if (last.size() == 1 && last.keySet().iterator().next().equals("text")) {
							toAdd = last.get("text");
						}
						add(localName, toAdd);
					}
				}

				@SuppressWarnings("unchecked")
				private void add(String name, Object toAdd) {
					Map parent = parseStack.peek();
					Object val = parent.get(name);
					if (val instanceof List) {
						((List) val).add(toAdd);
					} else if (val != null) {
						parent.put(name, new ArrayList(Arrays.asList(val, toAdd)));
					} else {
						parent.put(name, toAdd);
					}
				}

				private void textBoundary() {
					String text = "";
					if (writer != null) {
						text = writer.toString().trim();
						writer.reset();
					}
					if (text.length() > 0) {
						add("text", text);
					}
				}

				@Override
				public void characters(char[] ch, int start, int length) throws SAXException {
					if (writer == null) {
						writer = new CharArrayWriter();
					}
					writer.write(ch, start, length);
				}
			});
			xreader.parse(new InputSource(reader));
		} finally {
			returnXMLReader(xreader);
			reader.close();
		}
		return parseStack.pop();
	}
}

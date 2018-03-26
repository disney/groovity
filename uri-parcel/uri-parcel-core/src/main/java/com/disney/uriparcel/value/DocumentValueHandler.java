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
package com.disney.uriparcel.value;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

/**
 * Value handler for reading and writing XML DOM documents
 *
 * @author Alex Vigdor
 */
public class DocumentValueHandler extends AbstractValueHandler {
	TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	public int getPriority() {
		return 98;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Object doLoad(InputStream stream, String contentType, Class valueClass, Map config) throws Exception {
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(stream);
	}

	@Override
	protected void doStore(OutputStream stream, String contentType, Object value, @SuppressWarnings("rawtypes") Map config) throws Exception {
		Transformer t = TRANSFORMER_FACTORY.newTransformer();
		StreamResult result = new StreamResult(new OutputStreamWriter(stream,getCharset(contentType)));
		DOMSource source = new DOMSource((Document)value);
		t.transform(source,result);
	}

	@Override
	public boolean isSupported(@SuppressWarnings("rawtypes") Class valueClass, String contentType) {
		if(Document.class.equals(valueClass)){
			if(contentType!=null){
				if(!contentType.contains("xml")){
					return false;
				}
			}
			return true;
		}
		return false;
	}

}

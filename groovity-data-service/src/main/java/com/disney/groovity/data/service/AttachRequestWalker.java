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
package com.disney.groovity.data.service;

import java.io.File;
import java.io.IOException;


import com.disney.groovity.data.Attachment;
import com.disney.groovity.model.Model;
import com.disney.groovity.model.ModelWalker;
/**
 * Upload a raw PUT to update the payload of an attachment by name (attachment must already be present and named in the model)
 * 
 * @author Alex Vigdor
 *
 */
public class AttachRequestWalker extends ModelWalker{
	private boolean handled = false;
	private String attName;
	File file;
	String contentType;
	
	public AttachRequestWalker(String attName, String contentType, File file) throws IOException {
		//buffer request inputstream for replay
		this.attName = attName;
		this.contentType = contentType;
		this.file = file;
	}
	@Override
	public void visitObjectField(String name, Object value) throws Exception {
		if(!handled) {
			if(value instanceof Attachment) {
				Attachment a = (Attachment)value;
				if(a.getName().equals(attName)) {
					handled = true;
					//replace value with attachment based on incoming request, BUT copy over metadata
					Attachment.File na = new Attachment.File();
					na.copyMeta(a);
					na.setFile(file);
					if(contentType!=null) {
						na.setContentType(contentType);
					}
					na.setMd5(null);
					Model.put(objectStack.peek(), name, na);
				}
			}
			else {
				super.visitObjectField(name, value);
			}
		}
	}
	
	public boolean isHandled() {
		return handled;
	}
	
}

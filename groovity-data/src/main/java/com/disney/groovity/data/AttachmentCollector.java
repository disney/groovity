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
package com.disney.groovity.data;

import java.util.HashMap;
import java.util.Map;

import com.disney.groovity.model.ModelCollector;
/**
 * Capture attachments while devolving a model
 *
 * @author Alex Vigdor
 */
public class AttachmentCollector extends ModelCollector {
	Map<String, Attachment> attachments = new HashMap<>();
	
	public AttachmentCollector() {
		super();
	}
	
	public void visitObject(Object o) throws Exception{
		if(o instanceof Attachment) {
			Attachment a = (Attachment)o;
			attachments.put(a.getName(),a);
			if(a.getMd5()==null) {
				a.calculateMd5();
			}
			o = ((Attachment)o).describe();
		}
		super.visitObject(o);
	}
	public Map<String, Attachment> getAttachments(){
		return attachments;
	}
}

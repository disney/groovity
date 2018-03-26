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

import java.util.List;
import java.util.Map;

import com.disney.groovity.model.ModelCollector;

/**
 * Used to re-inline attachments to a loosely structured model
 *
 * @author Alex Vigdor
 */
public class Attacher extends ModelCollector {
	private List<Attachment> attachments;
	
	public Attacher(){
		
	}
	public Attacher(List<Attachment> attachments){
		this.attachments=attachments;
	}
	
	@SuppressWarnings("unchecked")
	public void visitObject(Object o) throws Exception{
		if(o instanceof Map && attachments!=null && !attachments.isEmpty()){
			Map<String,Object> map = ((Map<String,Object>)o);
			if(map.containsKey("name") && map.containsKey("contentType") && map.containsKey("length")) {
				String name = String.valueOf(map.get("name"));
				for(Attachment a: attachments){
					if(name.equals(a.getName())){
						a = (Attachment) a.copy();
						a.putAll(map);
						collected.push(a);
						return;
					}
				}
			}
		}
		super.visitObject(o);
	}
	
	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}
	
}

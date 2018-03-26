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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import com.disney.groovity.model.Model;
import com.disney.groovity.model.ModelConsumer;
import com.disney.groovity.model.ModelWalker;

/**
 * Attachment is a data model used to allow the storage of binary assets within the general data framework.  Data objects may declare fields
 * whose value is a single or array of attachments; attachments are meant to cross the mapping boundary between store and type so the binary data can travel; therefore
 * it should not be converted to a map during devolution like a normal DataModel.
 * 
 * @author Alex Vigdor
 *
 */
public abstract class Attachment implements Model {
	private String name;
	private String contentType;
	private Long length;
	private Date modified;
	private Map<String,Object> attributes;

	public Attachment() {}
	
	public Attachment(Attachment copyFrom) {
		this.name = copyFrom.getName();
		this.contentType = copyFrom.getContentType();
		this.length = copyFrom.getLength();
		this.modified = copyFrom.modified;
		if(copyFrom.attributes!=null) {
			this.attributes = new LinkedHashMap<>(copyFrom.attributes);
		}
	}
	
	@Override
	public void each(ModelConsumer c) {
		Model.each(this, c);
		if(attributes!=null) {
			for(Entry<String, Object> entry: attributes.entrySet()) {
				c.call(entry.getKey(), entry.getValue());
			}
		}
	}

	@Override
	public boolean put(String name, Object value) {
		if(!Model.put(this, name, value)) {
			if(attributes == null) {
				attributes = new LinkedHashMap<>();
			}
			attributes.put(name, value);
		}
		return true;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Long getLength() {
		return length;
	}

	public void setLength(Long length) {
		this.length = length;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

	public Object get(String attribute) {
		if(attributes!=null) {
			return attributes.get(attribute);
		}
		return null;
	}
	
	public String toString() {
		try {
			return toJsonString();
		} catch (Exception e) {
			return super.toString();
		}
	}

	public abstract InputStream getInputStream() throws Exception;
	
	public static Attachment find(Object model, final String name) throws Exception {
		AtomicReference<Attachment> a = new AtomicReference<>();
		new ModelWalker() {
			public void visitObject(Object o) throws Exception{
				if(o instanceof Attachment) {
					Attachment c = (Attachment) o;
					if(c.getName().equals(name)) {
						a.set(c);
					}
				}
				else {
					super.visitObject(o);
				}
			}
		}.visit(model);
		return a.get();
	}
	
	public static List<Attachment> findAll(Object model) throws Exception {
		ArrayList<Attachment> a = new ArrayList<>();
		new ModelWalker() {
			public void visitObject(Object o) throws Exception{
				if(o instanceof Attachment) {
					Attachment c = (Attachment) o;
					a.add(c);
				}
				else {
					super.visitObject(o);
				}
			}
		}.visit(model);
		return a;
	}

	public static class Empty extends Attachment{
		
		public Empty(){
			setLength(0l);
		}

		@Override
		public InputStream getInputStream() throws Exception {
			return new ByteArrayInputStream(new byte[0]);
		}
		
	}

	public static class File extends Attachment{
		private java.io.File file;

		public File() {}
		public File(java.io.File file) {
			setFile(file);
		}
		
		@Override
		public InputStream getInputStream() throws IOException{
			if(file==null) {
				throw new FileNotFoundException(getName());
			}
			return new FileInputStream(file);
		}

		public java.io.File getFile() {
			return file;
		}

		public void setFile(java.io.File file) {
			this.file = file;
			if(file!=null){
				setLength(file.length());
				setName(file.getName());
				setModified(new Date(file.lastModified()));
			}
		}
	}
	
	public static class Bytes extends Attachment{
		private byte[] bytes;
		
		public Bytes() {}
		public Bytes(byte[] bytes) { 
			setBytes(bytes);
		}
		public Bytes(Attachment copyFrom) throws Exception {
			super(copyFrom);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if(copyFrom.length>0) {
				InputStream in = copyFrom.getInputStream();
				try {
					byte[] buf = new byte[8192];
					int c;
					while((c=in.read(buf))!=-1) {
						baos.write(buf,0,c);
					}
				}
				finally {
					in.close();
				}
			}
			setBytes(baos.toByteArray());
		}
		
		public byte[] getBytes() {
			return bytes;
		}

		public void setBytes(byte[] bytes) {
			this.bytes = bytes;
			if(bytes!=null) {
				setLength(Long.valueOf(bytes.length));
			}
		}

		@Override
		public InputStream getInputStream() throws IOException {
			if(bytes==null) {
				throw new FileNotFoundException(getName());
			}
			return new ByteArrayInputStream(bytes);
		}
	}
	
	public static class Callable extends Attachment{
		private java.util.concurrent.Callable<InputStream> callable;
		
		public Callable() {}
		public Callable(java.util.concurrent.Callable<InputStream> callable) {
			this.callable = callable;
		}

		@Override
		public InputStream getInputStream() throws Exception {
			if(callable==null) {
				throw new FileNotFoundException(getName());
			}
			return callable.call();
		}

		public java.util.concurrent.Callable<InputStream> getCallable() {
			return callable;
		}

		public void setCallable(java.util.concurrent.Callable<InputStream> callable) {
			this.callable = callable;
		}
	}

}

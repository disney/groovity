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
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import javax.activation.DataSource;
import javax.xml.bind.DatatypeConverter;

import com.disney.groovity.model.Model;
import com.disney.groovity.model.ModelFilter;
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
public class Attachment implements Model, DataSource {
	public static final ModelFilter DESCRIBE = ModelFilter.transform(Attachment.class, a->{ return a.describe(); });
	protected String name;
	protected String contentType;
	protected Long length;
	protected Date modified;
	protected String md5;
	protected Map<String,Object> attributes;

	public Attachment() {}
	
	public Attachment(Attachment copyFrom) {
		this.name = copyFrom.getName();
		this.contentType = copyFrom.getContentType();
		this.length = copyFrom.getLength();
		this.modified = copyFrom.modified;
		this.md5 = copyFrom.md5;
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

	public void set(String name, Object value) {
		if(attributes == null) {
			attributes = new LinkedHashMap<>();
		}
		attributes.put(name, value);
	}

	public Map<String,Object> describe() {
		Map<String,Object> map = new LinkedHashMap<>();
		map.put("name", getName());
		map.put("contentType", getContentType());
		map.put("length", getLength());
		map.put("md5", getMd5());
		map.put("modified", getModified());
		if(attributes!=null) {
			map.putAll(attributes);
		}
		return map;
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

	public String getMd5() {
		return md5;
	}

	public void setMd5(String md5) {
		this.md5 = md5;
	}

	public boolean calculateMd5() throws Exception {
		MessageDigest digest = MessageDigest.getInstance("MD5");
		try(InputStream in = getInputStream()){
			if(in==null) {
				return false;
			}
			byte[] buf = new byte[8192];
			int c = 0;
			while((c=in.read(buf))!=-1) {
				digest.update(buf, 0, c);
			}
		}
		byte[] d = digest.digest();
		String m = DatatypeConverter.printBase64Binary(d);
		if(md5==null || !md5.equals(m)) {
			setMd5(m);
			modified = new Date();
			return true;
		}
		return false;
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

	public boolean hasContent() {
		return !(getClass().equals(Attachment.class));
	}

	public InputStream getInputStream() throws IOException{
		return null;
	}
	
	@Override
	public OutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException();
	}

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
		public InputStream getInputStream() throws IOException {
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
		}

		public Date getModified() {
			if(modified==null && file!=null) {
				modified = new Date(file.lastModified());
			}
			return modified;
		}

		public Long getLength() {
			if(length==null && file!=null) {
				length = file.length();
			}
			return length;
		}

		public String getName() {
			if(name==null && file!=null) {
				name = file.getName();
			}
			return name;
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
			if(copyFrom.length!=null && copyFrom.length>0) {
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
		public InputStream getInputStream() throws IOException {
			if(callable==null) {
				throw new FileNotFoundException(getName());
			}
			try {
				return callable.call();
			}
			catch(IOException e) {
				throw e;
			}
			catch(RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new IOException(e);
			}
		}

		public java.util.concurrent.Callable<InputStream> getCallable() {
			return callable;
		}

		public void setCallable(java.util.concurrent.Callable<InputStream> callable) {
			this.callable = callable;
		}
	}

}

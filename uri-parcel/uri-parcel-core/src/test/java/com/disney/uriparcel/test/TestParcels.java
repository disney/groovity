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
package com.disney.uriparcel.test;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.net.URI;
import java.security.Key;
import java.security.KeyStore;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.crypto.spec.SecretKeySpec;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.disney.uriparcel.MemoryContext;
import com.disney.uriparcel.MemoryPayload;
import com.disney.uriparcel.URIParcel;
import com.disney.uriparcel.value.KeyStoreValueHandler;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
/**
 * Unit tests for URIParcel
 *
 * @author Alex Vigdor
 */
public class TestParcels {
	@Rule
	public WireMockRule wireMockRule = new WireMockRule(28187); // No-args constructor defaults to port 8080

	@Test
	public void testPropsFileParcel() throws Exception{
		File myMapFile = File.createTempFile("testMapParcel", ".json");
		try{
			FileWriter fw = new FileWriter(myMapFile);
			try{
				fw.write("a=1\nb=foo\ncom.abc.def=xyz mno");
			}
			finally{
				fw.close();
			}
			URI myUri = myMapFile.toURI();
			URIParcel<Properties> myParcel = new URIParcel<Properties>(Properties.class, myUri);
			Properties result = myParcel.call();
			//System.out.println("Got "+result);
			Properties compareTo = new Properties();
			compareTo.put("a", "1");
			compareTo.put("b","foo");
			compareTo.put("com.abc.def","xyz mno");
			Assert.assertEquals(compareTo, result);
		}
		finally{
			myMapFile.delete();
		}
		
	}
	
	@Test
	public void testXml() throws Exception{
		String doc="<a><b>123</b><c>456</c></a>";
		String lmod = wiremock.org.apache.http.client.utils.DateUtils.formatDate(new Date());
		
		stubFor(get(urlEqualTo("/sample.xml"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Last-Modified", lmod)
						.withBody(doc)));
		URIParcel<Document> myDoc = new URIParcel<Document>(Document.class, new URI("http://localhost:28187/sample.xml"));
		Element rootElem = myDoc.call().getDocumentElement();
		Assert.assertEquals(2, rootElem.getChildNodes().getLength());
		Assert.assertEquals("123",rootElem.getChildNodes().item(0).getTextContent());
		Assert.assertEquals("456",rootElem.getChildNodes().item(1).getTextContent());
	}
	
	@Test
	public void testKeystore() throws Exception{
		File myMapFile = File.createTempFile("testMapParcel", ".json");
		try{
			URI myUri = myMapFile.toURI();
			String password = "myKeystorePassword";
			Map<String,Object> config = new HashMap<>();
			config.put(KeyStoreValueHandler.KEYSTORE_TYPE, "JCEKS");
			config.put(KeyStoreValueHandler.KEYSTORE_PASSWORD,password);
			SecretKeySpec secretKey = new SecretKeySpec("nobodyKnows".getBytes(), "DES");
			KeyStore orig = KeyStore.getInstance("JCEKS");
			orig.load(null);
			orig.setKeyEntry("foobar", secretKey, password.toCharArray(), null);
			URIParcel<KeyStore> putstore = new URIParcel<KeyStore>(KeyStore.class, myUri,config);
			putstore.put(orig);
			
			RuntimeException e=null;
			try{
				URIParcel<KeyStore> myParcel = new URIParcel<KeyStore>(KeyStore.class, myUri);
				myParcel.call();
			}
			catch(RuntimeException r){
				e=r;
			}
			Assert.assertNotNull("Expected error opening keystore with no config",e);
			
			URIParcel<KeyStore> myParcel = new URIParcel<KeyStore>(KeyStore.class, myUri,config);
			KeyStore result = myParcel.call();
			Key rkey = result.getKey("foobar", password.toCharArray());
			Assert.assertEquals(secretKey, rkey);
		}
		finally{
			myMapFile.delete();
		}
	}
	
	@Test
	public void testMem() throws Exception{
		String password = "sled";
		SecretKeySpec secretKey = new SecretKeySpec("rosebud".getBytes(), "DES");
		KeyStore orig = KeyStore.getInstance("JCEKS");
		orig.load(null);
		orig.setKeyEntry("kane", secretKey, password.toCharArray(), null);
		//MemoryPayload payload = MemoryPayload.serialize(orig,password.toCharArray());
		
		//MemoryContext.put(keystoreURI, payload);
		
		Map<String,Object> config = new HashMap<>();
		config.put(KeyStoreValueHandler.KEYSTORE_TYPE, "JCEKS");
		config.put(KeyStoreValueHandler.KEYSTORE_PASSWORD,password);
		URI keystoreURI = new URI("mem:/user/citizen.jceks");
		URIParcel<KeyStore> myParcel = new URIParcel<KeyStore>(KeyStore.class, keystoreURI, config);
		myParcel.put(orig);
		//now dereference
		myParcel = new URIParcel<KeyStore>(KeyStore.class, keystoreURI,config);
		KeyStore result = myParcel.call();
		Key rkey = result.getKey("kane", password.toCharArray());
		Assert.assertEquals(secretKey, rkey);
	}
	
	@Test
	public void testString() throws Exception{
		String val = "TEN FOUR OK \u201c";
		URI uri = new URI("mem:stringTest");
		MemoryContext.put(uri, new MemoryPayload(val.getBytes("UTF-8"), "text/plain;charset=UTF-8"));
		URIParcel<String> parcel = new URIParcel<String>(String.class,uri);
		String rval = parcel.call();
		Assert.assertFalse(rval==val);
		Assert.assertEquals(rval,val);
	}

	@Test
	public void testBytes() throws Exception{
		byte[] val = "kirksulu".getBytes();
		URI uri = new URI("mem:bytesTest");
		MemoryContext.put(uri, new MemoryPayload(val, "application/octet-stream"));
		URIParcel<byte[]> parcel = new URIParcel<byte[]>(byte[].class,uri);
		byte[] rval = parcel.call();
		Assert.assertFalse(rval==val);
		Assert.assertArrayEquals(rval,val);
	}
	
	@Test
	public void testMemObject() throws Exception{
		SomeThing originalThing = new SomeThing();
		originalThing.setWhat("sample");
		originalThing.setWhen(new Date());
		originalThing.setWhere("here");
		originalThing.setWho("us");
		URI uri = new URI("mem:thing");
		URIParcel<Object> parcel = new URIParcel<Object>(Object.class,uri);
		parcel.put(originalThing);
		parcel = new URIParcel<Object>(Object.class,uri);
		Object po = parcel.call();
		Assert.assertEquals(SomeThing.class,po.getClass());
		Assert.assertFalse(po==originalThing);
		Assert.assertEquals(po,originalThing);
	}
	
	@Test
	public void testClasspathObject() throws Exception{
		URI uri = new URI("classpath:someData.txt");
		URIParcel<String> parcel = new URIParcel<String>(String.class,uri);
		Assert.assertEquals("This is some really good data",parcel.call());
	}
	
	public static class SomeThing implements Serializable{
		private static final long serialVersionUID = 1661081986153145279L;
		private String where;
		private String what;
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((what == null) ? 0 : what.hashCode());
			result = prime * result + ((when == null) ? 0 : when.hashCode());
			result = prime * result + ((where == null) ? 0 : where.hashCode());
			result = prime * result + ((who == null) ? 0 : who.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SomeThing other = (SomeThing) obj;
			if (what == null) {
				if (other.what != null)
					return false;
			} else if (!what.equals(other.what))
				return false;
			if (when == null) {
				if (other.when != null)
					return false;
			} else if (!when.equals(other.when))
				return false;
			if (where == null) {
				if (other.where != null)
					return false;
			} else if (!where.equals(other.where))
				return false;
			if (who == null) {
				if (other.who != null)
					return false;
			} else if (!who.equals(other.who))
				return false;
			return true;
		}
		private String who;
		private Date when;
		public String getWhere() {
			return where;
		}
		public void setWhere(String where) {
			this.where = where;
		}
		public String getWhat() {
			return what;
		}
		public void setWhat(String what) {
			this.what = what;
		}
		public String getWho() {
			return who;
		}
		public void setWho(String who) {
			this.who = who;
		}
		public Date getWhen() {
			return when;
		}
		public void setWhen(Date when) {
			this.when = when;
		}
	}
}

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
package com.disney.uriparcel.json.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.disney.uriparcel.URIParcel;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;

public class TestJsonParcels {
	@Rule
	public WireMockRule wireMockRule = new WireMockRule(28187); // No-args constructor defaults to port 8080

	@Test
	public void testJsonFile() throws Exception{
		File myMapFile = File.createTempFile("testMapParcel", ".json");
		FileWriter fw = new FileWriter(myMapFile);
		try{
			fw.write("{\"a\":1,\"b\":\"foo\"}");
		}
		finally{
			fw.close();
		}
		try{
			URI myUri = myMapFile.toURI();
			@SuppressWarnings("rawtypes")
			URIParcel<Map> myParcel = new URIParcel<>(Map.class, myUri);
			Map<?,?> result = myParcel.call();
			Assert.assertEquals(1.0, result.get("a"));
		}
		finally{
			myMapFile.delete();
		}
	}
	
	@Test
	public void testJsonHttpCustomClass() throws Exception{
		Gson gson = new Gson();
		GarbageTruck gt = new GarbageTruck();
		gt.setCapacity(100);
		gt.setUsage(50);
		String doc=gson.toJson(gt);
		String lmod = wiremock.org.apache.http.client.utils.DateUtils.formatDate(new Date());
		
		stubFor(get(urlEqualTo("/sample.json"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Last-Modified", lmod)
						.withBody(doc)));
		URIParcel<GarbageTruck> myDoc = new URIParcel<GarbageTruck>(GarbageTruck.class, new URI("http://localhost:28187/sample.json"));
		GarbageTruck received = myDoc.call();
		Assert.assertEquals(gt.getCapacity(), received.getCapacity());
		Assert.assertEquals(gt.getUsage(), received.getUsage());
	}
	
	@Test
	public void testSerializedHttpCustomClass() throws Exception{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		GarbageTruck gt = new GarbageTruck();
		gt.setCapacity(120);
		gt.setUsage(90);
		oos.writeObject(gt);
		oos.flush();
		byte[] content = baos.toByteArray();

		String lmod = wiremock.org.apache.http.client.utils.DateUtils.formatDate(new Date());
		
		stubFor(get(urlEqualTo("/sample.ser"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Last-Modified", lmod)
						.withBody(content)));
		URIParcel<GarbageTruck> myDoc = new URIParcel<GarbageTruck>(GarbageTruck.class, new URI("http://localhost:28187/sample.ser"));
		GarbageTruck received = myDoc.call();
		Assert.assertEquals(gt.getCapacity(), received.getCapacity());
		Assert.assertEquals(gt.getUsage(), received.getUsage());
	}
	
	public static class GarbageTruck implements Serializable{
		private static final long serialVersionUID = 2774500462342981450L;
		private int capacity;
		private int usage;
		public int getCapacity() {
			return capacity;
		}
		public void setCapacity(int capacity) {
			this.capacity = capacity;
		}
		public int getUsage() {
			return usage;
		}
		public void setUsage(int usage) {
			this.usage = usage;
		}
	}
}

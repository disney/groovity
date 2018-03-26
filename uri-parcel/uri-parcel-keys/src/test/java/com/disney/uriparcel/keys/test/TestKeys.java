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
package com.disney.uriparcel.keys.test;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.Assert;
import org.junit.Test;

import com.disney.uriparcel.URIParcel;


public class TestKeys {

	@Test public void testKeyPair() throws Exception{
		URI keyLoc = new URI("mem:myPrivateKey");
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();
		URIParcel<KeyPair> myParcel = new URIParcel<KeyPair>(KeyPair.class,keyLoc);
		myParcel.put(keyPair);
		URIParcel<byte[]> rawParcel = new URIParcel<byte[]>(byte[].class,keyLoc);
		Assert.assertTrue("Expected PKCS8 private key file", new String(rawParcel.call()).startsWith("-----BEGIN RSA PRIVATE KEY-----"));
		myParcel = new URIParcel<KeyPair>(KeyPair.class,keyLoc);
		KeyPair copy = myParcel.call();
		Assert.assertEquals(keyPair.getPrivate(), copy.getPrivate());
	}
	
	
	@Test public void testEncryptedKeyPair() throws Exception{
		URI keyLoc = new URI("mem:myEncryptedPrivateKey");
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();
		Map<String,Object> config = new HashMap<>();
		config.put("password", "hooloo");
		URIParcel<KeyPair> myParcel = new URIParcel<KeyPair>(KeyPair.class,keyLoc,config);
		myParcel.put(keyPair);
		URIParcel<byte[]> rawParcel = new URIParcel<byte[]>(byte[].class,keyLoc);
		Matcher encryptedPrivateMatcher = Pattern.compile("^-----BEGIN RSA PRIVATE KEY-----\\s+Proc-Type: 4,ENCRYPTED").matcher(new String(rawParcel.call()));
		Assert.assertTrue("Expected PKCS8 encrypted private key file", encryptedPrivateMatcher.find());
		Exception e = null;
		try{
			myParcel = new URIParcel<KeyPair>(KeyPair.class,keyLoc);
			myParcel.call();
		}
		catch(IllegalArgumentException e1){
			e = e1;
		}
		Assert.assertNotNull("Expected error", e);
		myParcel = new URIParcel<KeyPair>(KeyPair.class,keyLoc,config);
		KeyPair copy = myParcel.call();
		Assert.assertEquals(keyPair.getPrivate(), copy.getPrivate());
	}
	
	@Test public void testPublicKey() throws Exception{
		URI keyLoc = new URI("mem:myPublicKey");
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();
		URIParcel<PublicKey> myParcel = new URIParcel<PublicKey>(PublicKey.class,keyLoc);
		myParcel.put(keyPair.getPublic());
		URIParcel<byte[]> rawParcel = new URIParcel<byte[]>(byte[].class,keyLoc);
		Assert.assertTrue("Expected PKCS8 public key file", new String(rawParcel.call()).startsWith("-----BEGIN PUBLIC KEY-----"));
		myParcel = new URIParcel<PublicKey>(PublicKey.class,keyLoc);
		PublicKey copy = myParcel.call();
		Assert.assertEquals(keyPair.getPublic(), copy);
	}
	
	@Test public void testCertificate() throws Exception{
		URI keyLoc = new URI("mem:myCertificate");
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();
		X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
			new X500Name("CN=Some authority, OU=DATG, O=Disney, C=US"), 
			new BigInteger(64, new SecureRandom()), 
			//yesterday
			new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000), 
			//10 years
			new Date(System.currentTimeMillis() + 10 * 365 * 24 * 60 * 60 * 1000), 
			new X500Name("DN=mySubject"), 
			SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
		);
		JcaContentSignerBuilder builder = new JcaContentSignerBuilder("SHA256withRSA");
		ContentSigner signer = builder.build(keyPair.getPrivate());
		byte[] certBytes = certBuilder.build(signer).getEncoded();
		Certificate cert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certBytes));
		URIParcel<Certificate> certParcel = new URIParcel<Certificate>(Certificate.class,keyLoc);
		certParcel.put(cert);
		byte[] rawBytes = URIParcel.get(keyLoc, byte[].class);
		Assert.assertTrue("Expected X509 certificate", new String(rawBytes).startsWith("-----BEGIN CERTIFICATE-----"));
		Certificate rc = URIParcel.get(keyLoc, Certificate.class);
		Assert.assertEquals(keyPair.getPublic(), rc.getPublicKey());
	}
}

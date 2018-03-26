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
package com.disney.http.auth.server.test;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Callable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.disney.http.auth.Algorithms;
import com.disney.http.auth.AuthConstants;
import com.disney.http.auth.SignatureAuthorization;
import com.disney.http.auth.keychain.KeyChain;
import com.disney.http.auth.keychain.KeyStoreKeyChainImpl;
import com.disney.http.auth.server.ServerAuthorizationRequest;
import com.disney.http.auth.server.ServletAuthorizationRequest;
import com.disney.http.auth.server.VerifierResult;
import com.disney.http.auth.server.signature.SignatureVerifierImpl;

public class TestHttpSignature implements AuthConstants{
	
	@BeforeClass
	public static void setup(){
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testSigning() throws Exception{
		SignatureVerifierImpl verifier = new SignatureVerifierImpl();
		verifier.setMaxDateDrift(5000);
		final KeyStore testStore = KeyStore.getInstance("JCEKS");
		testStore.load(null);
		Key hmac256key = new SecretKeySpec("hello world".getBytes(), "HmacSHA256");
		testStore.setKeyEntry("hmac256key", hmac256key, new char[0], null);
		verifier.setKeyChains(Arrays.asList((KeyChain)new KeyStoreKeyChainImpl(new Callable<KeyStore>() {
			@Override
			public KeyStore call() throws Exception {
				return testStore;
			}
		}, new char[0])));
		DateFormat headerDateFormat  = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		verifier.setRequiredHeaders(Arrays.asList(REQUEST_TARGET,"date"));
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServerAuthorizationRequest areq = new ServletAuthorizationRequest(request);
		//FIRST TEST: missing signature
		VerifierResult result = verifier.verify(areq);
		Assert.assertEquals(ERROR_MISSING_SIGNATURE, result.getMessage());
		SignatureAuthorization signature = new SignatureAuthorization();
		signature.setAlgorithm("rsa-sha256");
		signature.setKeyId("rsa256key");
		signature.setHeaders(new ArrayList<String>());
		signature.setSignature(new byte[0]);
		request.addHeader("Authorization", "Signature "+signature.toString());
		//SECOND TEST: missing REQUEST_TARGET
		result = verifier.verify(areq);
		Assert.assertEquals(MessageFormat.format(ERROR_MISSING_HEADER_FORMAT, REQUEST_TARGET), result.getMessage());
		signature.setHeaders(Arrays.asList(REQUEST_TARGET));
		request = new MockHttpServletRequest();
		areq = new ServletAuthorizationRequest(request);
		request.addHeader("Authorization", "Signature "+signature.toString());
		//THIRD TEST: missing date
		result = verifier.verify(areq);
		Assert.assertEquals(MessageFormat.format(ERROR_MISSING_HEADER_FORMAT, "date"), result.getMessage());
		signature.setHeaders(Arrays.asList(REQUEST_TARGET,"date"));
		request = new MockHttpServletRequest();
		areq = new ServletAuthorizationRequest(request);
		request.addHeader("Authorization", "Signature "+signature.toString());
		request.addHeader("Date", headerDateFormat.format(new Date(System.currentTimeMillis()-6000)));
		
		//FOURTH TEST: out-of-range date
		result = verifier.verify(areq);
		Assert.assertEquals(ERROR_INVALID_DATE, result.getMessage());
		
		request = new MockHttpServletRequest();
		areq = new ServletAuthorizationRequest(request);
		request.addHeader("Authorization", "Signature "+signature.toString());
		request.addHeader("Date", headerDateFormat.format(new Date(System.currentTimeMillis()-3000)));
		
		//FIFTH TEST: unknown key ID
		result = verifier.verify(areq);
		Assert.assertEquals(MessageFormat.format(ERROR_UNKOWN_KEY_ID_FORMAT, signature.getKeyId()), result.getMessage());
		
		signature.setKeyId("hmac256key");
		request = new MockHttpServletRequest();
		areq = new ServletAuthorizationRequest(request);
		request.addHeader("Authorization", "Signature "+signature.toString());
		request.addHeader("Date", headerDateFormat.format(new Date(System.currentTimeMillis()-3000)));
		//SIXTH TEST: rsa mismatch
		result = verifier.verify(areq);
		Assert.assertEquals(MessageFormat.format(ERROR_EXPECTED_RSA_FORMAT, signature.getKeyId()), result.getMessage());
		
		KeyPair keypair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		X509Certificate certificate = generateCertificate(keypair);  
		testStore.setKeyEntry("rsa256key", keypair.getPrivate(), new char[0], new Certificate[] {certificate});
		signature.setKeyId("rsa256key");
		signature.setAlgorithm("hmac-sha256");
		request = new MockHttpServletRequest();
		areq = new ServletAuthorizationRequest(request);
		request.addHeader("Authorization", "Signature "+signature.toString());
		request.addHeader("Date", headerDateFormat.format(new Date(System.currentTimeMillis()-3000)));
		//Seventh TEST: hmac mismatch
		result = verifier.verify(areq);
		Assert.assertEquals(MessageFormat.format(ERROR_EXPECTED_HMAC_FORMAT, signature.getKeyId()), result.getMessage());
		
		signature.setAlgorithm("rsa-sha256");
		request = new MockHttpServletRequest();
		areq = new ServletAuthorizationRequest(request);
		request.addHeader("Authorization", "Signature "+signature.toString());
		request.addHeader("Date", headerDateFormat.format(new Date(System.currentTimeMillis()-3000)));
		
		//EIGHT test: invalid signature
		Exception sigEx = null;
		try{
			verifier.verify(areq);
		}
		catch(Exception e){
			sigEx=e;
		}
		Assert.assertNotNull(sigEx);
		
		//NINTH test: good signature
		request = new MockHttpServletRequest();
		areq = new ServletAuthorizationRequest(request);
		request.setMethod("GET");
		request.setRequestURI("/");
		request.addHeader("Date", headerDateFormat.format(new Date(System.currentTimeMillis()-3000)));
		String signingString = "(request-target): get /\ndate: "+request.getHeader("date");
		byte[] sigBytes = signMessage(keypair.getPrivate(), signingString, "rsa-sha256");
		signature.setSignature(sigBytes);
		request.addHeader("Authorization", "Signature "+signature.toString());
		result = verifier.verify(areq);
		Assert.assertTrue("Verification failed",result.isAuthenticated());
		
		//TENTH test: bad signature
		request = new MockHttpServletRequest();
		areq = new ServletAuthorizationRequest(request);
		request.setMethod("GET");
		request.setRequestURI("/nogood");
		request.addHeader("Date", headerDateFormat.format(new Date(System.currentTimeMillis()-3000)));
		signingString = "(request-target): get /\ndate: "+request.getHeader("date");
		sigBytes = signMessage(keypair.getPrivate(), signingString, "rsa-sha256");
		signature.setSignature(sigBytes);
		request.addHeader("Authorization", "Signature "+signature.toString());
		result = verifier.verify(areq);
		Assert.assertFalse("Verification succeed when it should have failed",result.isAuthenticated());
		Assert.assertEquals(ERROR_VERIFICATION_FAILED, result.getMessage());
		
	}
	
	public static final byte[] signMessage(Key key, String message, String algorithm) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, UnsupportedEncodingException{
		String signingAlgorithm = Algorithms.getSecurityAlgorithm(algorithm);
		if(signingAlgorithm.endsWith("RSA")){
			Signature signer = Signature.getInstance(signingAlgorithm);
			signer.initSign((PrivateKey) key);
			signer.update(message.getBytes("UTF-8"));
			return signer.sign();
		}
		else if(signingAlgorithm.startsWith("Hmac")){
			Mac mac = Mac.getInstance(signingAlgorithm);
			mac.init(key);
			mac.update(message.getBytes("UTF-8"));
			return mac.doFinal();
		}
		else{
			throw new NoSuchAlgorithmException("No known algorithm for "+algorithm);
		}
	}
	
	public X509Certificate generateCertificate(KeyPair keyPair) throws Exception{  
		X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
			new X500Name("CN=localhost"), 
			new BigInteger(64, new SecureRandom()), 
			//yesterday
			new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000), 
			//10 years
			new Date(System.currentTimeMillis() + 10 * 365 * 24 * 60 * 60 * 1000), 
			new X500Name("CN=localhost"), 
			SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
		);
		JcaContentSignerBuilder builder = new JcaContentSignerBuilder("SHA256withRSA");
		ContentSigner signer = builder.build(keyPair.getPrivate());
		byte[] certBytes = certBuilder.build(signer).getEncoded();
		return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certBytes));
	}
	
}

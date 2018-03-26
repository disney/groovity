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
package com.disney.http.auth.client.test;

import com.disney.http.auth.Algorithms;
import com.disney.http.auth.AuthConstants;
import com.disney.http.auth.SignatureAuthorization;
import com.disney.http.auth.client.ClientAuthorizationRequest;
import com.disney.http.auth.client.KeyUtils;
import com.disney.http.auth.client.keyloader.KeyObjectKeyLoader;
import com.disney.http.auth.client.keyloader.KeyChainKeyLoader;
import com.disney.http.auth.client.signer.HttpSignatureSigner;
import com.disney.http.auth.keychain.KeyChain;
import com.disney.http.auth.keychain.KeyStoreKeyChainImpl;
import com.disney.uriparcel.URIParcel;
import com.disney.uriparcel.value.KeyStoreValueHandler;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.junit.Assert;
import org.junit.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import java.io.File;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Rachel Kobayashi
 */
public class TestSignatureAuth implements AuthConstants {
    SimpleDateFormat utc = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    private String getAuthHeader(HttpRequest request) {
        if (request != null) {
            Header authHeader = request.getLastHeader(AUTHORIZATION_HEADER);
            if (authHeader == null) {
                authHeader = request.getLastHeader(SIGNATURE_HEADER);
            }
            if (authHeader != null) {
                return authHeader.getValue();
            }
        }
        return null;
    }

    private byte[] signHmac(String algorithm, String keyValue, String data) throws Exception{
        String securityAlgorithm = Algorithms.getSecurityAlgorithm(algorithm);
        Key hmacKey = new SecretKeySpec(DatatypeConverter.parseBase64Binary(keyValue), securityAlgorithm);

        Mac mac = Mac.getInstance(securityAlgorithm);
        mac.init(hmacKey);
        byte[] encodedData = mac.doFinal(data.getBytes("UTF-8"));
        return encodedData;
    }

    private boolean verifyRsa(String algorithm, PublicKey publicKey, String data, byte[] message) throws Exception{
        Signature rsaVerify = Signature.getInstance(algorithm);
        rsaVerify.initVerify(publicKey);
        rsaVerify.update(data.getBytes("UTF-8"));
        return rsaVerify.verify(message);
    }

    @Test(expected=HttpException.class)
    public void testMissingKeyId() throws Exception {
        HttpGet request = new HttpGet("http://localhost:8080/");
        HttpClientContext localContext = new HttpClientContext();

        HttpSignatureSigner signer = new HttpSignatureSigner();
        signer.process(request, localContext);
    }

    @Test
    public void testGeneralSettings() throws Exception {
        HttpGet request = new HttpGet("http://localhost:8080/");
        HttpClientContext localContext = new HttpClientContext();

        HttpSignatureSigner signer = new HttpSignatureSigner();

        String keyId = "apiUser123";
        String keyValue = "someBase64Secret";
        String headers = "(request-target) host x-date";
        String algorithm = "hmac-sha256";

        // check default header was set
        Assert.assertEquals(AUTHORIZATION_HEADER, signer.getHeaderName());

        //check all contents got set correctly
        signer.setHeaderName(SIGNATURE_HEADER);
        signer.setKeyId(keyId);
        signer.setAlgorithm(algorithm);
        signer.setKeyLoader(new KeyObjectKeyLoader(new SecretKeySpec(keyValue.getBytes(), "HmacSHA256")));
        signer.process(request, localContext);

        // no headers specified, should have added 'Date' header
        Assert.assertEquals(signer.getHeaders().get(0), "Date");

        signer.setHeaders(Arrays.asList(headers.split(" ")));
        signer.process(request, localContext);

        Assert.assertEquals(SIGNATURE_HEADER, signer.getHeaderName());

        String authHeader = getAuthHeader(request);
        String[] signatureParts = authHeader.split(",");
        for(int i=0; i<signatureParts.length; i++) {
            String attributeString = (signatureParts[i]);
            String[] attributeParts = attributeString.split("=");
            String key = attributeParts[0];
            String value = attributeParts[1];
            if(key == "keyId") {
                Assert.assertEquals(keyId, value);
            } else if(key == "algorithm") {
                Assert.assertEquals(algorithm, value);
            } else if(key == "headers") {
                Assert.assertEquals(headers, value);
            }
        }
    }

    @Test
    public void testHmac() throws Exception{
        HttpGet request = new HttpGet("http://localhost:8080/");
        HttpClientContext localContext = new HttpClientContext();

        HttpSignatureSigner signer = new HttpSignatureSigner();
        signer.setHeaderName(SIGNATURE_HEADER);

        String keyId = "apiUser123";
        String keyValue = "someBase64Secret";
        String headers = "(request-target) host x-date";
        String algorithm = "hmac-sha256";

        KeyObjectKeyLoader hmacKey = new KeyObjectKeyLoader(algorithm, keyValue);

        signer.setHeaderName(SIGNATURE_HEADER);
        signer.setKeyId(keyId);
        signer.setKeyLoader(hmacKey);
        signer.setAlgorithm(algorithm);
        signer.process(request, localContext);
        
        SignatureAuthorization testAuth = new SignatureAuthorization();
        testAuth.setHeaders(signer.getHeaders());

        Assert.assertNotNull(signer.getHeaderName());
        Assert.assertNotNull(getAuthHeader(request));
        String signingString =  testAuth.generateSigningString(new ClientAuthorizationRequest(request));
        byte[] expectedResult = signHmac(algorithm, keyValue,signingString);
        byte[] signature = signer.doAuthorization(request).getSignature();
        Assert.assertArrayEquals(expectedResult, signature);

        // bad signing string
        Assert.assertFalse(Arrays.equals(signHmac(algorithm, keyValue, signingString + "invalid"), signature));

        // wrong key
        signer.setKeyLoader(new KeyObjectKeyLoader(algorithm, "differentKeyValue"));
        signer.process(request, localContext);
        signature = signer.doAuthorization(request).getSignature();
        Assert.assertFalse("Wrong Key", Arrays.equals(expectedResult, signature));

        // wrong algorithm
        signer.setAlgorithm("hmac-md5");
        signer.process(request, localContext);
        signature = signer.doAuthorization(request).getSignature();
        Assert.assertFalse("Wrong algorithm", Arrays.equals(expectedResult, signature));

        // wrong headers
        signer.setHeaders(Arrays.asList(headers.split(" ")));
        signer.setAlgorithm(algorithm);
        signer.setKeyLoader(hmacKey);
        signer.process(request, localContext);
        signature =  signer.doAuthorization(request).getSignature();
        Assert.assertFalse("Incorrect Headers", Arrays.equals(expectedResult, signature));

        // wrong header order
        signer.setHeaders(Arrays.asList("host (request-target) x-date"));
        signer.process(request, localContext);
        signature =  signer.doAuthorization(request).getSignature();
        Assert.assertFalse("Incorrect header order", Arrays.equals(expectedResult, signature));

    }

    @Test
    public void testRSA() throws Exception{
        HttpGet request = new HttpGet("http://localhost:8080/");
        HttpClientContext localContext = new HttpClientContext();

        HttpSignatureSigner signer = new HttpSignatureSigner();
        signer.setHeaderName(SIGNATURE_HEADER);

        String keyId = "apiUser123";
        String headers = "(request-target) host x-date";

        KeyPair pair = KeyUtils.generateKeyPair();
        PrivateKey privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();
        KeyObjectKeyLoader privateKeyLoader = new KeyObjectKeyLoader(privateKey);

        signer.setAlgorithm("rsa-sha256");
        signer.setKeyId(keyId);
        signer.setHeaders(Arrays.asList(headers.split(" ")));
        signer.setKeyLoader(privateKeyLoader);
        signer.process(request, localContext);
        
        SignatureAuthorization testAuth = new SignatureAuthorization();
        testAuth.setAlgorithm("rsa-sha256");
        testAuth.setHeaders(signer.getHeaders());
        String signingString =  testAuth.generateSigningString(new ClientAuthorizationRequest(request));
        
        byte[] encryptedString = signer.doAuthorization(request).getSignature();

        boolean verify = verifyRsa("SHA256withRSA", publicKey, signingString, encryptedString);
        Assert.assertTrue(verify);

        // can choose algorithm
        signer.setAlgorithm("rsa-md5");
        signer.process(request, localContext);
        encryptedString = signer.doAuthorization(request).getSignature();
        verify = verifyRsa("MD5withRSA", publicKey, signingString, encryptedString);
        Assert.assertTrue(verify);

        // wrong keyid, not a key loader so no effect
        signer.setAlgorithm("rsa-sha256");
        signer.setKeyId("something else");
        signer.process(request, localContext);
        encryptedString = signer.doAuthorization(request).getSignature();
        verify = verifyRsa("SHA256withRSA", publicKey, signingString, encryptedString);
        Assert.assertTrue(verify);

        // different headers
        signer.setHeaders(Arrays.asList("host","x-date"));
        signer.process(request, localContext);
        encryptedString = signer.doAuthorization(request).getSignature();
        verify = verifyRsa("SHA256withRSA", publicKey, signingString, encryptedString);
        Assert.assertFalse(verify);

        // load plain key from file;
        String location = "target/priv.pem";
        File pemFile = new File(location);
        URIParcel.put(pemFile.toURI(), pair);
        
        URIParcel<KeyPair> pemParcel = new URIParcel<KeyPair>(KeyPair.class,pemFile.toURI());
       
        signer = new HttpSignatureSigner();
        signer.setHeaderName(SIGNATURE_HEADER);
        signer.setKeyId("defaultValue");
        signer.setAlgorithm("rsa-sha256");
        signer.setHeaders(Arrays.asList(headers.split(" ")));
        signer.setKeyPairLoader(pemParcel);
        signingString =  testAuth.generateSigningString(new ClientAuthorizationRequest(request));
        encryptedString = signer.doAuthorization(request).getSignature();
        verify = verifyRsa("SHA256withRSA", publicKey, signingString, encryptedString);
        Assert.assertTrue(verify);

        // try using a KeyStoreLoader
        signer = new HttpSignatureSigner();
        signer.setHeaderName(SIGNATURE_HEADER);
        signer.setAlgorithm("rsa-sha256");
        location = "target/testKeytool.store";
        
        Map<String,Object> config = new HashMap<String,Object>();
    	config.put(KeyStoreValueHandler.KEYSTORE_PASSWORD, "rachel");
    	config.put(KeyStoreValueHandler.KEYSTORE_TYPE, "JCEKS");
    	
    	URIParcel<KeyStore> parcel = new URIParcel<KeyStore>(KeyStore.class,new File(location).toURI(),config);
 
    	KeyChain chain = new KeyStoreKeyChainImpl(parcel,"".toCharArray());
        KeyChainKeyLoader keystoreLoader = new KeyChainKeyLoader(chain);
        keystoreLoader.setAlias("test");
        
        signer.setKeyId("test");
        signer.setHeaders(Arrays.asList(headers.split(" ")));
        signer.setKeyLoader(keystoreLoader);
        signer.process(request, localContext);
        signingString =  testAuth.generateSigningString(new ClientAuthorizationRequest(request));
        encryptedString = signer.doAuthorization(request).getSignature();


        // check again public key
  
        KeyStore importedKeystore = parcel.call();
        PublicKey loadedPublicKey = importedKeystore.getCertificate("test").getPublicKey();
        verifyRsa("SHA256withRSA", loadedPublicKey, signingString, encryptedString);
        Assert.assertTrue(verify);

    }
}

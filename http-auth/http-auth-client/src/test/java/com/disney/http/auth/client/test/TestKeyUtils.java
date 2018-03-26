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

import com.disney.http.auth.client.KeyUtils;
import com.disney.http.auth.client.keyloader.KeyChainKeyLoader;
import com.disney.http.auth.keychain.KeyChain;
import com.disney.http.auth.keychain.KeyStoreKeyChainImpl;
import com.disney.uriparcel.URIParcel;
import com.disney.uriparcel.value.KeyStoreValueHandler;

import org.apache.http.client.protocol.HttpClientContext;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;

import javax.xml.bind.DatatypeConverter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Rachel Kobayashi
 */
public class TestKeyUtils {
    HttpClientContext context = new HttpClientContext();
    String targetDirectory = "";

    @BeforeClass
    public static void setup(){
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void TestKeyGeneration() throws Exception {
        KeyPair pair = KeyUtils.generateKeyPair(2048);
        Assert.assertNotNull(pair);

        PrivateKey privateKey = pair.getPrivate();
        Assert.assertNotNull(privateKey);
        Assert.assertEquals(privateKey.getAlgorithm(), "RSA");

        PublicKey publicKey = pair.getPublic();
        Assert.assertNotNull(publicKey);
        Assert.assertEquals(publicKey.getAlgorithm(), "RSA");

        String alias = "apiUser123";

        // Store private key
        File privateKeyFile = new File("target/testPrivateKey.pem");
        File privateKeyStore = new File("target/testKey.store");
        String privatePassword = "passwordForPrivateKey";
        URIParcel.put(privateKeyFile.toURI(), pair);
        writePrivateKeystoreToFile(pair, privateKeyStore.getAbsolutePath(), alias, privatePassword);

        // test retrieval of KeyStore
    
    	Map<String,Object> config = new HashMap<String,Object>();
    	config.put(KeyStoreValueHandler.KEYSTORE_PASSWORD, privatePassword);
    	config.put(KeyStoreValueHandler.KEYSTORE_TYPE, "JCEKS");
    	URIParcel<KeyStore> parcel = new URIParcel<KeyStore>(KeyStore.class,privateKeyStore.toURI(),config);

    	KeyChain chain = new KeyStoreKeyChainImpl(parcel,"".toCharArray());
        KeyChainKeyLoader privateKeyStoreLoader = new KeyChainKeyLoader(chain);
        
        privateKeyStoreLoader.setAlias(alias);
        PrivateKey loadedKeyFromKeyStore = (PrivateKey) privateKeyStoreLoader.call();
        Assert.assertEquals(privateKey, loadedKeyFromKeyStore);

        // test retrieval of Key
        URIParcel<KeyPair> pkParcel = new URIParcel<KeyPair>(KeyPair.class,privateKeyFile.toURI());

        KeyPair loadedPrivateKey = pkParcel.call();
        Assert.assertArrayEquals(privateKey.getEncoded(), loadedPrivateKey.getPrivate().getEncoded());

        // check storage and retrieval of public key
        String publicKeyFileName = "testPublicKey.store";
        String password = "publicPassword";
        File publicKeyFile = new File("target/"+publicKeyFileName);
        KeyUtils.writePublicKeyStoreToFile(publicKey, publicKeyFile.getAbsolutePath(), alias, password);
        PublicKey loadedPublicKey = loadPublicKeyStore(publicKeyFile.getAbsolutePath(), alias, password);
        Assert.assertArrayEquals(publicKey.getEncoded(), loadedPublicKey.getEncoded());

        // check if valid key pair by trying to sign and verify a string
        String testString = "This is the message to encode.";
        Signature rsaSign = Signature.getInstance("MD5withRSA");
        rsaSign.initSign(privateKey);
        rsaSign.update(testString.getBytes("UTF-8"));
        byte[] signedMessage = rsaSign.sign();

        Signature rsaVerify = Signature.getInstance("MD5withRSA");
        rsaVerify.initVerify(loadedPublicKey);
        rsaVerify.update(testString.getBytes("UTF-8"));
        boolean valid = rsaVerify.verify(signedMessage);
        Assert.assertTrue(valid);

        // make new key pair and save KeyStore to file.
        KeyPair keyPairForFile = KeyUtils.generateKeyPair();
        String location = new File("target/testKeytool.store").getAbsolutePath();
        writePrivateKeystoreToFile(keyPairForFile,location, "test", "rachel");

        // load from file
        config = new HashMap<String,Object>();
    	config.put(KeyStoreValueHandler.KEYSTORE_PASSWORD, "rachel");
    	config.put(KeyStoreValueHandler.KEYSTORE_TYPE, "JCEKS");
    	URIParcel<KeyStore> ksParcel = new URIParcel<KeyStore>(KeyStore.class,new File(location).toURI(),config);

    	KeyChain chain2 = new KeyStoreKeyChainImpl(ksParcel,"".toCharArray());
        KeyChainKeyLoader savedKeyStoreLoader = new KeyChainKeyLoader(chain2);
        savedKeyStoreLoader.setAlias("test");
        Key newKey = savedKeyStoreLoader.call();
        Assert.assertEquals(keyPairForFile.getPrivate(), newKey);

        KeyStore importedKeystore = ksParcel.call();
        Assert.assertEquals(keyPairForFile.getPublic(), importedKeystore.getCertificate("test").getPublicKey());
    }

    @Test
    public void testKeyStringParsing() throws Exception {
        String testKeyData = "Encode this text: May 1, 2015";

        String base64KeyData = DatatypeConverter.printBase64Binary(testKeyData.getBytes());

        // no preface around key;
        Assert.assertEquals("No preface", base64KeyData, KeyUtils.findKey(base64KeyData));

        String privateKeyString = "-----BEGIN PRIVATE KEY-----"+
                base64KeyData+"-----END PRIVATE KEY-----";
        Assert.assertEquals("Private Key", base64KeyData, KeyUtils.findKey(privateKeyString));

        String publicKeyString = "-----BEGIN PUBLIC KEY-----"+
                base64KeyData+"-----END PUBLIC KEY-----";
        Assert.assertEquals("Public Key", base64KeyData, KeyUtils.findKey(publicKeyString));

        privateKeyString = "-----BEGIN PRIVATE KEY-----\n\n\n\n\n\n"+
                base64KeyData+"\n\n-----END PRIVATE KEY-----";
        Assert.assertEquals("With extra new lines", base64KeyData, KeyUtils.findKey(privateKeyString));
    }

    private PublicKey loadPublicKeyStore(String fileName, String alias, String password) throws Exception{
    	Map<String,Object> config = new HashMap<String,Object>();
    	config.put(KeyStoreValueHandler.KEYSTORE_PASSWORD, password);
    	config.put(KeyStoreValueHandler.KEYSTORE_TYPE, "JCEKS");
    	URIParcel<KeyStore> parcel = new URIParcel<KeyStore>(KeyStore.class,new File(fileName).toURI(),config);

        KeyStore ks = parcel.call();
        PublicKey publicKey = (PublicKey) ks.getKey(alias, password.toCharArray());
        return publicKey;
    }

    private void writePrivateKeystoreToFile(KeyPair keyPair, String fileName, String alias, String password) throws Exception{
        Certificate certificate = generateCertificate(keyPair);
        KeyStore testStore = KeyStore.getInstance("JCEKS");
        testStore.load(null);
        testStore.setKeyEntry(alias, keyPair.getPrivate(), new char[0], new java.security.cert.Certificate[] {certificate});
        FileOutputStream keyFOS = new FileOutputStream(fileName);
        testStore.store(keyFOS, password.toCharArray());
        keyFOS.close();
    }

    public Certificate generateCertificate(KeyPair keyPair) throws Exception{
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
        return cert;
    }

}

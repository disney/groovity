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

import com.disney.http.auth.client.keyloader.KeyChainKeyLoader;
import com.disney.http.auth.keychain.KeyChain;
import com.disney.http.auth.keychain.KeyStoreKeyChainImpl;
import com.disney.uriparcel.URIParcel;
import com.disney.uriparcel.value.KeyStoreValueHandler;

import org.apache.http.client.protocol.HttpClientContext;
import org.junit.Test;

import java.io.File;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rachel Kobayashi
 */
public class TestKeyStoreKeyLoader {
    HttpClientContext context = new HttpClientContext();

    private KeyChainKeyLoader setupKeyLoader(String keystorePassword){
    	Map<String,Object> config = new HashMap<String,Object>();
    	config.put(KeyStoreValueHandler.KEYSTORE_PASSWORD, keystorePassword);
    	config.put(KeyStoreValueHandler.KEYSTORE_TYPE, "JCEKS");
    	
    	URIParcel<KeyStore> parcel = new URIParcel<KeyStore>(KeyStore.class,new File("src/test/resources/testKey.store").toURI(),config);
    	KeyChain chain = new KeyStoreKeyChainImpl(parcel,"".toCharArray());
        KeyChainKeyLoader loader = new KeyChainKeyLoader(chain);
        return loader;
    }
    // no alias, no algorithm, throw exception.
    @Test(expected=Exception.class)
    public void testImportKeystoreMissingProperties() throws Exception{
    	KeyChainKeyLoader loader = setupKeyLoader(null);
        loader.call();
    }

    // no algorithm, should throw exception
    @Test(expected=Exception.class)
    public void testImportKeystoreMissingPassword() throws Exception{
    	KeyChainKeyLoader loader = setupKeyLoader(null);
        loader.setAlias("apiUser123");
        loader.call();
    }


    // alias has no key, should throw exception
    @Test(expected=UnrecoverableKeyException.class)
    public void testImportKeystoreWrongAlias() throws Exception{
    	KeyChainKeyLoader loader = setupKeyLoader("passwordForPrivateKey");
    	loader.setAlias("test");
        loader.call();
    }
/*
    // wrong type set for keystore, should throw error
    @Test(expected=Exception.class)
    public void testImportKeystoreWrongType() throws Exception{
        KeyChainKeyLoader loader = new KeyChainKeyLoader("/testKey.store", context, "passwordForPrivateKey");
        loader.setAlias("apiUser123");
        loader.setAlgorithm("RSA");
        loader.setKeystoreType("JKS");
        loader.load();
    }
    */

    // all requirements satisfied, should work since default keystore type is set.
    @Test
    public void testImportKeystoreDefaultType() throws Exception{
    	KeyChainKeyLoader loader = setupKeyLoader("passwordForPrivateKey");
        loader.setAlias("apiUser123");
        loader.call();
    }
}

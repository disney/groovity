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
import com.disney.http.auth.client.keyloader.KeyObjectKeyLoader;
import org.apache.http.client.protocol.HttpClientContext;
import org.junit.Assert;
import org.junit.Test;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.*;

/**
 * @author Rachel Kobayashi
 */
public class TestKeyObjectKeyLoader {
    HttpClientContext context = new HttpClientContext();

    @Test(expected=NoSuchAlgorithmException.class)
    public void testKeyObjectKeyLoaderBadAlgorithm() throws Exception{
        KeyObjectKeyLoader loader = new KeyObjectKeyLoader("DSA", "something else");
        loader.call();
    }

    @Test(expected=NoSuchAlgorithmException.class)
    public void testKeyObjectKeyLoaderBadAlgorithmFormat() throws Exception{
        KeyObjectKeyLoader loader = new KeyObjectKeyLoader("HmacSHA384", "something else");
        loader.call();
    }

    @Test(expected=NoSuchAlgorithmException.class)
    public void testKeyObjectKeyLoaderBadAHmacFormat() throws Exception{
        KeyObjectKeyLoader loader = new KeyObjectKeyLoader("hacm-sha23", "something else");
        loader.call();
    }

    @Test(expected=Exception.class)
    public void testKeyObjectKeyLoaderRSA() throws Exception{
        KeyObjectKeyLoader loader = new KeyObjectKeyLoader("rsa-sha1", "something else");
        loader.call();
    }

    @Test
    public void testKeyObjectKeyLoaderGoodAlgorithms() throws Exception{
        KeyObjectKeyLoader loader = new KeyObjectKeyLoader("hmac-sha1", "something else");
        loader.call();

        loader = new KeyObjectKeyLoader("hmac-md5", "something else");
        loader.call();
    }

    @Test
    public void testKeyObjectPrivateKey() throws Exception{
        KeyPair pair = KeyUtils.generateKeyPair(2048);
        PrivateKey privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();

        // test with private key
        KeyObjectKeyLoader loader = new KeyObjectKeyLoader(privateKey);
        PrivateKey loadedPrivateKey = (PrivateKey) loader.call();
        Assert.assertEquals(privateKey, loadedPrivateKey);

        // test with public key
        loader = new KeyObjectKeyLoader(publicKey);
        PublicKey loadedPublicKey = (PublicKey) loader.call();
        Assert.assertEquals(publicKey, loadedPublicKey);

        // test with secret key
        Key key = new SecretKeySpec(DatatypeConverter.parseBase64Binary("someString"), "HmacMD5");
        loader = new KeyObjectKeyLoader(key);
        Key loadedKey = loader.call();
        Assert.assertEquals(key, loadedKey);
    }
}

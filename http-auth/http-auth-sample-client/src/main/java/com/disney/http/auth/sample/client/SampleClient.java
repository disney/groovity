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
package com.disney.http.auth.sample.client;

import com.disney.http.auth.AuthConstants;
import com.disney.http.auth.client.KeyUtils;
import com.disney.http.auth.client.keyloader.KeyObjectKeyLoader;
import com.disney.http.auth.client.keyloader.KeyChainKeyLoader;
import com.disney.http.auth.client.signer.HttpSignatureSigner;
import com.disney.http.auth.keychain.KeyChain;
import com.disney.http.auth.keychain.KeyStoreKeyChainImpl;
import com.disney.uriparcel.URIParcel;
import com.disney.uriparcel.value.KeyStoreValueHandler;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.security.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rachel Kobayashi
 */
public class SampleClient implements AuthConstants {

    public static void main(String[] args) throws Exception{
    	try{
        HttpClientBuilder clientBuilder = HttpClients.custom();

        // Ways to set the symmetric key data (HMAC):

        /*
         * Set Key value explicitly
         */
        KeyObjectKeyLoader simpleLoader = new KeyObjectKeyLoader("hmac-sha256", "someBase64Secret");

        /*
         * Configuring the HttpSignatureSigner (HttpRequestInterceptor)
         *
         *   - must set the keyId / alias
         *   - must set key/encryption/algorithm
         *   - if no headers are set, default to just using the Date header
         *   - Lastly, the signer must be added to the clientBuilder
         */

        ///// Signing for SIGNATURE Authorization with imported RSA key
        // setting the key of the singer either with a loader or a key.
        HttpSignatureSigner signer = new HttpSignatureSigner();
        signer.setKeyId("apiUser123"); 
        signer.setHeaders(Arrays.asList("(request-target)","host","x-date"));
        // set key (choose one)
//        signer.setKey(loader);
//        signer.setKey(keyLoader);
        signer.setKeyLoader(simpleLoader);
        clientBuilder.addInterceptorLast(signer);
        /////

        CloseableHttpClient client = clientBuilder.build();

            getRequest(client, "http://localhost:8080/");
 

        client.close();
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    }

    private static void getRequest(CloseableHttpClient client, String url) throws Exception{

        HttpGet request = new HttpGet(url);
        CloseableHttpResponse response = client.execute(request);
        try {
            HttpEntity entity = response.getEntity();
            if(response.getStatusLine().getStatusCode() == 401){
                System.out.println(response.getFirstHeader(WWW_AUTHENTICATE_HEADER));
                EntityUtils.consumeQuietly(entity);
            }
            else if(entity != null){
                long len = entity.getContentLength();
                if(len != -1 && len < 2048){
                    System.out.println(EntityUtils.toString(entity));
                } else {
                    // stream content out
                }
            }
        } finally {
            response.close();
        }
    }

}

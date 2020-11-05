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
package com.disney.http.auth.client.keyloader;

import com.disney.http.auth.Algorithms;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.concurrent.Callable;

/**
 * Class for loading Key objects or creating Hmac keys from the algorithm and value.
 *
 * @author Rachel Kobayashi
 */
public class KeyObjectKeyLoader implements Callable<Key> {
    private Key key;

    public KeyObjectKeyLoader(Key importKey) {
        this.key = importKey;
    }

    public KeyObjectKeyLoader(String algorithm, String keyValue) throws Exception {
        String signingAlgorithm = Algorithms.getSecurityAlgorithm(algorithm);
        if (signingAlgorithm.startsWith("Hmac")) {
            this.key = new SecretKeySpec(Base64.getDecoder().decode(keyValue), signingAlgorithm);
        } else if(signingAlgorithm.endsWith("RSA")){
            throw new Exception("Cannot use KeyObjectKeyLoader to generate RSA keys");
        }
    }

    @Override
    public Key call() throws Exception{
        return key;
    }
}

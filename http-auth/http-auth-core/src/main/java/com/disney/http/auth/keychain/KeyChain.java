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
package com.disney.http.auth.keychain;

import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;
/**
 * A minimal abstraction of KeyStores, so that calling code can make use
 * of them without worrying about entry passwords
 * 
 * This API supports an array of keys to simplify key rotation implemenations where more than
 * one valid key for a given alias might exist at a given moment in time.
 * 
 * @author Alex Vigdor
 *
 */
public interface KeyChain {
	public boolean containsKey(String keyId) throws Exception;
	public PublicKey[] getPublicKeys(String keyId) throws Exception;
	public PrivateKey[] getPrivateKeys(String keyId) throws Exception;
	public SecretKey[] getSecretKeys(String keyId) throws Exception;
}

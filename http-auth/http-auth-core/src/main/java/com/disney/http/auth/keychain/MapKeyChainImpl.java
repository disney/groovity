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

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.util.Map;

import javax.crypto.SecretKey;
/**
 * simple KeyChain implementation that holds keys in memory in a map
 *
 * @author Alex Vigdor
 */
public class MapKeyChainImpl implements KeyChain {
	private Map<String,Key> keys;
	
	public MapKeyChainImpl(){
		
	}
	
	public MapKeyChainImpl(Map<String, Key> keys){
		this.keys=keys;
	}

	@Override
	public boolean containsKey(String keyId) throws Exception {
		return keys.containsKey(keyId);
	}

	@Override
	public PublicKey[] getPublicKeys(String keyId) throws Exception {
		Key key = keys.get(keyId);
		if(key==null){
			throw new UnrecoverableKeyException("No key found for "+keyId);
		}
		if(key instanceof PublicKey){
			return new PublicKey[] {(PublicKey) key};
		}
		throw new UnrecoverableKeyException("Expected Public key, found "+key.getClass().getName());
	}

	@Override
	public PrivateKey[] getPrivateKeys(String keyId) throws Exception {
		Key key = keys.get(keyId);
		if(key==null){
			throw new UnrecoverableKeyException("No key found for "+keyId);
		}
		if(key instanceof PrivateKey){
			return new PrivateKey[] {(PrivateKey) key};
		}
		throw new UnrecoverableKeyException("Expected Private key, found "+key.getClass().getName());
	}

	@Override
	public SecretKey[] getSecretKeys(String keyId) throws Exception {
		Key key = keys.get(keyId);
		if(key==null){
			throw new UnrecoverableKeyException("No key found for "+keyId);
		}
		if(key instanceof SecretKey){
			return new SecretKey[] {(SecretKey) key};
		}
		throw new UnrecoverableKeyException("Expected Secret key, found "+key.getClass().getName());
	}

}

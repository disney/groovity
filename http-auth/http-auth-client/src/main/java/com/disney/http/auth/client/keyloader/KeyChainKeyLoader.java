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

import com.disney.http.auth.keychain.KeyChain;

import java.security.Key;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.Callable;

import javax.crypto.SecretKey;

/**
 * Class for converting KeyStoreLoaders into Key Loaders
 *
 * @author Rachel Kobayashi
 */
public class KeyChainKeyLoader implements Callable<Key> {
    private KeyChain keyChain;
    private String alias;
    
    public KeyChainKeyLoader(KeyChain keyChain){
    	this.keyChain=keyChain;
    }

    public KeyChainKeyLoader(KeyChain keyChain, String alias){
        this.keyChain=keyChain;
        this.alias=alias;
    }

    @Override
    public Key call() throws Exception{
        if(keyChain.containsKey(alias)){
        	try{
	        	SecretKey[] sks = keyChain.getSecretKeys(alias);
	        	if(sks!=null && sks.length>0){
	        		return sks[0];
	        	}
        	}
        	catch(UnrecoverableKeyException e){
        		
        	}
        	PrivateKey[] pks = keyChain.getPrivateKeys(alias);
        	if(pks!=null && pks.length>0){
        		return pks[0];
        	}
        }
        throw new UnrecoverableKeyException("No key for alias: "+alias);
    }

    public void setAlias(String alias){ this.alias = alias; }

	public String getAlias() {
		return alias;
	}
 
}

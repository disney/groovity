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
package com.disney.groovity.tags;

import java.net.URI;
import java.security.Key;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.http.auth.Algorithms;
import com.disney.http.auth.client.keyloader.KeyChainKeyLoader;
import com.disney.http.auth.client.keyloader.KeyObjectKeyLoader;
import com.disney.http.auth.client.signer.HttpSignatureSigner;
import com.disney.http.auth.keychain.KeyStoreKeyChainImpl;
import com.disney.uriparcel.URIParcel;

import groovy.lang.Closure;
/**
 * Sign an HTTP request
 * <p>
 * signature( <ul>	
 *	<li><b>keyId</b>: 
 *	the keyId to be used in the signature and sent in the request,</li>	
 *	<li><i>key</i>: 
 *	a base64 encoded secret or java.security.Key object to use in signing, required unless keystore is used,</li>	
 *	<li><i>keystore</i>: 
 *	a Keystore object or URI of a keystore to load the signing key from, required unless 'key' is specified,</li>	
 *	<li><i>password</i>: 
 *	a password to decrypt the keystore and its entries, required if a keystore is used,</li>	
 *	<li><i>alias</i>: 
 *	the alias of the key entry to use for signing, required if a keystore is used,</li>	
 *	<li><i>type</i>: 
 *	specifies the type of keystore to load from a URI, defaults to PKCS12,</li>	
 *	<li><i>algorithm</i>: 
 *	hmac-sha256, hmac-sha1, hmac-md5 for symmetric keys; rsa-sha256, rsa-sha1, rsa-md5 for rsa keypair; defaults to sha256 with either hmac or rsa depending on the key,</li>	
 *	<li><i>headers</i>: 
 *	which headers to sign, defaults to ['Date'],</li>
 *	</ul>{});
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;~ &lt;g:http url=&quot;http://my.service/endpoint&quot;&gt;
 *	&lt;g:signature key=&quot;${secret}&quot; keyId=&quot;${keyId}&quot; /&gt;&lt;/g:http&gt; ~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
	info = "Sign an HTTP request",
	sample="<~ <g:http url=\"http://my.service/endpoint\">\n<g:signature key=\"${secret}\" keyId=\"${keyId}\" /></g:http> ~>",
	attrs = {
		@Attr(
			name = "keyId", 
			info="the keyId to be used in the signature and sent in the request",
			required = true
		),
		@Attr(
			name = "key", 
			info="a base64 encoded secret or java.security.Key object to use in signing, required unless keystore is used",
			required = false
		),
		@Attr(
			name = "keystore", 
			info="a Keystore object or URI of a keystore to load the signing key from, required unless 'key' is specified",
			required = false
		),
		@Attr(
			name = "password", 
			info="a password to decrypt the keystore and its entries, required if a keystore is used",
			required = false
		),
		@Attr(
			name = "alias", 
			info="the alias of the key entry to use for signing, required if a keystore is used",
			required = false
		),
		@Attr(
			name = "type", 
			info="specifies the type of keystore to load from a URI, defaults to PKCS12",
			required = false
		),
		@Attr(
			name = "algorithm", 
			info="hmac-sha256, hmac-sha1, hmac-md5 for symmetric keys; rsa-sha256, rsa-sha1, rsa-md5 for rsa keypair; defaults to sha256 with either hmac or rsa depending on the key",
			required = false
		),
		@Attr(
			name = "headers", 
			info="which headers to sign, defaults to ['Date']",
			required = false
		)
	} 
)
public class Signature implements Taggable {
	final static String SIGNATURE_BINDING = INTERNAL_BINDING_PREFIX+"Optional.Signature";
	ConcurrentHashMap<String, KeyChainKeyLoader> keystores = new ConcurrentHashMap<String, KeyChainKeyLoader>();

	@SuppressWarnings({"rawtypes","unchecked"})
	public static void acceptSigner(Map variables){
		variables.put(SIGNATURE_BINDING, Optional.empty());
	}

	@SuppressWarnings({"rawtypes","unchecked"})
	public static Optional<HttpSignatureSigner> resolveSigner(Map variables){
		return (Optional<HttpSignatureSigner>)variables.remove(SIGNATURE_BINDING);
	}

	@SuppressWarnings({"rawtypes","unchecked"})
	public Object tag(Map attributes, Closure body) throws Exception {
		Object keyId = resolve(attributes,"keyId");
		if(keyId==null){
			throw new RuntimeException("<g:signature> requires a keyId for signing");
		}
		Callable<Key> useLoader = null;
		Object key = resolve(attributes,"key");
		if(key==null){
			Object keystore = resolve(attributes,"keystore");
			if(keystore==null){
				throw new RuntimeException("<g:signature> requires a key or keystore for signing");
			}
			String password = resolve(attributes, "password", String.class);
			if(password==null){
				throw new RuntimeException("<g:signature> requires a password when using a keystore");
			}
			String alias = resolve(attributes, "alias", String.class);
			if(alias==null){
				throw new RuntimeException("<g:signature> requires an alias when using a keystore");
			}
			if(!(keystore instanceof KeyStore)){
				String ksl = keystore.toString();
				KeyChainKeyLoader loader = keystores.get(ksl);
				if(loader==null){
					URIParcel<KeyStore> keystoreParcel = new URIParcel<KeyStore>(KeyStore.class,new URI(ksl));
					keystoreParcel.setRefresh(60000);
					Map conf = new HashMap();
					conf.put("password", password);
					String type = resolve(attributes, "type", String.class);
					if(type!=null){
						conf.put("type", type);
					}
					keystoreParcel.setConfig(conf);
					loader = new KeyChainKeyLoader(new KeyStoreKeyChainImpl(keystoreParcel, password.toCharArray()), alias);
					keystores.put(ksl, loader);
				}
				useLoader = loader;
			}
			else{
				useLoader = new KeyChainKeyLoader(new KeyStoreKeyChainImpl((KeyStore)keystore, password.toCharArray()), alias);
			}
		}
		if(key instanceof Callable<?>){
			useLoader = (Callable<Key>) key;
		}
		else if(key instanceof Key){
			useLoader = new KeyObjectKeyLoader((Key)key);
		}
		String useAlgorithm = "hmac-sha256";
		Object algorithm = resolve(attributes,"algorithm");
		if(algorithm!=null){
			useAlgorithm=algorithm.toString();
		}
		if(useLoader==null){
			if(useAlgorithm.startsWith("rsa")){
				//TODO load private key from object
			}
			else{
				String signingAlg = Algorithms.getSecurityAlgorithm(useAlgorithm);
				//System.out.println("Generating hmac key "+signingAlg+" with "+new String(DatatypeConverter.parseBase64Binary(key.toString())));
				useLoader = new KeyObjectKeyLoader(new SecretKeySpec(DatatypeConverter.parseBase64Binary(key.toString()), signingAlg));
			}
		}
		Object headers = resolve(attributes,"headers");
		HttpSignatureSigner signer = new HttpSignatureSigner();
		signer.setAlgorithm(useAlgorithm);
		signer.setKeyId(keyId.toString());
		signer.setKeyLoader(useLoader);
		if(headers!=null){
			if(!(headers instanceof List)){
				throw new RuntimeException("signature tag requires that 'headers' attribut contains a List, instead found "+headers.getClass().toString());
			}
			signer.setHeaders((List)headers);
		}
		bind(body, SIGNATURE_BINDING, Optional.of(signer));
		return null;
	}

}

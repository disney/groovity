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
package com.disney.groovity.servlet.auth;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import com.disney.groovity.servlet.GroovityScriptViewFactory;
import com.disney.http.auth.Algorithms;
import com.disney.http.auth.AuthorizationRequest;
import com.disney.http.auth.keychain.KeyChain;
import com.disney.http.auth.keychain.KeyStoreKeyChainImpl;
import com.disney.http.auth.keychain.MapKeyChainImpl;
import com.disney.http.auth.server.ACLAccessControllerImpl;
import com.disney.http.auth.server.AbstractVerifier;
import com.disney.http.auth.server.AccessController;
import com.disney.http.auth.server.ServerAuthorizationRequest;
import com.disney.http.auth.server.Verifier;
import com.disney.http.auth.server.VerifierChain;
import com.disney.http.auth.server.VerifierResult;
import com.disney.http.auth.server.basic.BasicVerifierImpl;
import com.disney.http.auth.server.basic.MapPasswordChecker;
import com.disney.http.auth.server.basic.PasswordChecker;
import com.disney.http.auth.server.digest.DigestVerifierImpl;
import com.disney.http.auth.server.digest.MapPasswordDigester;
import com.disney.http.auth.server.digest.PasswordDigester;
import com.disney.http.auth.server.policy.PolicyVerifierImpl;
import com.disney.http.auth.server.signature.SignatureVerifierImpl;
import com.disney.uriparcel.URIParcel;
import com.disney.uriparcel.value.KeyStoreValueHandler;

import groovy.lang.Closure;
import groovy.lang.Script;

/**
 * VerifierFactory is responsible for parsing declarative static "auth" blocks from groovity scripts and turning them into 
 * VerifierChains for basic, digest and/or http signature authentication.  See the groovity wiki for detailed documentation
 * of the auth configuration options.
 * 
 * @author Alex Vigdor
 *
 */
public class VerifierFactory {
	private GroovityScriptViewFactory viewResolver;
	
	@SuppressWarnings("rawtypes")
	public Verifier createVerifier(List auths, Class<Script> scriptClass) throws InstantiationException, IllegalAccessException, ClassNotFoundException, MalformedURLException, URISyntaxException, NoSuchAlgorithmException, InvalidKeySpecException, CertificateException{
		ArrayList<Verifier> verifiers = new ArrayList<Verifier>(auths.size());
		for(Object auth:auths){
			if(auth instanceof Map){
				Map conf = (Map) auth;
				Object policy = conf.get("policy");
				if(policy!=null){
					verifiers.add(processPolicy(conf,scriptClass));
				}
				else{
					String type = (String) conf.get("type");
					if("signature".equals(type)){
						verifiers.add(processSignature(conf,scriptClass));
					}
					else if("basic".equals(type)){
						verifiers.add(processBasic(conf, scriptClass));
					}
					else if("digest".equals(type)){
						verifiers.add(processDigest(conf,scriptClass));
					}
					else{
						throw new IllegalArgumentException("Unkown auth type: "+type);
					}
				}
			}
			else if(auth instanceof CharSequence){
				verifiers.add((Verifier)fallbackConstruct(auth, scriptClass));
			}
			else if(auth instanceof Closure){
				verifiers.add(new Verifier() {
					@Override
					public VerifierResult verify(ServerAuthorizationRequest request) throws Exception {
						Object result = ((Closure)auth).call(request);
						if(!(result instanceof VerifierResult)){
							result = DefaultTypeTransformation.castToType(result, VerifierResult.class);
						}
						return (VerifierResult) result;
					}
				});
			}
		}
		return new VerifierChain(verifiers);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> T resolve(Map map, String name, Class<T> type){
		Object o = map.get(name);
		if(o==null){
			return null;
		}
		if(o instanceof Closure){
			o = ((Closure)o).call();
			if(o==null){
				return null;
			}
		}
		if(String.class.equals(type)){
			return (T) o.toString();
		}
		return type.cast(o);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void processCommon(AbstractVerifier verifier, Map map, Class scriptClass) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		List<AccessController> accessControllers = new ArrayList<AccessController>();
		String realm = resolve(map,"realm",String.class);
		if(realm!=null){
			verifier.setRealm(realm);
		}
		List acl = resolve(map,"acl",List.class);
		if(acl!=null){
			accessControllers.add(new ACLAccessControllerImpl(acl));
		}
		Object ac = resolve(map,"accessController",Object.class);
		if(ac!=null){
			addAccessController(ac, accessControllers, scriptClass);
		}
		List<Object> acs = resolve(map,"accessControllers",List.class);
		if(acs!=null){
			for(Object c: acs){
				addAccessController(c, accessControllers, scriptClass);
			}
		}
		verifier.setAccessControllers(accessControllers);
	}

	private void addKeychain(Object keychain, List<KeyChain> keychains, Class<Script> scriptClass) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		if (keychain instanceof CharSequence) {
			keychains.add((KeyChain) fallbackConstruct(keychain, scriptClass));
		} else if (keychain instanceof Closure) {
			keychains.add(new KeyChain() {

				@SuppressWarnings("unchecked")
				private List<Key> getKeys(String keyId) {
					@SuppressWarnings("rawtypes")
					Object ks = ((Closure) keychain).call(keyId);
					if (ks == null) {
						return null;
					}
					if (ks instanceof Key) {
						return Arrays.asList((Key) ks);
					}
					if (ks instanceof List) {
						return (List<Key>) ks;
					}
					throw new IllegalArgumentException("Cannot convert " + ks + " to a list of keys");
				}

				@Override
				public SecretKey[] getSecretKeys(String keyId) throws Exception {
					List<Key> allKeys = getKeys(keyId);
					if (allKeys != null) {
						return allKeys.stream().filter(key -> key instanceof SecretKey).collect(Collectors.toList())
								.toArray(new SecretKey[0]);
					}
					return null;
				}

				@Override
				public PublicKey[] getPublicKeys(String keyId) throws Exception {
					List<Key> allKeys = getKeys(keyId);
					if (allKeys != null) {
						return allKeys.stream().filter(key -> key instanceof PublicKey).collect(Collectors.toList())
								.toArray(new PublicKey[0]);
					}
					return null;
				}

				@Override
				public PrivateKey[] getPrivateKeys(String keyId) throws Exception {
					List<Key> allKeys = getKeys(keyId);
					if (allKeys != null) {
						return allKeys.stream().filter(key -> key instanceof PrivateKey).collect(Collectors.toList())
								.toArray(new PrivateKey[0]);
					}
					return null;
				}

				@Override
				public boolean containsKey(String keyId) throws Exception {
					List<Key> allKeys = getKeys(keyId);
					return (allKeys != null) && !allKeys.isEmpty();
				}
			});
		}
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private SignatureVerifierImpl processSignature(Map signature, Class<Script> scriptClass) throws InstantiationException, IllegalAccessException, ClassNotFoundException, MalformedURLException, URISyntaxException, NoSuchAlgorithmException, InvalidKeySpecException, CertificateException{
		SignatureVerifierImpl verifier = new SignatureVerifierImpl();
		processCommon(verifier, signature, scriptClass);
		List<KeyChain> keyChains = new ArrayList<KeyChain>();
		List headers = (List) signature.get("headers");
		if(headers!=null){
			verifier.setRequiredHeaders(headers);
		}
		Number drift = (Number) signature.get("drift");
		if(drift!=null){
			verifier.setMaxDateDrift(drift.longValue());
		}
		Map<Object,Map> keys = (Map) signature.get("keys");
		if(keys!=null){
			//we need to convert to proper Key objects
			Map<String,Key> realKeys = new HashMap<String, Key>();
			for(Entry<Object,Map> entry: keys.entrySet()){
				String algorithm = (String) entry.getValue().get("algorithm");
				Object secret = entry.getValue().get("key");
				String signingAlg = Algorithms.getSecurityAlgorithm(algorithm);
				Key key;
				if(signingAlg.startsWith("Hmac")){
					//expect base 64 encoding
					key = new SecretKeySpec(DatatypeConverter.parseBase64Binary(secret.toString()), signingAlg);
				}
				else{
					//expect x509 encoding
					CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
					Certificate certificate = certificateFactory.generateCertificate(new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(secret.toString())));
					key = certificate.getPublicKey();
					/*X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(DatatypeConverter.parseBase64Binary(secret.toString()));
					KeyFactory factory = KeyFactory.getInstance("rsa");
					key = factory.generatePublic(pubKeySpec);
					*/
				}
				realKeys.put(entry.getKey().toString(), key);
			}
			keyChains.add(new MapKeyChainImpl(realKeys));
		}
		Map keystore = (Map) signature.get("keystore");
		if(keystore!=null){
			keyChains.add(makeKeyStoreLoader(keystore));
		}
		List<Map> keystores = (List<Map>) signature.get("keystores");
		if(keystores!=null){
			for(Map k: keystores){
				keyChains.add(makeKeyStoreLoader(k));
			}
		}
		Object keychain = signature.get("keychain");
		addKeychain(keychain, keyChains, scriptClass);
		List kcs = (List) signature.get("keychains");
		if(kcs!=null){
			for(Object kc: kcs){
				addKeychain(kc, keyChains,scriptClass);
			}
		}
		verifier.setKeyChains(keyChains);
		return verifier;
	}
	@SuppressWarnings("rawtypes")
	private KeyChain makeKeyStoreLoader(final Map conf) throws MalformedURLException, URISyntaxException{
		Callable<URI> uri;
		final Object loc = conf.get("location");
		if(loc instanceof Closure){
			uri = new Callable<URI>() {

				public URI call() throws Exception {
					String location = ((Closure)loc).call().toString();
					if(location.startsWith("/")){
						//look for webapp resource
						URL url = viewResolver.getServletContext().getResource(location);
						if(url!=null){
							return url.toURI();
						}
					}
					return new URI(location);
				}
			};
		}
		else{
			final URI mUri = new URI(loc.toString());
			uri = new Callable<URI>() {
				public URI call() throws Exception {
					return mUri;
				}
			};
		}
		Callable<Long> refresh;
		final Object ttl = (Object) conf.get("ttl");
		if(ttl!=null){
			if(ttl instanceof Closure){
				refresh = new Callable<Long>() {
					public Long call() throws Exception {
						return ((Number)(((Closure)ttl).call())).longValue();
					}
				};
			}
			else{
				refresh = new Callable<Long>() {
					public Long call() throws Exception {
						return ((Number)ttl).longValue();
					}
				};
			}
		}
		else{
			refresh = new Callable<Long>() {
				public Long call() throws Exception {
					return 120000l;
				}
			};
		}
		Callable<Map<String,Object>> confg = new Callable<Map<String,Object>>() {

			public Map<String,Object> call() throws Exception {
				String password=resolve(conf,"password",String.class);
				String tp = resolve(conf,"type",String.class);
				String type= tp!=null?(String) tp.toString():"PKCS12";
				Map<String,Object> config = new HashMap<>();
				config.put(KeyStoreValueHandler.KEYSTORE_PASSWORD,password);
				if(type!=null){
					config.put(KeyStoreValueHandler.KEYSTORE_TYPE,type);
				}
				return config;
			}
		};
	
		Callable<char[]> passwd = new Callable<char[]>() {

			public char[] call() throws Exception {
				String password=resolve(conf,"password",String.class);
				return password.toCharArray();
			}
		};
		
		URIParcel<KeyStore> parcel = new URIParcel<KeyStore>(KeyStore.class,uri,refresh, confg);
		return new KeyStoreKeyChainImpl(parcel, passwd);
	}
	
	private PolicyVerifierImpl processPolicy(@SuppressWarnings("rawtypes") final Map policy, Class<Script> scriptClass) throws MalformedURLException, URISyntaxException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		PolicyVerifierImpl verifier = new PolicyVerifierImpl();
		processCommon(verifier, policy, scriptClass);
		verifier.setPolicyLoader(new Callable<Verifier>() {
			String curPolicy = null;
			Callable<Verifier> curLoader = null;

			public Verifier call() throws Exception {
				String loc = resolve(policy,"policy",String.class);
				if(!loc.equals(curPolicy)){
					//location has changed, update loaded policy
					URI policyURI = null;
					if(loc.startsWith("/")){
						URL url = viewResolver.getServletContext().getResource(loc);
						if(url!=null){
							policyURI = url.toURI();
						}
						else{
							ViewPolicyLoader vpl = new ViewPolicyLoader();
							vpl.setLocation(loc);
							vpl.setViewResolver(viewResolver);
							curLoader = vpl;
						}
					}
					else{
						policyURI = new URI(loc);
					}
					if(policyURI!=null){
						Number ttl = (Number) policy.get("ttl");
						// set right loader based on location: view, file, HTTP url or servlet url ...
						URIParcel<Verifier> parcel = new URIParcel<Verifier>(Verifier.class,policyURI);
						if(ttl!=null){
							parcel.setRefresh(ttl.longValue());
						}
						curLoader = parcel;
					}
					curPolicy=loc;
				}
				return curLoader.call();
			}
		});
		
		return verifier;
	}
	
	private void addPasswordChecker(Object passwordChecker, List<PasswordChecker> passwordCheckers, Class<Script> scriptClass) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		if(passwordChecker instanceof CharSequence){
			passwordCheckers.add((PasswordChecker)fallbackConstruct(passwordChecker, scriptClass));
		}
		else if(passwordChecker instanceof Closure){
			passwordCheckers.add(new PasswordChecker() {
				@SuppressWarnings("rawtypes")
				@Override
				public boolean check(String username, String password) {
					return (boolean) ((Closure)passwordChecker).call(username,password);
				}
			});
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private BasicVerifierImpl processBasic(Map basic, Class scriptClass) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		BasicVerifierImpl verifier = new BasicVerifierImpl();
		processCommon(verifier, basic, scriptClass);
		List<PasswordChecker> passwordCheckers = new ArrayList<PasswordChecker>();
		Map passwords = (Map) basic.get("passwords");
		if(passwords!=null){
			passwordCheckers.add(new MapPasswordChecker(passwords));
		}
		Object passwordChecker = basic.get("passwordChecker");
		addPasswordChecker(passwordChecker, passwordCheckers, scriptClass);
		List pcs = (List) basic.get("passwordCheckers");
		if(pcs!=null){
			for(Object pc: pcs){
				addPasswordChecker(pc, passwordCheckers, scriptClass);
			}
		}
		verifier.setPasswordCheckers(passwordCheckers);
		return verifier;
	}
	
	private Object fallbackConstruct(Object className, Class<Script> scriptClass) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		try{
			return Class.forName(className.toString()).newInstance();
		}
		catch(ClassNotFoundException ce){
			 return Class.forName(className.toString(),true,scriptClass.getClassLoader()).newInstance();
		}
	}
	
	@SuppressWarnings("rawtypes")
	private void addDigester(Object passwordDigester, List<PasswordDigester> passwordDigesters, Class<Script> scriptClass) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		if(passwordDigester instanceof CharSequence){
			passwordDigesters.add((PasswordDigester) fallbackConstruct(passwordDigester, scriptClass));
		}
		else if(passwordDigester instanceof Closure){
			passwordDigesters.add(new PasswordDigester() {
				@Override
				public byte[] digest(String username, String realm)
						throws NoSuchAlgorithmException, UnsupportedEncodingException {
					return (byte[]) ((Closure)passwordDigester).call(username,realm);
				}
			});
		}
	}
	
	@SuppressWarnings("rawtypes")
	private void addAccessController(Object accessController, List<AccessController> accessControllers, Class<Script> scriptClass) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		if(accessController instanceof CharSequence){
			accessControllers.add((AccessController)fallbackConstruct(accessController, scriptClass));
		}
		else if(accessController instanceof Closure){
			accessControllers.add(new AccessController() {
				@Override
				public boolean allow(Principal principal, AuthorizationRequest request) {
					return DefaultTypeTransformation.castToBoolean(((Closure)accessController).call(principal,request));
				}
			});
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private DigestVerifierImpl processDigest(Map digest, Class<Script> scriptClass) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		DigestVerifierImpl verifier = new DigestVerifierImpl();
		processCommon(verifier, digest, scriptClass);
		ArrayList<PasswordDigester> passwordDigesters= new ArrayList<PasswordDigester>();
		String nonceSecret = (String) digest.get("nonceSecret");
		if(nonceSecret!=null){
			verifier.setNonceSecret(nonceSecret);
		}
		String domain = (String) digest.get("domain");
		if(domain!=null){
			verifier.setDomain(domain);
		}
		Number maxNonceAge = (Number) digest.get("maxNonceAge");
		if(maxNonceAge!=null){
			verifier.setMaxNonceAge(maxNonceAge.longValue());
		}
		Map passwords = (Map) digest.get("passwords");
		if(passwords!=null){
			passwordDigesters.add(new MapPasswordDigester(passwords));
		}
		Object passwordDigester = digest.get("passwordDigester");
		addDigester(passwordDigester, passwordDigesters,scriptClass);
		List pds = (List) digest.get("passwordDigesters");
		if(pds!=null){
			for(Object pd: pds){
				addDigester(pd, passwordDigesters,scriptClass);
			}
		}
		verifier.setPasswordDigesters(passwordDigesters);
		return verifier;
	}

	public GroovityScriptViewFactory getViewResolver() {
		return viewResolver;
	}

	public void setViewResolver(GroovityScriptViewFactory viewResolver) {
		this.viewResolver = viewResolver;
	}
}

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
package com.disney.http.auth.server.policy;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletContext;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.disney.http.auth.Algorithms;
import com.disney.http.auth.server.ACLAccessControllerImpl;
import com.disney.http.auth.server.AbstractVerifier;
import com.disney.http.auth.server.AccessController;
import com.disney.http.auth.server.Verifier;
import com.disney.http.auth.server.VerifierChain;
import com.disney.http.auth.server.basic.BasicVerifierImpl;
import com.disney.http.auth.server.basic.MapPasswordChecker;
import com.disney.http.auth.server.basic.PasswordChecker;
import com.disney.http.auth.server.digest.DigestVerifierImpl;
import com.disney.http.auth.server.digest.MapPasswordDigester;
import com.disney.http.auth.server.digest.PasswordDigester;
import com.disney.http.auth.server.signature.SignatureVerifierImpl;
import com.disney.uriparcel.URIParcel;
import com.disney.uriparcel.value.KeyStoreValueHandler;
import com.disney.http.auth.keychain.KeyChain;
import com.disney.http.auth.keychain.KeyStoreKeyChainImpl;
import com.disney.http.auth.keychain.MapKeyChainImpl;
/**
 * A parser that takes an XML verifier configuration and instantiates the appropriate verifier components for runtime authorization.
 * See META-INF/auth.dtd for the schema
 *
 * @author Alex Vigdor
 */
public class XmlPolicyParser {
	public static Verifier parsePolicy(InputSource source, ServletContext servletContext) throws SAXException, ParserConfigurationException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(true);
		factory.setNamespaceAware(false);
		factory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		builder.setEntityResolver(new EntityResolver() {
			@Override
			public InputSource resolveEntity(String publicId, String systemId)
					throws SAXException, IOException {
				//System.out.println("Getting entity for "+systemId);
				if(systemId.endsWith("auth.dtd")){
					return new InputSource(XmlPolicyParser.class.getResourceAsStream("/auth.dtd"));
				}
				return null;
			}
		});
		Document doc = builder.parse(source);
		List<Verifier> configs = new ArrayList<Verifier>();
		NodeList cnodes = doc.getDocumentElement().getChildNodes();
		for(int i=0;i<cnodes.getLength();i++){
			Node cnode = cnodes.item(i);
			if(cnode instanceof Element){
				Element cel = (Element)cnode;
				if(cel.getNodeName().equals("basic")){
					configs.add(processBasic(cel));
				}
				else if(cel.getNodeName().equals("signature")){
					configs.add(processSignature(cel,servletContext));
				}
				else if(cel.getNodeName().equals("digest")){
					configs.add(processDigest(cel));
				}
			}
		}
		if(configs.size()==0){
			return null;
		}
		if(configs.size()==1){
			return configs.get(0);
		}
		return new VerifierChain(configs);
	}
	
	private static void processCommon(AbstractVerifier config, Element verifier) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		List<AccessController> accessControllers = new ArrayList<AccessController>();
		NodeList bcnodes = verifier.getChildNodes();
		for(int j=0;j<bcnodes.getLength();j++){
			Node bcnode = bcnodes.item(j);
			if(bcnode instanceof Element){
				Element bcel = (Element) bcnode;
				if(bcel.getNodeName().equals("realm")){
					config.setRealm(bcel.getTextContent().trim());
				}
				else if(bcel.getNodeName().equals("acl")){
					accessControllers.add(processAcl(bcel));
				}
				else if(bcel.getNodeName().equals("accessController")){
					accessControllers.add((AccessController) Class.forName(bcel.getAttribute("class")).newInstance());
				}
			}
		}
		config.setAccessControllers(accessControllers);
	}
	
	private static SignatureVerifierImpl processSignature(Element sig, ServletContext context) throws InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, MalformedURLException, URISyntaxException{
		SignatureVerifierImpl config = new SignatureVerifierImpl();
		List<KeyChain> keyChains = new ArrayList<KeyChain>();
		processCommon(config, sig);
		NodeList bcnodes = sig.getChildNodes();
		for(int j=0;j<bcnodes.getLength();j++){
			Node bcnode = bcnodes.item(j);
			if(bcnode instanceof Element){
				Element bcel = (Element) bcnode;
				if(bcel.getNodeName().equals("drift")){
					config.setMaxDateDrift(Long.parseLong(bcel.getTextContent().trim()));
				}
				else if(bcel.getNodeName().equals("headers")){
					config.setRequiredHeaders(Arrays.asList(bcel.getTextContent().trim().split("(,\\s*|\\s+)")));
				}
				else if(bcel.getNodeName().equals("keys")){
					keyChains.add(new MapKeyChainImpl(processKeys(bcel)));
				}
				else if(bcel.getNodeName().equals("keystore")){
					keyChains.add(processKeystore(bcel,context));
				}
			}
		}
		config.setKeyChains(keyChains);
		return config;
	}
	
	private static KeyChain processKeystore(Element keystore, ServletContext context) throws MalformedURLException, URISyntaxException{
		String location = null;
		long ttl = -1;
		String password=null;
		String type="JCEKS";
		NodeList kids = keystore.getChildNodes();
		for(int i=0;i<kids.getLength();i++){
			Node n = kids.item(i);
			if("location".equals(n.getNodeName())){
				location = n.getTextContent().trim();
			}
			else if("ttl".equals(n.getNodeName())){
				ttl = Long.parseLong(n.getTextContent().trim());
			}
			else if("type".equals(n.getNodeName())){
				type = n.getTextContent().trim();
			}
			else if("password".equals(n.getNodeName())){
				password = n.getTextContent().trim();
			}
		}
		URL ku = location.contains(":") ? new URL(location) : context.getResource(location);
		Map<String,Object> config = new HashMap<String,Object>();
    	config.put(KeyStoreValueHandler.KEYSTORE_PASSWORD, password);
    	config.put(KeyStoreValueHandler.KEYSTORE_TYPE, type);
    	URIParcel<KeyStore> parcel = new URIParcel<KeyStore>(KeyStore.class,ku.toURI(),ttl,config);
    	 
		return new KeyStoreKeyChainImpl(parcel,password.toCharArray());
	}
	
	private static Map<String,Key> processKeys(Element keys) throws NoSuchAlgorithmException, InvalidKeySpecException{
		Map<String,Key> keyMap = new HashMap<String, Key>();
		NodeList kids = keys.getChildNodes();
		for(int i=0;i<kids.getLength();i+=2){
			String keyId = kids.item(i).getTextContent().trim();
			String keyVal = kids.item(i+1).getTextContent().trim();
			NamedNodeMap keyAtts = kids.item(i+1).getAttributes();
			String keyAlg = "hmac-sha256";
			if(keyAtts!=null){
				Node algn = keyAtts.getNamedItem("algorithm");
				if(algn!=null){
					keyAlg = algn.getNodeValue();
				}				
			}
			Key key;
			String signingAlg = Algorithms.getSecurityAlgorithm(keyAlg);
			if(signingAlg.startsWith("Hmac")){
				//expect base 64 encoding
				key = new SecretKeySpec(DatatypeConverter.parseBase64Binary(keyVal), signingAlg);
			}
			else{
				//expect x509 encoding
				X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(DatatypeConverter.parseBase64Binary(keyVal));
				KeyFactory factory = KeyFactory.getInstance("rsa");
				key = factory.generatePublic(pubKeySpec);
			}
			keyMap.put(keyId, key);
		}
		return keyMap;
	}
	
	private static DigestVerifierImpl processDigest(Element digest) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		DigestVerifierImpl config = new DigestVerifierImpl();
		processCommon(config, digest);
		ArrayList<PasswordDigester> passwordDigesters= new ArrayList<PasswordDigester>();
		NodeList bcnodes = digest.getChildNodes();
		for(int j=0;j<bcnodes.getLength();j++){
			Node bcnode = bcnodes.item(j);
			if(bcnode instanceof Element){
				Element bcel = (Element) bcnode;
				if(bcel.getNodeName().equals("passwords")){
					passwordDigesters.add(new MapPasswordDigester(processPasswords(bcel)));
				}
				else if(bcel.getNodeName().equals("passwordDigester")){
					passwordDigesters.add((PasswordDigester) Class.forName(bcel.getAttribute("class")).newInstance());
				}
				else if(bcel.getNodeName().equals("maxNonceAge")){
					config.setMaxNonceAge(Long.valueOf(bcel.getTextContent()));
				}
				else if(bcel.getNodeName().equals("nonceSecret")){
					config.setNonceSecret(bcel.getTextContent().trim());
				}
				else if(bcel.getNodeName().equals("domain")){
					config.setDomain(bcel.getTextContent().trim());
				}
			}
		}
		config.setPasswordDigesters(passwordDigesters);
		return config;
	}
	
	private static BasicVerifierImpl processBasic(Element basic) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		BasicVerifierImpl bc = new BasicVerifierImpl();
		processCommon(bc, basic);
		List<PasswordChecker> passwordCheckers = new ArrayList<PasswordChecker>();
		NodeList bcnodes = basic.getChildNodes();
		for(int j=0;j<bcnodes.getLength();j++){
			Node bcnode = bcnodes.item(j);
			if(bcnode instanceof Element){
				Element bcel = (Element) bcnode;
				if(bcel.getNodeName().equals("passwords")){
					passwordCheckers.add(new MapPasswordChecker(processPasswords(bcel)));
				}
				else if(bcel.getNodeName().equals("passwordChecker")){
					passwordCheckers.add((PasswordChecker) Class.forName(bcel.getAttribute("class")).newInstance());
				}
			}
		}
		bc.setPasswordCheckers(passwordCheckers);
		return bc;
	}
	
	private static AccessController processAcl(Element acl){
		ArrayList<String> keyIds = new ArrayList<String>();
		NodeList kids = acl.getChildNodes();
		for(int i=0;i<kids.getLength();i++){
			Node n = kids.item(i);
			if(n instanceof Element){
				Element k = (Element) n;
				if(k.getNodeName().equals("keyId")){
					keyIds.add(k.getTextContent().trim());
				}
			}
		}
		return new ACLAccessControllerImpl(keyIds);
	}
	
	private static Map<String,String> processPasswords(Element passwords){
		Map<String,String> passwordMap = new ConcurrentHashMap<String, String>();
		NodeList kids = passwords.getChildNodes();
		for(int i=0;i<kids.getLength();i+=2){
			passwordMap.put(kids.item(i).getTextContent().trim(), kids.item(i+1).getTextContent().trim());
		}
		return passwordMap;
	}
}

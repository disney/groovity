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
package com.disney.uriparcel.keys;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMEncryptor;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.bouncycastle.util.io.pem.PemWriter;

import com.disney.uriparcel.value.AbstractValueHandler;
/**
 * ValueHandler for reading and writing PKCS8 private keypair files.  IF a password is provided in the config map,
 * the PKCS8 file is treated as encrypted using the algorithm AES-128-CFB.  Otherwise an unencrypted PKSC8 file is assumed.
 *
 * @author Alex Vigdor
 */
public class KeyPairValueHandler extends AbstractValueHandler {
	public static final String PASSWORD = "password";
	public static final String PEM_ENCRYPTION_ALGORITHM = "AES-128-CFB";
	KeyFactory factory;
	
	static{
		Security.addProvider(new BouncyCastleProvider());
	}
	
	public KeyPairValueHandler() throws NoSuchAlgorithmException{
		factory = KeyFactory.getInstance("RSA");
	}
	
	public int getPriority() {
		return 88;
	}

	@Override
	protected Object doLoad(InputStream stream, String contentType, @SuppressWarnings("rawtypes") Class valueClass, @SuppressWarnings("rawtypes") Map config) throws Exception {
		//look here http://stackoverflow.com/questions/15656644/get-keypair-from-pem-key-with-bouncycastle
		PEMKeyPair keyPair;
		Reader reader = new InputStreamReader(stream, getCharset(contentType));
		PEMParser pemReader = new PEMParser(reader);
		try {
			Object o = pemReader.readObject();
			if(o==null){
				return null;
			}
			
			if(o instanceof PEMEncryptedKeyPair){
				if(config!=null && config.containsKey(PASSWORD)){
					String password = config.get(PASSWORD).toString();
					 PEMDecryptorProvider decryptionProv = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
					 keyPair = ((PEMEncryptedKeyPair) o).decryptKeyPair(decryptionProv);
				}
				else{
					throw new RuntimeException("Keypair requires password but none provided");
				}
			}
			else{
				keyPair = ((PEMKeyPair) o);
			}
		}
		finally {
			pemReader.close();
		}
        return new JcaPEMKeyConverter().getKeyPair(keyPair);
	}

	@Override
	protected void doStore(OutputStream stream, String contentType, Object value, @SuppressWarnings("rawtypes") Map config) throws Exception {
		Writer writer = new OutputStreamWriter(stream,getCharset(contentType));
		PemWriter pemWriter = new PemWriter(writer);
		
		if(config!=null && config.containsKey(PASSWORD)){
			 PEMEncryptor penc = (new JcePEMEncryptorBuilder(PEM_ENCRYPTION_ALGORITHM))
		                .build(config.get(PASSWORD).toString().toCharArray());
			 pemWriter.writeObject(new JcaMiscPEMGenerator(value, penc));
		}
		else{
			 pemWriter.writeObject(new JcaMiscPEMGenerator(value));
		}
		pemWriter.close();
	}

	@Override
	public boolean isSupported(@SuppressWarnings("rawtypes") Class valueClass, String contentType) {
		return KeyPair.class.isAssignableFrom(valueClass);
	}

}

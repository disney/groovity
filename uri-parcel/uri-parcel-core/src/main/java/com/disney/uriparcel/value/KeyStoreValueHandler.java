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
package com.disney.uriparcel.value;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.Map;
/**
 * Value handler for reading and writing java keystore files; requires an encryption "password" be set
 * in the configuration; an optional type may be specified for loading keystores in a format other than PKCS12
 *
 * @author Alex Vigdor
 */
public class KeyStoreValueHandler extends AbstractValueHandler {
	public static final String KEYSTORE_TYPE = "type";
	public static final String KEYSTORE_PASSWORD = "password";

	public int getPriority() {
		return 99;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Object doLoad(InputStream stream, String contentType, Class valueClass, Map config) throws Exception {
		if(config==null){
			throw new RuntimeException("KeyStoreValueLoader requires KEYSTORE_PASSWORD and KEYSTORE_TYPE config properties");
		}
		String keystoreType = (String) config.get(KEYSTORE_TYPE);
		if(keystoreType==null){
			keystoreType="PKCS12";
		}
		String keystorePass = (String) config.get(KEYSTORE_PASSWORD);
		KeyStore keystore = KeyStore.getInstance(keystoreType);
		keystore.load(stream, keystorePass.toCharArray());
		return keystore;
	}

	@Override
	protected void doStore(OutputStream stream, String contentType, Object value, @SuppressWarnings("rawtypes") Map config) throws Exception {
		if(config==null){
			throw new RuntimeException("KeyStoreValueLoader requires 'password' and optional 'type' config properties");
		}
		String keystorePass = (String) config.get(KEYSTORE_PASSWORD);
		if(keystorePass==null){
			throw new RuntimeException("KeyStoreValueLoader requires password");
		}
		((KeyStore)value).store(stream, keystorePass.toCharArray());
	}

	@Override
	public boolean isSupported(@SuppressWarnings("rawtypes") Class valueClass, String contentType) {
		return KeyStore.class.equals(valueClass);
	}

}

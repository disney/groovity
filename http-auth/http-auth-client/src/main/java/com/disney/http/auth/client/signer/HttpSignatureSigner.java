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
package com.disney.http.auth.client.signer;

import com.disney.http.auth.Algorithms;
import com.disney.http.auth.AuthConstants;
import com.disney.http.auth.SignatureAuthorization;
import com.disney.http.auth.client.ClientAuthorizationRequest;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.utils.DateUtils;

import javax.crypto.Mac;

import java.security.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * HttpRequestInterceptor for adding the signed HTTP Signature header to an outgoing request.  Should be configured either
 * with a KeyLoader or KeyPairLoader (whose private key would be used to sign)
 *
 * @author Rachel Kobayashi
 *
 */
public class HttpSignatureSigner extends AbstractSigner implements HttpRequestInterceptor, AuthConstants {
    private Callable<? extends Key> keyLoader;
    private Callable<? extends KeyPair> keyPairLoader;
    private List<String> headers = Arrays.asList("Date");
    private String keyId;
    private String algorithm = "hmac-sha256";

    public HttpSignatureSigner(){
        super.setHeaderName(AUTHORIZATION_HEADER);
    }

    @Override
    protected String makeSignature(HttpRequest request) throws HttpException{
    	SignatureAuthorization sa = doAuthorization(request);
    	if(super.getHeaderName().equals(AUTHORIZATION_HEADER)){
            return SIGNATURE_HEADER+" "+sa.toString();
        } else {
            return sa.toString();
        }
    }
    
    public SignatureAuthorization doAuthorization(HttpRequest request) throws HttpException{
    	if(keyId == null || keyId.isEmpty()){
            throw new HttpException("Signer Configuration Error: no KeyId set");
        }
    	// fill in date field if missing from request;
        if(request.getLastHeader("x-date") == null && request.getLastHeader("date") == null){
            request.addHeader("Date", DateUtils.formatDate(new Date()));
        }
    	SignatureAuthorization sa = new SignatureAuthorization();
        sa.setAlgorithm(algorithm);
        sa.setHeaders(headers);
        sa.setKeyId(keyId);
        String signingString = sa.generateSigningString(new ClientAuthorizationRequest(request));
        try {
        	//System.out.println("Client signing string "+signingString);
        	String signingAlgorithm = Algorithms.getSecurityAlgorithm(algorithm);
        	Key key = null;
        	if(keyLoader!=null){
        		key = keyLoader.call();
        	}
        	else if(getKeyPairLoader()!=null){
        		//always sign with the private key, validate with the public key
        		key = getKeyPairLoader().call().getPrivate();
        	}
        	else{
        		throw new RuntimeException("No key loader provided for HTTP Signature Signer");
        	}
            // keyId must be set, as per protocol
            if(signingAlgorithm.startsWith("Hmac")) {
                Mac mac = Mac.getInstance(signingAlgorithm);
                mac.init(key);
                sa.setSignature(mac.doFinal(signingString.getBytes("UTF-8")));
            } else if(signingAlgorithm.endsWith("RSA")){ // rsa
                Signature rsaSigner = Signature.getInstance(signingAlgorithm);
                rsaSigner.initSign((PrivateKey) key);
                rsaSigner.update(signingString.getBytes("UTF-8"));
                sa.setSignature(rsaSigner.sign());
            } else {
                throw new NoSuchAlgorithmException("No known algorithm for "+signingAlgorithm);
            }
            return sa;
        } catch(Exception e) {
            throw new HttpException("Invalid Signature Authorization: signer was not correctly configured.",e);
        }
    }

	public List<String> getHeaders() {
		return headers;
	}

	public void setHeaders(List<String> headers) {
		this.headers = headers;
	}

	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public Callable<? extends Key> getKeyLoader() {
		return keyLoader;
	}

	public void setKeyLoader(Callable<? extends Key> keyLoader) {
		this.keyLoader = keyLoader;
	}

	public Callable<? extends KeyPair> getKeyPairLoader() {
		return keyPairLoader;
	}

	public void setKeyPairLoader(Callable<? extends KeyPair> keyPairLoader) {
		this.keyPairLoader = keyPairLoader;
	}
}

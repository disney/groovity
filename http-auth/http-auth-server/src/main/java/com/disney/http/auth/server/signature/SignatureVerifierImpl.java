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
package com.disney.http.auth.server.signature;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Mac;

import com.disney.http.auth.server.AbstractVerifier;
import com.disney.http.auth.server.AuthenticatedPrincipal;
import com.disney.http.auth.server.ServerAuthorizationRequest;
import com.disney.http.auth.AuthConstants;
import com.disney.http.auth.SignatureAuthorization;
import com.disney.http.auth.server.Verifier;
import com.disney.http.auth.server.VerifierResult;
import com.disney.http.auth.keychain.KeyChain;
/**
 * Verifier implementation of HTTP Signature Draft 5
 *
 * @author Alex Vigdor
 */
public class SignatureVerifierImpl extends AbstractVerifier implements AuthConstants, Verifier {
	private long maxDateDrift=300000;
	private List<KeyChain> keyChains;
	private List<String> requiredHeaders = Arrays.asList("date");
	
	/**
	 * Given a request and response, analyze the request Authorization header and 
	 * look up the appropriate key in the keystore; if the
	 * header is missing, incomplete or invalid, will send a 401 error on the response
	 * with WWW-Authenticate header and return null.
	 * 
	 * If the request contains a Digest header, the returned HttpServletRequest
	 * will wrap the original with one that will lazily verify the request body.  
	 * Consumers should replace the original ServletRequest with the returned one
	 * for further processing, and be prepared for the possible VerificationException
	 * 
	 * @param request
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 */
	protected VerifierResult doVerifyInternal(ServerAuthorizationRequest request) throws Exception{
		VerifierResult result = new VerifierResult();
		List<String> authHeader = request.getHeaders(AUTHORIZATION_HEADER);
		if(authHeader==null || authHeader.isEmpty() || !authHeader.get(0).startsWith(SIGNATURE_HEADER)){
			authHeader = request.getHeaders(SIGNATURE_HEADER);
		}
		//System.out.println("Received auth header "+authHeader);
		if(authHeader==null || authHeader.isEmpty()){
			challenge(result, ERROR_MISSING_SIGNATURE);
			return result;
		}
		// this will throw an exception if the auth header is not properly formatted
		SignatureAuthorization authSig;
		try{
			authSig = new SignatureAuthorization(authHeader.get(0));
		}
		catch(Exception e){
			challenge(result, e.getMessage());
			return result;
		}
		//now validate that the authSig was signed with all the required headers
		for(String required: requiredHeaders){
			if(!authSig.getHeaders().contains(required.toLowerCase())){
				if(required.toLowerCase().equals("date") && authSig.getHeaders().contains("x-date")){
					//allow x-date to substitute for date
					continue;
				}
				challenge(result, MessageFormat.format(ERROR_MISSING_HEADER_FORMAT, required));
				return result;
			}
		}
		List<String> reqDateStr = request.getHeaders("x-date");
		if(reqDateStr==null || reqDateStr.isEmpty()){
			reqDateStr = request.getHeaders("date");
		}
		//now validate the date is in range, use x-date if provided (for ajax support)
		long reqDate = -1;
		if(reqDateStr!=null && !reqDateStr.isEmpty()){
			String rd = reqDateStr.get(0);
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
			try{
				reqDate = dateFormat.parse(rd).getTime();
			}
			catch(ParseException e){
				dateFormat = new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss zzz");
				try{
					reqDate = dateFormat.parse(rd).getTime();
				}
				catch(ParseException e1){
					dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy");
					reqDate = dateFormat.parse(rd).getTime();
				}
			}
		}
		if((System.currentTimeMillis()-reqDate)>maxDateDrift){
			challenge(result, ERROR_INVALID_DATE);
			return result;
		}
		//now lookup the key
		String errorFormat = ERROR_UNKOWN_KEY_ID_FORMAT;
		Key[] keys = null;
		for(KeyChain keyChain: keyChains){
			if(keyChain.containsKey(authSig.getKeyId())){
				if(authSig.getAlgorithm().startsWith("hmac")){
					try{
						keys = keyChain.getSecretKeys(authSig.getKeyId());
					}
					catch(UnrecoverableKeyException e){
						errorFormat = ERROR_EXPECTED_HMAC_FORMAT;
					}
				}
				else{
					try{
						keys = keyChain.getPublicKeys(authSig.getKeyId());
					}
					catch(UnrecoverableKeyException e){
						errorFormat = ERROR_EXPECTED_RSA_FORMAT;
					}
				}
			}
		}
		if(keys==null || keys.length==0){
			challenge(result, MessageFormat.format(errorFormat, authSig.getKeyId()));
			return result;
		}
		String errorMessage = null;
		for(Key key: keys){
			//validate key algorithm is appropriate
			if(key.getAlgorithm().equals("RSA")){
				if(!authSig.getAlgorithm().startsWith("rsa")){
					errorMessage = MessageFormat.format(ERROR_EXPECTED_HMAC_FORMAT, authSig.getKeyId());
					continue;
				}
			}
			if(key.getAlgorithm().startsWith("Hmac")){
				if(!authSig.getAlgorithm().startsWith("hmac")){
					errorMessage = MessageFormat.format(ERROR_EXPECTED_RSA_FORMAT, authSig.getKeyId());
					continue;
				}
				
			}
			//now generate the signature to compare
			String toSign = authSig.generateSigningString(request);
			//System.out.println("Signing string is "+toSign);
			if(verifyMessage(key, toSign, authSig.getSignature(), authSig.getAlgorithm())){
				result.setAuthenticated(true);
				result.setPrincipal(new AuthenticatedPrincipal(authSig.getKeyId()));
				return result;
			}
			else{
				errorMessage = ERROR_VERIFICATION_FAILED;
			}
		}
		challenge(result, errorMessage);
		return result;
	}
	
	public static boolean verifyMessage(Key key, String message, byte[] signature, String algorithm) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, UnsupportedEncodingException{
		if(algorithm.startsWith("rsa")){
			String signingAlgorithm;
			if(algorithm.endsWith("sha1")){
				signingAlgorithm = "SHA1withRSA";
			}
			else if(algorithm.endsWith("sha256")){
				signingAlgorithm = "SHA256withRSA";
			}
			else if(algorithm.endsWith("sha384")){
				signingAlgorithm = "SHA384withRSA";
			}
			else if(algorithm.endsWith("sha512")){
				signingAlgorithm = "SHA512withRSA";
			}
			else if(algorithm.endsWith("md5")){
				signingAlgorithm = "MD5withRSA";
			}
			else{
				throw new NoSuchAlgorithmException("No known algorithm for "+algorithm);
			}
			Signature signer = Signature.getInstance(signingAlgorithm);
			signer.initVerify((PublicKey) key);
			signer.update(message.getBytes("UTF-8"));
			return signer.verify(signature);
		}
		else if(algorithm.startsWith("hmac")){
			String signingAlgorithm;
			if(algorithm.endsWith("sha1")){
				signingAlgorithm = "HmacSHA1";
			}
			else if(algorithm.endsWith("sha256")){
				signingAlgorithm = "HmacSHA256";
			}
			else if(algorithm.endsWith("sha384")){
				signingAlgorithm = "HmacSHA384";
			}
			else if(algorithm.endsWith("sha512")){
				signingAlgorithm = "HmacSHA512";
			}
			else if(algorithm.endsWith("md5")){
				signingAlgorithm = "HmacMD5";
			}
			else{
				throw new NoSuchAlgorithmException("No known algorithm for "+algorithm);
			}
			Mac mac = Mac.getInstance(signingAlgorithm);
			mac.init(key);
			mac.update(message.getBytes("UTF-8"));
			byte[] finalBytes = mac.doFinal();
			//System.out.println("Computed signature of "+message+" to compare: "+DatatypeConverter.printBase64Binary(finalBytes)+" vs "+DatatypeConverter.printBase64Binary(signature));
			return Arrays.equals(signature, finalBytes);
		}
		else{
			throw new NoSuchAlgorithmException("No known algorithm for "+algorithm);
		}
	}
	
	private void challenge(VerifierResult result, String message) throws IOException{
		StringBuilder authn = new StringBuilder(SIGNATURE_HEADER);
		authn.append(" ").append(REALM).append("=\"").append(getRealm()).append("\",").append(HEADERS).append("=\"");
		if(requiredHeaders != null){
			for(int i=0;i<requiredHeaders.size();i++){
				if(i>0){
					authn.append(" ");
				}
				authn.append(requiredHeaders.get(i));
			}
			//force date to be required per the spec
			if(!requiredHeaders.contains("date")){
				if(requiredHeaders.size()>0){
					authn.append(" ");
				}
				authn.append("date");
			}
		}
		authn.append("\"");
		result.setChallenge(authn.toString());
		result.setAuthenticated(false);
		result.setMessage(message);
	}

	public long getMaxDateDrift() {
		return maxDateDrift;
	}

	public void setMaxDateDrift(long maxDateDrift) {
		this.maxDateDrift = maxDateDrift;
	}

	public List<String> getRequiredHeaders() {
		return requiredHeaders;
	}

	public void setRequiredHeaders(List<String> requiredHeaders) {
		this.requiredHeaders = requiredHeaders;
	}

	public List<KeyChain> getKeyChains() {
		return keyChains;
	}

	public void setKeyChains(List<KeyChain> keyChains) {
		this.keyChains = keyChains;
	}

	
}

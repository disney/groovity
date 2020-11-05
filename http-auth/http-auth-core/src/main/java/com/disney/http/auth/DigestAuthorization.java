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
package com.disney.http.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Parse or format a Digest Authorization header
 * 
 * @author Alex Vigdor
 */
public class DigestAuthorization implements AuthConstants{
	private String username;
	private String uri;
	private String qop;
	private String nonce;
	private String cnonce;
	private String nonceCount;
	private byte[] digest;
	private String realm;
	private String opaque;
	private String algorithm="MD5";
	
	public DigestAuthorization(){
		
	}
	
	public DigestAuthorization(String authHeader){
		//support either Authorization or Signature header format
				if(authHeader.startsWith(DIGEST)){
					authHeader = authHeader.substring(DIGEST.length()+1);
				}
				String[] parts = authHeader.split(",");
				for(String part: parts){
					int ep = part.indexOf("=");
					if(ep==-1){
						throw new IllegalArgumentException("Unrecognized statement in signature: "+part);
					}
					String[] pair = new String[] {part.substring(0,ep),part.substring(ep+1)};
					String key = pair[0].trim();
					String value = pair[1].trim();
					if(value.startsWith("\"")){
						if(value.endsWith("\"")){
							value = value.substring(1, value.length()-1);
						}
					}
					if(USERNAME.equals(key)){
						this.username=value;
					}
					else if(REALM.equals(key)){
						this.realm =value;
					}
					else if(NONCE.equals(key)){
						this.nonce = value;
					}
					else if(URI.equals(key)){
						this.uri = value;
					}
					else if(QOP.equals(key)){
						this.qop = value;
					}
					else if(NONCE_COUNT.equals(key)){
						this.nonceCount = value;
					}
					else if(CNONCE.equals(key)){
						this.cnonce = value;
					}
					else if(OPAQUE.equals(key)){
						this.opaque = value;
					}
					else if(RESPONSE.equals(key)){
						this.digest = decodeHex(value);
					}
					else if(ALGORITHM.equals(key)){
						if(value!=null && !value.trim().equals("")){
							this.algorithm=value;
						}
					}
					else if("auth-param".equals(key)){
						//do nothing for now
					}
					else{
						throw new IllegalArgumentException("Unrecognized component of digest auth: "+key);
					}
				}
				if(qop!=null){
					if(cnonce==null || nonceCount==null){
						throw new IllegalArgumentException("Incomplete digest authentication request");
					}
				}
				if(username==null || realm ==null || nonce==null || uri==null || digest==null || opaque==null){
					throw new IllegalArgumentException("Incomplete digest authentication request");
				}
	}

	public static String encodeHex(byte[] value) {
		char[] encoded = new char[value.length*2];
		int p = 0;
		for(int i=0; i < value.length; i++){
			byte b = value[i];
			encoded[p++] = Character.forDigit((b >> 4) & 0xF, 16);
			encoded[p++] = Character.forDigit((b & 0xF), 16);
		}
		return new String(encoded);
	}

	public static byte[] decodeHex(String str) {
		byte[] decoded = new byte[str.length()/2];
		for(int i=0; i< str.length(); i+=2) {
			int result = Character.digit(str.charAt(i), 16) << 4;
			result += Character.digit(str.charAt(i+1), 16);
			decoded[i/2]=(byte)result;
		}
		return decoded;
	}
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getQop() {
		return qop;
	}

	public void setQop(String qop) {
		this.qop = qop;
	}

	public String getCnonce() {
		return cnonce;
	}

	public void setCnonce(String cnonce) {
		this.cnonce = cnonce;
	}

	public String getNonceCount() {
		return nonceCount;
	}

	public void setNonceCount(String nonceCount) {
		this.nonceCount = nonceCount;
	}

	public byte[] getDigest() {
		return digest;
	}

	public void setDigest(byte[] digest) {
		this.digest = digest;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getNonce() {
		return nonce;
	}

	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	public String getOpaque() {
		return opaque;
	}

	public void setOpaque(String opaque) {
		this.opaque = opaque;
	}
	
	public String generateSigningString(String credentialDigest, AuthorizationRequest request) throws NoSuchAlgorithmException{
		StringBuilder sb = new StringBuilder(credentialDigest);
		sb.append(":").append(nonce).append(":");
		if("auth".equals(getQop())){
			sb.append(nonceCount).append(":").append(cnonce).append(":").append(qop).append(":");
		}
		sb.append(digest(request.getMethod(),request.getURI()));
		return sb.toString();
	}
	
	public String generateSigningString(String username, String password, AuthorizationRequest request) throws NoSuchAlgorithmException{
		String creds = digest(username,realm,password);
		return generateSigningString(creds, request);
	}
	
	private String digest(String... strings) throws NoSuchAlgorithmException{
		MessageDigest md5 = MessageDigest.getInstance(algorithm);
		for(int i=0;i<strings.length;i++){
			if(i>0){
				md5.update((byte)':');
			}
			if(strings[i]!=null){
				md5.update(strings[i].getBytes());
			}
		}
		return encodeHex(md5.digest()).toLowerCase();
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder(DIGEST);
		sb.append(" username=\"").append(username).append("\", ");
		sb.append("realm=\"").append(realm).append("\", ");
		sb.append("nonce=\"").append(nonce).append("\", ");
		sb.append("uri=\"").append(uri).append("\", ");
		if(qop!=null){
			sb.append("qop=").append(qop).append(", ");
			sb.append("nc=").append(nonceCount).append(", ");
			sb.append("cnonce=\"").append(cnonce).append("\", ");
		}
		sb.append("response=\"").append(encodeHex(digest).toLowerCase()).append("\", ");
		sb.append("opaque=\"").append(opaque).append("\"");
		return sb.toString();
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}
}

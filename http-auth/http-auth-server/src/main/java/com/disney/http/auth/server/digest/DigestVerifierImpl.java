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
package com.disney.http.auth.server.digest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import com.disney.http.auth.AuthConstants;
import com.disney.http.auth.DigestAuthorization;
import static com.disney.http.auth.DigestAuthorization.encodeHex;
import com.disney.http.auth.server.AbstractVerifier;
import com.disney.http.auth.server.AuthenticatedPrincipal;
import com.disney.http.auth.server.ServerAuthorizationRequest;
import com.disney.http.auth.server.VerifierResult;
/**
 * Digest verifier implementation performs Digest authentication according to RFC 2617, including the "auth" level of QOP
 * <p>
 * This implementation relies on one or more PasswordDigesters to provide the necessary credential validation
 *
 * @author Alex Vigdor
 */
public class DigestVerifierImpl extends AbstractVerifier implements AuthConstants{
	private List<PasswordDigester> passwordDigesters;
	private String nonceSecret = "HttpAuthNonce";
	private long maxNonceAge = 120000;
	private String domain = "/";

	@Override
	protected VerifierResult doVerifyInternal(ServerAuthorizationRequest request) throws NoSuchAlgorithmException, IOException {
		VerifierResult result = new VerifierResult();
		List<String> headers = request.getHeaders(AUTHORIZATION_HEADER);
		String authHeader = null;
		for(String header: headers){
			if(header.startsWith(DIGEST)){
				authHeader = header;
				break;
			}
		}
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		if(authHeader==null){
			challenge(request, md5,result,ERROR_MISSING_CREDENTIALS,false);
			return result;
		}
		DigestAuthorization authd;
		try{
			authd = new DigestAuthorization(authHeader);
		}
		catch(Exception e){
			challenge(request, md5,result,e.getMessage(),false);
			return result;
		}
		//validate URI
		if(!request.getURI().equals(authd.getUri())){
			challenge(request, md5,result,ERROR_INCORRECT_URI,false);
			return result;
		}
		String nonce = authd.getNonce();
		byte[] nonceBytes = Base64.getDecoder().decode(nonce);
		//validate timestamp
		long timestamp = toLong(nonceBytes);
		//validate nonce
		if(!nonce.equals(makeNonce(md5, timestamp))){
			challenge(request, md5,result,ERROR_INVALID_NONCE,false);
			return result;
		}
		//validate digest
		byte[] ha2 = encodeHex(md5.digest((request.getMethod()+":"+authd.getUri()).getBytes("UTF-8"))).toLowerCase().getBytes();
		for(int i=0;i<passwordDigesters.size();i++){
			PasswordDigester digester = passwordDigesters.get(i);
			byte[] ha1 = digester.digest(authd.getUsername(), getRealm());
			if(ha1!=null){
				ByteArrayOutputStream baos = new ByteArrayOutputStream(200);
				baos.write(encodeHex(ha1).toLowerCase().getBytes());
				baos.write((byte)':');
				baos.write(nonce.getBytes());
				baos.write((byte)':');
				if("auth".equals(authd.getQop())){
					baos.write(authd.getNonceCount().getBytes());
					baos.write((byte)':');
					baos.write(authd.getCnonce().getBytes("UTF-8"));
					baos.write((byte)':');
					baos.write(authd.getQop().getBytes());
					baos.write((byte)':');
				}
				byte[] rd = baos.toByteArray();
				md5.reset();
				md5.update(rd);
				md5.update(ha2);
				byte[] digestVal = md5.digest();
				if(Arrays.equals(digestVal,authd.getDigest())){
					if("auth".equals(authd.getQop())){
						md5.reset();
						md5.update(rd);
						md5.update(encodeHex(md5.digest((":"+authd.getUri()).getBytes("UTF-8"))).toLowerCase().getBytes());
						byte[] rspAuth = md5.digest();
						result.setAuthenticationInfo("qop=\""+authd.getQop()+"\",cnonce=\""+authd.getCnonce()+"\",nc="+authd.getNonceCount()+",rspauth=\""+encodeHex(rspAuth).toLowerCase()+"\"");
					}
					if((System.currentTimeMillis()-timestamp) > maxNonceAge){
						challenge(request, md5,result,ERROR_STALE_NONCE,true);
					}
					else{
						result.setAuthenticated(true);
						result.setPrincipal(new AuthenticatedPrincipal(authd.getUsername()));
					}
					return result;
				}
			}
		}
		challenge(request, md5,result,ERROR_UNKNOWN_CREDENTIALS,false);
		return result;
	}
	
	private void challenge(ServerAuthorizationRequest request, MessageDigest digester, VerifierResult result, String message, boolean stale) throws UnsupportedEncodingException{
		StringBuilder authn = new StringBuilder(DIGEST);
		authn.append(" ").append(REALM).append("=\"").append(getRealm()).append("\", qop=\"auth\", nonce=\"");
		long curTime = System.currentTimeMillis();
		authn.append(makeNonce(digester,curTime)).append("\", opaque=\"");
		authn.append(Base64.getEncoder().encodeToString(toBytes(curTime))).append("\"");
		if(domain!=null){
			authn.append(", domain=\"").append(domain).append("\"");
		}
		if(stale){
			authn.append(", stale=\"true\"");
		}
		//System.out.println("digest challenge: "+message+"; "+authn.toString());
		result.setChallenge(authn.toString());
		result.setAuthenticated(false);
		result.setMessage(message);
	}
	
	private String makeNonce(MessageDigest digest, long timestamp) throws UnsupportedEncodingException{
		digest.reset();
		String nonceSource = String.valueOf(timestamp)+":"+nonceSecret;
		byte[] nonceHash = digest.digest(nonceSource.getBytes("UTF-8"));
		byte[] nonce = new byte[nonceHash.length+8];
		toBytes(timestamp,nonce);
		System.arraycopy(nonceHash, 0, nonce, 8, nonceHash.length);
		return Base64.getEncoder().encodeToString(nonce);
	}
	
	public static byte[] toBytes(long val) {
		byte [] b = new byte[8];
		for (int i = 7; i > 0; i--) {
			b[i] = (byte) val;
			val >>>= 8;
		}
		b[0] = (byte) val;
		return b;
	}
	public static void toBytes(long val, byte[] b) {
		for (int i = 7; i > 0; i--) {
			b[i] = (byte) val;
			val >>>= 8;
		}
		b[0] = (byte) val;
	}
	public static long toLong(byte[] bytes){
		long l = 0;
	    for(int i = 0; i < 8; i++) {
	      l <<= 8;
	      l ^= bytes[i] & 0xFF;
	    }
	    return l;
	}


	public List<PasswordDigester> getPasswordDigesters() {
		return passwordDigesters;
	}

	public void setPasswordDigesters(List<PasswordDigester> passwordDigesters) {
		this.passwordDigesters = passwordDigesters;
	}

	public String getNonceSecret() {
		return nonceSecret;
	}

	public void setNonceSecret(String nonceSecret) {
		this.nonceSecret = nonceSecret;
	}

	public long getMaxNonceAge() {
		return maxNonceAge;
	}

	public void setMaxNonceAge(long maxNonceAge) {
		this.maxNonceAge = maxNonceAge;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

}

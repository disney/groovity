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

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Parse or format an authorization signature string from or to a header
 * 
 * @author vigdora
 *
 */
public class SignatureAuthorization implements AuthConstants {
	private String keyId;
	private String algorithm;
	private List<String> headers;
	private byte[] signature;
	
	public SignatureAuthorization(){
		
	}
	
	public SignatureAuthorization(String authHeader) {
		//support either Authorization or Signature header format
		if(authHeader.startsWith(SIGNATURE_HEADER)){
			authHeader = authHeader.substring(SIGNATURE_HEADER.length()+1);
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
			if(KEY_ID.equals(key)){
				this.setKeyId(value);
			}
			else if(HEADERS.equals(key)){
				this.setHeaders(Arrays.asList(value.toLowerCase().split(" ")));
			}
			else if(ALGORITHM.equals(key)){
				this.setAlgorithm(value);
			}
			else if(SIGNATURE.equals(key)){
				this.setSignature(Base64.getDecoder().decode(value));
			}
			else{
				throw new IllegalArgumentException("Unrecognized component of signature: "+key);
			}
		}
		if(keyId==null || algorithm ==null || headers.size()==0 || signature==null){
			throw new IllegalArgumentException("Incomplete signature");
		}
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
	public List<String> getHeaders() {
		return headers;
	}
	public void setHeaders(List<String> headers) {
		this.headers = headers;
	}
	public byte[] getSignature() {
		return signature;
	}
	public void setSignature(byte[] signature) {
		this.signature = signature;
	}
	
	public String generateSigningString(AuthorizationRequest request){
		StringBuilder sb = new StringBuilder();
		List<String> authHeaders = this.getHeaders();
		for(int i=0;i<authHeaders.size();i++){
			if(i>0){
				sb.append("\n");
			}
			String headerName = authHeaders.get(i);
			if(headerName.equals(REQUEST_TARGET)){
				sb.append(REQUEST_TARGET).append(": ");
				sb.append(request.getMethod().toLowerCase()).append(" ");
				sb.append(request.getURI());
			}
			else{
				sb.append(headerName.toLowerCase()).append(": ");
				List<String> vals = request.getHeaders(headerName);
				for(int pos=0;pos<vals.size();pos++){
					String val = vals.get(pos);
					if(pos>0){
						sb.append(", ");
					}
					sb.append(val);
				}
			}
		}
		return sb.toString();
	}
	
	public String toString(){
		StringBuilder builder = new StringBuilder(KEY_ID);
		builder.append("=\"").append(keyId).append("\",");
		builder.append(ALGORITHM).append("=\"").append(algorithm).append("\",");
		builder.append(HEADERS).append("=\"");
		for(int i=0;i<headers.size();i++){
			if(i>0){
				builder.append(" ");
			}
			builder.append(headers.get(i));
		}
		builder.append("\",");
		builder.append(SIGNATURE).append("=\"").append(Base64.getEncoder().encodeToString(signature)).append("\"");
		return builder.toString();
	}

}

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
/**
 * Constant strings used in HTTP Auth components
 *
 * @author Alex Vigdor
 */
public interface AuthConstants {
	public final static String AUTHORIZATION_HEADER = "Authorization";
	public final static String SIGNATURE_HEADER = "Signature";
	public final static String BASIC = "Basic";
	public final static String DIGEST = "Digest";
	public final static String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
	public final static String KEY_ID = "keyId";
	public final static String ALGORITHM = "algorithm";
	public final static String HEADERS = "headers";
	public final static String SIGNATURE = "signature";
	public final static String REALM = "realm";
	public final static String REQUEST_TARGET = "(request-target)";
	public final static String AUTHENTICATION_INFO = "Authentication-Info";
	
	//DIGEST authorization fields
	public final static String USERNAME = "username";
	public final static String NONCE = "nonce";
	public final static String URI = "uri";
	public final static String QOP = "qop";
	public final static String NONCE_COUNT = "nc";
	public final static String CNONCE = "cnonce";
	public final static String RESPONSE = "response";
	public final static String OPAQUE = "opaque";
	
	public final static String ERROR_MISSING_SIGNATURE = "This URL requires a signature";
	public final static String ERROR_MISSING_HEADER_FORMAT = "This URL requires the signature to incorporate the header {0}";
	public final static String ERROR_INVALID_DATE = "This URL requires a timely Date header";
	public final static String ERROR_UNKOWN_KEY_ID_FORMAT = "No key found for keyId {0}";
	public final static String ERROR_EXPECTED_RSA_FORMAT ="Expected to find RSA key, found Hmac instead for {0}";
	public final static String ERROR_EXPECTED_HMAC_FORMAT = "Expected to find Hmac key, found RSA instead for {0}";
	public final static String ERROR_VERIFICATION_FAILED = "Failed to verify signature";
	
	public final static String ERROR_MISSING_CREDENTIALS = "This URL requires credentials";
	public final static String ERROR_UNKNOWN_CREDENTIALS = "The provided credentials are not recognized";
	
	public final static String ERROR_INCORRECT_URI = "The requested URI is not represented in the authentication request";
	public final static String ERROR_STALE_NONCE = "The provided nonce has expired";
	public final static String ERROR_INVALID_NONCE = "The provided nonce is not recognized";
	
	public final static String ERROR_NOT_AUTHORIZED = "You are not authorized to access this URL";
}

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

import java.security.NoSuchAlgorithmException;

/**
 * Convert from http signature algorithm names, e.g. "hmac-sha256", to java security algorithms, e.g. "HmacSHA256"
 * 
 * @author Alex Vigdor
 */
public class Algorithms {
	public static final String getSecurityAlgorithm(String signatureAlgorithm) throws NoSuchAlgorithmException{
		if(signatureAlgorithm.startsWith("rsa")){
			if(signatureAlgorithm.endsWith("sha1")){
				return "SHA1withRSA";
			}
			else if(signatureAlgorithm.endsWith("sha256")){
				return "SHA256withRSA";
			}
			else if(signatureAlgorithm.endsWith("sha384")){
				return "SHA384withRSA";
			}
			else if(signatureAlgorithm.endsWith("sha512")){
				return "SHA512withRSA";
			}
			else if(signatureAlgorithm.endsWith("md5")){
				return "MD5withRSA";
			}
		}
		else if(signatureAlgorithm.startsWith("hmac")){
			if(signatureAlgorithm.endsWith("sha1")){
				return "HmacSHA1";
			}
			else if(signatureAlgorithm.endsWith("sha256")){
				return "HmacSHA256";
			}
			else if(signatureAlgorithm.endsWith("sha384")){
				return "HmacSHA384";
			}
			else if(signatureAlgorithm.endsWith("sha512")){
				return "HmacSHA512";
			}
			else if(signatureAlgorithm.endsWith("md5")){
				return "HmacMD5";
			}
		}
		throw new NoSuchAlgorithmException("No known algorithm for "+signatureAlgorithm);
	}

    public static final String getHttpAlgorithm(String signatureAlgorithm) throws NoSuchAlgorithmException{
        if(signatureAlgorithm.endsWith("RSA")){
            if(signatureAlgorithm.endsWith("SHA1")){
                return "rsa-sha1";
            }
            else if(signatureAlgorithm.startsWith("SHA256")){
                return "rsa-sha256";
            }
            else if(signatureAlgorithm.startsWith("SHA384")){
                return "rsa-sha384";
            }
            else if(signatureAlgorithm.startsWith("SHA512")){
                return "rsa-sha512";
            }
            else if(signatureAlgorithm.startsWith("MD5")){
                return "rsa-md5";
            }
        }
        else if(signatureAlgorithm.startsWith("Hmac")){
            if(signatureAlgorithm.endsWith("SHA1")){
                return "hmac-sha1";
            }
            else if(signatureAlgorithm.endsWith("SHA256")){
                return "hmac-sha256";
            }
            else if(signatureAlgorithm.endsWith("SHA384")){
                return "hmac-sha384";
            }
            else if(signatureAlgorithm.endsWith("SHA512")){
                return "hmac-sha512";
            }
            else if(signatureAlgorithm.endsWith("MD5")){
                return "hmac-md5";
            }
        }
        throw new NoSuchAlgorithmException("No known algorithm for "+signatureAlgorithm);
    }
}

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
package com.disney.groovity.tags;

import groovy.lang.Closure;
import groovy.lang.Writable;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.activation.DataSource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.HttpEntity;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
/**
 * Generate an HMAC signature given a key (secret) and value (message)
 * <p>
 * hmac( <ul>	
 *	<li><b>key</b>: 
 *	a string secret or java.security.Key object to use in signing,</li>	
 *	<li><i>value</i>: 
 *	the message (string or byte[]) to sign; alternative to using tag body,</li>	
 *	<li><i>var</i>: 
 *	variable name for the signature, also returned,</li>	
 *	<li><i>algorithm</i>: 
 *	sha256, sha1 or md5; defaults to sha256,</li>	
 *	<li><i>encoding</i>: 
 *	base64, hex or none; defaults to base64, none creates raw byte[],</li>
 *	</ul>{
 *	<blockquote>// Optionally the value or message to sign; alternative to using value attribute</blockquote>
 * 	});
 * 
 *	<p><b>returns</b> The signature in its encoded form
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;~&lt;g:hmac var=&quot;sig&quot; key=&quot;${secret}&quot; value=&quot;${message}&quot; algorithm=&quot;md5&quot; /&gt;~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
	info = "Generate an HMAC signature given a key (secret) and value (message)",
	body = "Optionally the value or message to sign; alternative to using value attribute",
	sample="<~<g:hmac var=\"sig\" key=\"${secret}\" value=\"${message}\" algorithm=\"md5\" />~>",
	returns="The signature in its encoded form",
	attrs = { 
		@Attr(
			name = "key", 
			info="a string secret or java.security.Key object to use in signing",
			required = true
		),
		@Attr(
			name = GroovityConstants.VALUE, 
			info="the message (string or byte[]) to sign; alternative to using tag body",
			required = false
		),
		@Attr(
			name = GroovityConstants.VAR, 
			info="variable name for the signature, also returned",
			required = false
		),
		@Attr(
			name = "algorithm", 
			info="sha256, sha1 or md5; defaults to sha256",
			required = false
		),
		@Attr(
			name = "encoding", 
			info="base64, hex or none; defaults to base64, none creates raw byte[]",
			required = false
		)
	} 
)
public class Hmac implements Taggable{

	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) throws NoSuchAlgorithmException, InvalidKeyException, IOException {
		Key useKey = null;
		Object key = resolve(attributes,"key");
		if(key==null){
			throw new RuntimeException("<g:hmac> requires a key for signing");
		}
		if(key instanceof Key){
			useKey = (Key) key;
		}
		String useAlgorithm = "HmacSHA256";
		Object algorithm = resolve(attributes,"algorithm");
		if(algorithm!=null){
			String testAlgorithm = algorithm.toString();
			if("sha256".equalsIgnoreCase(testAlgorithm)){
				useAlgorithm="HmacSHA256";
			}
			else if("sha1".equalsIgnoreCase(testAlgorithm)){
				useAlgorithm="HmacSHA1";
			}
			else if("sha512".equalsIgnoreCase(testAlgorithm)){
				useAlgorithm="HmacSHA512";
			}
			else if("md5".equalsIgnoreCase(testAlgorithm)){
				useAlgorithm="HmacMD5";
			}
			else{
				//instead of throwing an exception, assume it could be some non-standard HMac implementation and leave it up to the JVM
				//to throw an exception when generating the key if it is not available in the current runtime environment
				useAlgorithm=testAlgorithm;
			}
		}
		if(useKey==null){
			useKey = new SecretKeySpec(key.toString().getBytes("UTF-8"), useAlgorithm);
		}
		String useEncoding = "base64";
		Object encoding = resolve(attributes,"encoding");
		if(encoding!=null){
			String testEncoding = encoding.toString().trim();
			if(testEncoding.length()>0){
				if("base64".equalsIgnoreCase(testEncoding)){
					useEncoding="base64";
				}
				else if("hex".equalsIgnoreCase(testEncoding)){
					useEncoding="hex";
				}
				else if("none".equalsIgnoreCase(testEncoding)){
					useEncoding="none";
				}
				else{
					throw new RuntimeException("Unsupported hmac encoding: "+testEncoding);
				}
			}
		}
		Object var = attributes.get(VAR);
		Object value = resolve(attributes,VALUE);
		if(value==null){
			//grab value from body if it didn't come from the attribute
			Object oldOut = get(body,OUT);
			CharArrayWriter writer = new CharArrayWriter();
			bind(body,OUT, writer);
			try{
				Object rval = body.call();
				if(writer.size()==0) {
					if(rval instanceof Writable){
						((Writable)rval).writeTo(writer);
					}
					else if(rval instanceof CharSequence) {
						writer.append((CharSequence)rval);
					}
				}
			}
			finally{
				bind(body,OUT, oldOut);
			}
			value = writer.toString();
		}
		Mac mac = Mac.getInstance(useAlgorithm);
		mac.init(useKey);
		if(value instanceof byte[]){
			mac.update((byte[])value);
		}
		else if(value instanceof InputStream) {
			macStream(mac, (InputStream) value);
		}
		else if(value instanceof File) {
			try(FileInputStream fis = new FileInputStream((File)value)){
				macStream(mac,fis);
			}
		}
		else if(value instanceof HttpEntity) {
			try(InputStream is = ((HttpEntity) value).getContent()){
				macStream(mac, is);
			}
		}
		else if(value instanceof DataSource) {
			try(InputStream is = ((DataSource)value).getInputStream()){
				macStream(mac, is);
			}
		}
		else{
			mac.update(value.toString().getBytes("UTF-8"));
		}
		byte[] sig = mac.doFinal();
		if(useEncoding.equals("none")){
			if(var!=null){
				bind(body,var.toString(), sig);
			}
			return sig;
		}
		else{ 
			String encoded = null;
			if(useEncoding.equals("base64")){
				encoded = DatatypeConverter.printBase64Binary(sig);
			}
			else if(useEncoding.equals("hex")){
				encoded = DatatypeConverter.printHexBinary(sig);
			}
			if(var!=null){
				bind(body,var.toString(), encoded);
			}
			return encoded;
		}
	}

	private void macStream(Mac mac, InputStream stream) throws IOException {
		byte[] buf = new byte[8192];
		int c = 0;
		while((c=stream.read(buf))!=-1) {
			mac.update(buf, 0, c);
		}
	}
}

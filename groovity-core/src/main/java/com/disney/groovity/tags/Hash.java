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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

import javax.activation.DataSource;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpEntity;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.util.HashWriter;
/**
 * Generate the hash of a value (message)
 * <p>
 * hash( <ul>	
 *	<li><i>value</i>: 
 *	the message (string or byte[]) to hash; alternative to using tag body,</li>	
 *	<li><i>var</i>: 
 *	variable name for the hash, also returned</li>	
 *	<li><i>algorithm</i>: 
 *	sha256, sha1 or md5; defaults to sha256,</li>	
 *	<li><i>encoding</i>: 
 *	base64, hex or none; defaults to base64, none assigns raw byte[] to var,</li>
 *	</ul>{
 *	<blockquote>// Optionally the value or message to hash; alternative to using value attribute</blockquote>
 * 	});
 *	
 *	<p><b>returns</b> The hash in its encoded form
 *
 *	<p>Sample
 *	<pre>
 *	&lt;~&lt;g:hash var=&quot;h&quot; value=&quot;${message}&quot; algorithm=&quot;md5&quot; /&gt;~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */
@Tag(
	info = "Generate the hash of a value (message)",
	body = "Optionally the value or message to hash; alternative to using value attribute",
	sample="<~<g:hash var=\"h\" value=\"${message}\" algorithm=\"md5\" />~>",
	returns="The hash in its encoded form",
	attrs = { 
		@Attr(
			name = GroovityConstants.VALUE, 
			info="the message (string or byte[]) to hash; alternative to using tag body",
			required = false
		),
		@Attr(
			name = GroovityConstants.VAR, 
			info="variable name for the hash, also returned",
			required = false
		),
		@Attr(
			name = "algorithm", 
			info="sha256, sha1 or md5; defaults to sha256",
			required = false
		),
		@Attr(
			name = "encoding", 
			info="base64, hex or none; defaults to base64, none assigns raw byte[] to var",
			required = false
		)
	} 
)
public class Hash implements Taggable{

	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) throws NoSuchAlgorithmException, InvalidKeyException, IOException {
		String useAlgorithm = "SHA-256";
		Object algorithm = resolve(attributes,"algorithm");
		if(algorithm!=null){
			String testAlgorithm = algorithm.toString();
			if("sha256".equalsIgnoreCase(testAlgorithm)){
				useAlgorithm="SHA-256";
			}
			else if("sha1".equalsIgnoreCase(testAlgorithm)){
				useAlgorithm="SHA-1";
			}
			else if("sha512".equalsIgnoreCase(testAlgorithm)){
				useAlgorithm="SHA-512";
			}
			else if("md5".equalsIgnoreCase(testAlgorithm)){
				useAlgorithm="MD5";
			}
			else{
				useAlgorithm=testAlgorithm;
			}
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
					throw new RuntimeException("Unsupported hash encoding: "+testEncoding);
				}
			}
		}
		MessageDigest md = MessageDigest.getInstance(useAlgorithm);
		byte[] hash;
		Object var = attributes.get(VAR);
		Object value = resolve(attributes,VALUE);
		if(value==null){
			//grab value from body if it didn't come from the attribute
			Object oldOut = get(body,OUT);
			HashWriter writer = new HashWriter(md, Charset.forName("UTF-8").newEncoder());
			bind(body,OUT, writer);
			try{
				Object rval = body.call();
				if(!writer.isUsed()) {
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
				writer.close();
			}
			hash = writer.getHash();
		}
		else {
			if(value instanceof byte[]){
				md.update((byte[])value);
			}
			else if(value instanceof InputStream) {
				digestStream(md, (InputStream) value);
			}
			else if(value instanceof File) {
				try(FileInputStream fis = new FileInputStream((File)value)){
					digestStream(md,fis);
				}
			}
			else if(value instanceof HttpEntity) {
				try(InputStream is = ((HttpEntity) value).getContent()){
					digestStream(md, is);
				}
			}
			else if(value instanceof DataSource) {
				try(InputStream is = ((DataSource)value).getInputStream()){
					digestStream(md, is);
				}
			}
			else{
				md.update(value.toString().getBytes("UTF-8"));
			}
			hash = md.digest();
		}
		if(useEncoding.equals("none")){
			if(var!=null){
				bind(body,var.toString(), hash);
			}
			return hash;
		}
		else{ 
			String encoded = null;
			if(useEncoding.equals("base64")){
				encoded = Base64.getEncoder().encodeToString(hash);
			}
			else if(useEncoding.equals("hex")){
				encoded = Hex.encodeHexString(hash);
			}
			if(var!=null){
				bind(body,var.toString(), encoded);
			}
			return encoded;
		}
	}

	private void digestStream(MessageDigest md, InputStream stream) throws IOException {
		byte[] buf = new byte[8192];
		int c = 0;
		while((c=stream.read(buf))!=-1) {
			md.update(buf, 0, c);
		}
	}

}

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
package com.disney.uriparcel;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an implementation-agnostic handle on a remote stream, 
 * including the URI at which it is located, a callable to retrieve its stream,
 * and getters and setters for modified time, eTag, content length and type.
 * 
 * Used by the URIParcel to abstract the retrieval of content metadata and streams from the handling thereof.
 * 
 * @author Alex Vigdor
 */
public class ContentStream {
	private static final Pattern charsetPattern = Pattern.compile("(?i)(?<=charset=)([^;,\\r\\n]+)");
	
	private URI uri;
	private long lastModified;
	private String contentType;
	private long contentLength;
	private String eTag;
	private Callable<InputStream> content;
	
	public ContentStream(){
		
	}
	
	public ContentStream(URI uri){
		this.uri = uri;
	}
	
	public URI getUri() {
		return uri;
	}
	public void setUri(URI uri) {
		this.uri = uri;
	}
	public long getLastModified() {
		return lastModified;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + (int) (contentLength ^ (contentLength >>> 32));
		result = prime * result + ((contentType == null) ? 0 : contentType.hashCode());
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + (int) (lastModified ^ (lastModified >>> 32));
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContentStream other = (ContentStream) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (contentLength != other.contentLength)
			return false;
		if (contentType == null) {
			if (other.contentType != null)
				return false;
		} else if (!contentType.equals(other.contentType))
			return false;
		if (eTag == null) {
			if (other.eTag != null)
				return false;
		} else if (!eTag.equals(other.eTag))
			return false;
		if (lastModified != other.lastModified)
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}
	public String getContentType() {
		return contentType;
	}
	public static String getCharset(String contentType){
		if(contentType!=null){
			Matcher charMatcher = charsetPattern.matcher(contentType);
			if(charMatcher.find()){
				return charMatcher.group(1);
			}
		}
		return null;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public long getContentLength() {
		return contentLength;
	}
	public void setContentLength(long contentLength) {
		this.contentLength = contentLength;
	}
	public String geteTag() {
		return eTag;
	}
	public void seteTag(String eTag) {
		this.eTag = eTag;
	}
	public Callable<InputStream> getContent() {
		return content;
	}
	public void setContent(Callable<InputStream> content) {
		this.content = content;
	}
}

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
package com.disney.http.auth.client;

import com.disney.http.auth.AuthorizationRequest;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpUriRequest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Masks a HttpRequest to an Authorization Request, so it can be used with the Authorization classes.
 *
 * @author Rachel Kobayashi
 *
 */
public class ClientAuthorizationRequest implements AuthorizationRequest {
    private final HttpRequest httpRequest;
    private final String uri;
    private final String host;

	public ClientAuthorizationRequest(final HttpRequest httpRequest) {
		this.httpRequest = httpRequest;
		if (httpRequest instanceof HttpUriRequest) {
			URI uri = ((HttpUriRequest) httpRequest).getURI();
			String h = uri.getHost();
			if (uri.getScheme().equals("http")) {
				if (uri.getPort() != 80) {
					h += ":" + uri.getPort();
				}
			} else if (uri.getScheme().equals("https")) {
				if (uri.getPort() != 443) {
					h += ":" + uri.getPort();
				}
			}
			this.host = h;
			this.uri = uri.getPath() + (uri.getRawQuery() == null ? "" : "?".concat(uri.getRawQuery()));
		} else {
			this.uri = null;
			this.host = null;
		}
	}

    @Override
    public String getMethod() {
        return httpRequest.getRequestLine().getMethod();
    }

    @Override
    public String getURI() {
    	if(this.uri!=null){
    		return uri;
    	}
        return httpRequest.getRequestLine().getUri();
    }

    @Override
    public List<String> getHeaders(String headerName) {
        Header[] headers = httpRequest.getHeaders(headerName);
        List<String> headerValues = new ArrayList<String>(headers.length);
        for (int i = 0; i < headers.length; i++) {
            headerValues.add(headers[i].getValue());
        }
        if(this.host!=null && headerValues.isEmpty() && headerName.equalsIgnoreCase("host")){
        	headerValues.add(host);
        }
        return headerValues;
    }
}

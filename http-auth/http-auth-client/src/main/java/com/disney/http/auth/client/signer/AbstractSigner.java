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
package com.disney.http.auth.client.signer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

/**
 * Abstract request signer
 *
 * @author Alex Vigdor
 */
public abstract class AbstractSigner implements HttpRequestInterceptor{
    private String headerName;
    private Pattern uriPattern;

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        if(!(request instanceof HttpUriRequest)){
            return;
        }
        URI uri = ((HttpUriRequest)request).getURI();

        if(uriPattern!=null){
            //validate URI against REGEX - only sign desired URLs
            URI absolute = uri;
            if(!uri.isAbsolute()){
            	HttpHost host = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
                if(host!=null){ 
	                URIBuilder abs = new URIBuilder(uri);
	                abs.setHost(host.toHostString());
	                abs.setScheme(host.getSchemeName());
	                try {
	                    absolute = abs.build();
	                } catch (URISyntaxException e) {
	                    e.printStackTrace();
	                }
                }
            }
            Matcher matcher = uriPattern.matcher(absolute.toString());
            if(!matcher.matches()){
                return;
            }
        }
        String value = makeSignature(request);
        if(headerName!=null){
            request.setHeader(headerName, value);
        }
    }

    protected abstract String makeSignature(HttpRequest request) throws HttpException;


    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public Pattern getUriPattern() {
        return uriPattern;
    }

    public void setUriPattern(Pattern baseUriPattern) {
        this.uriPattern = baseUriPattern;
    }
}
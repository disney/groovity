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
package com.disney.groovity.servlet.error;

import java.io.IOException;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.disney.groovity.servlet.GroovityServletResponseWrapper;
/**
 * An error handler that forwards to an arbitrary servlet path using the RequestDispatcher; used to support custom
 * errorPage declarations in groovity scripts.
 *
 * @author Alex Vigdor
 */
public class ForwardErrorHandlerImpl implements GroovityErrorHandler {
	private static String alreadyHandledAttribute = "com.disney.groovy.view.error.ForwardErrorHandlerImpl.handled";
	private String errorPage;
	
	public ForwardErrorHandlerImpl(){
		
	}
	
	public ForwardErrorHandlerImpl(String errorPage){
		this.errorPage=errorPage;
	}

	public boolean handleError(HttpServletRequest request, HttpServletResponse response, GroovityError groovyError) throws ServletException, IOException {
		if(request.getAttribute(alreadyHandledAttribute) == null){
			request.setAttribute(alreadyHandledAttribute, alreadyHandledAttribute);
			int statusCode = response.getStatus();
			if(statusCode!=groovyError.getStatus()){
				response.setStatus(groovyError.getStatus());
			}
			if(groovyError.getStatus()==500 && groovyError.getCause()!=null && !(groovyError.getCause() instanceof StatusException)){
				groovyError.getLogger().log(Level.SEVERE, "500 error caused by exception", groovyError.getCause());
			}
			if(!response.isCommitted()){
				if(response instanceof GroovityServletResponseWrapper) {
					//unwrap for forwarding
					response = (HttpServletResponse) ((GroovityServletResponseWrapper)response).getResponse();
				}
				request.getRequestDispatcher(errorPage).forward(request, response);
				return true;
			}
		}
		return false;
	}

	public String getErrorPage() {
		return errorPage;
	}

	public void setErrorPage(String errorPage) {
		this.errorPage = errorPage;
	}

}

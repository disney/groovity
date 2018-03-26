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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 * Handler implementation that chains together a list of other handlers in order, breaking if and when one of them returns true
 *
 * @author Alex Vigdor
 */
public class GroovityErrorHandlerChain extends ArrayList<GroovityErrorHandler> implements GroovityErrorHandler {

	public GroovityErrorHandlerChain(){
		super();
	}
	public GroovityErrorHandlerChain(List<GroovityErrorHandler> errorHandlers) {
		super(errorHandlers);
	}

	public boolean handleError(HttpServletRequest request, HttpServletResponse response, GroovityError groovyError) throws ServletException, IOException {
		boolean handled = false;
		for(GroovityErrorHandler handler: this){
			if((handled = handler.handleError(request, response, groovyError)) == true){
				break;
			}
		}
		return handled;
	}

	public static GroovityErrorHandlerChain createDefault(){
		GroovityErrorHandlerChain chain = new GroovityErrorHandlerChain();
		chain.add(new RemoteDisconnectHandlerImpl());
		chain.add(new StatusCodeErrorHandlerImpl());
		chain.add(new LoggingErrorHandlerImpl());
		return chain;
	}
}

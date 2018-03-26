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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * API for groovity error handlers, which are chained together; a terminal handler breaks the chain by returning true
 *
 * @author Alex Vigdor
 */
public interface GroovityErrorHandler {
	/**
	 * handler may either provide the appropriate response entirely, or set status or request attributes 
	 * 
	 * returns true if this handler is terminal, false if the error should proceed along the chain.
	 * 
	 * @param request
	 * @param response
	 * @param groovityError
	 * @throws IOException 
	 * @throws ServletException 
	 */
	public boolean handleError(HttpServletRequest request, HttpServletResponse response, GroovityError groovityError) throws ServletException, IOException;
}

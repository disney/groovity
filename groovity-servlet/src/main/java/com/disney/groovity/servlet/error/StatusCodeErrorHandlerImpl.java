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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.HttpResponseException;

import com.disney.groovity.ArgsException;
/**
 * Error handler that maps exceptions by class to different HTTP status codes
 *
 * @author Alex Vigdor
 */
public class StatusCodeErrorHandlerImpl implements GroovityErrorHandler {
	private static String alreadyHandledAttribute = "com.disney.groovy.view.error.StatusCodeErrorHandlerImpl.handled";
	private Map<Class<? extends Throwable>,Integer> statusCodes;
	
	public StatusCodeErrorHandlerImpl(){
		this.statusCodes = new ConcurrentHashMap<Class<? extends Throwable>, Integer>();
		statusCodes.put(FileNotFoundException.class, 404);
		statusCodes.put(ArgsException.class, 400);
		statusCodes.put(TimeoutException.class, 504);
		statusCodes.put(SocketTimeoutException.class, 504);
		statusCodes.put(HttpResponseException.class, 502);
	}

	public boolean handleError(HttpServletRequest request, HttpServletResponse response, GroovityError groovyError) throws IOException {
		if(request.getAttribute(alreadyHandledAttribute)==null){
			request.setAttribute(alreadyHandledAttribute, alreadyHandledAttribute);
			Throwable cause = groovyError.getCause();
			Integer status = null;
			while(cause != null){
				if(cause.getClass().equals(StatusException.class)){
					//convert to sendError
					StatusException se = (StatusException) cause;
					groovyError.setReason(se.getReason());
					groovyError.setStatus(se.getStatus());
					if(!response.isCommitted()){
						response.sendError(se.getStatus(), se.getMessage());
					}
					else{
						response.setStatus(se.getStatus());
						return false;
					}
					return true;
				}
				status = statusCodes.get(cause.getClass());
				if(status!=null){
					groovyError.setStatus(status);
					if(!response.isCommitted()){
						response.sendError(status);
					}
					else{
						response.setStatus(status);
						return false;
					}
					return true;
				}
				cause = cause.getCause();
			}
			if(response.getStatus()<400){
				response.setStatus(500);
			}
		}
		return false;
	}

	public Map<Class<? extends Throwable>,Integer> getStatusCodes() {
		return statusCodes;
	}

	public void setStatusCodes(Map<Class<? extends Throwable>,Integer> statusCodes) {
		this.statusCodes.putAll(statusCodes);
	}

}

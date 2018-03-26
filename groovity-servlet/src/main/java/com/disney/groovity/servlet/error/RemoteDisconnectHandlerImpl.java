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
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 * Handler to minimize log noise when a client breaks the connection before it is complete; contains
 * built-in knowledge of the specific exception names used in Tomcat and Jetty to indicate this condition,
 * and may be configured with additional classnames to support other containers.  Strings are used to represent
 * class names so that container-specific implementations do not have to be accessible in the application classpath.
 *
 * @author Alex Vigdor
 */
public class RemoteDisconnectHandlerImpl implements GroovityErrorHandler {
	private static String DISCONNECT_HANDLED = "com.disney.groovy.view.error.RemoteDisconnectHandlerImpl.handled";
	private static final Logger logger = Logger.getLogger(RemoteDisconnectHandlerImpl.class.getName());
	private Collection<String> remoteDisconnectExceptions;
	
	public RemoteDisconnectHandlerImpl() {
		remoteDisconnectExceptions = new HashSet<>();
		//Built-in knowledge of Tomcat's client disconnect exception 
		remoteDisconnectExceptions.add("org.apache.catalina.connector.ClientAbortException");
		//Built-in knowledge of Jetty's client disconnect exception
		remoteDisconnectExceptions.add("org.eclipse.jetty.io.EofException");
	}

	@Override
	public boolean handleError(HttpServletRequest request, HttpServletResponse response, GroovityError groovityError) throws ServletException, IOException {
		if(request.getAttribute(DISCONNECT_HANDLED)==null){
			request.setAttribute(DISCONNECT_HANDLED, DISCONNECT_HANDLED);
			Throwable cause = groovityError.getCause();
			while(cause != null){
				if(remoteDisconnectExceptions.contains(cause.getClass().getName())){
					logger.warning("Remote client "+request.getRemoteAddr()+" disconnected while being served "+groovityError.getUri());
					return true;
				}
				cause = cause.getCause();
			}
		}
		return false;
	}

	public Collection<String> getRemoteDisconnectExceptions() {
		return remoteDisconnectExceptions;
	}

	public void setRemoteDisconnectExceptions(Collection<String> remoteDisconnectExceptions) {
		this.remoteDisconnectExceptions = remoteDisconnectExceptions;
	}

}

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

import java.util.logging.Logger;

import com.disney.groovity.model.ModelSkip;

/**
 * A Bean that describes an error that resulted from a thrown exception or call to sendError or response.sendError,
 * including contextual details
 * 
 * @author Alex Vigdor
 */
public class GroovityError {
	private Logger logger = Logger.getLogger(GroovityError.class.getName());
	private String uri=null;
	private Integer status = 500;
	private String reason=null;
	private String message=null;
	private Throwable cause = null;
	private Class scriptClass = null;
	private String scriptPath=null;
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Throwable getCause() {
		return cause;
	}
	public void setCause(Throwable cause) {
		this.cause = cause;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	public Class getScriptClass() {
		return scriptClass;
	}
	public void setScriptClass(Class scriptClass) {
		this.scriptClass = scriptClass;
	}
	public String getScriptPath() {
		return scriptPath;
	}
	public void setScriptPath(String scriptPath) {
		this.scriptPath = scriptPath;
	}
	@Override
	public String toString() {
		return "GroovityError [uri=" + uri + ", status=" + status + ", reason=" + reason + ", message=" + message
				+ ", cause=" + cause + ", scriptPath=" + scriptPath + "]";
	}
	@ModelSkip
	public Logger getLogger() {
		return logger;
	}
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
}

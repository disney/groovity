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

import java.util.Locale;

import org.apache.http.impl.EnglishReasonPhraseCatalog;

/**
 * Applications may throw a StatusException as a simple way to ad-hoc force a specific HTTP status code, error message and reason.
 * Providing this as an exception simplifies the process of stopping request processing if the code is nested too deep to return 
 * after performing response.sendError()
 *
 * @author Alex Vigdor
 */
public class StatusException extends Exception{
	private int status;
	private String reason;
	
	public StatusException(String message, int status){
		super(message);
		this.status = status;
		this.reason = EnglishReasonPhraseCatalog.INSTANCE.getReason(status, Locale.getDefault());
	}
	
	public StatusException(String message, int status, String reason){
		super(message);
		this.status = status;
		this.reason=reason;
	}
	
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	
}

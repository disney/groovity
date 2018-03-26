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
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Condensed error logger
 * 
 * You can prevent logging of errors by status code using the system property 
 * "groovity.ignoreStatusCodes"
 *
 * @author Alex Vigdor
 */
public class LoggingErrorHandlerImpl implements GroovityErrorHandler {
	private Collection<Integer> filterCodes;
	
	public LoggingErrorHandlerImpl(){
		filterCodes = new ConcurrentSkipListSet<>();
		String codes = System.getProperty("groovity.ignoreStatusCodes");
		if(codes!=null){
			String[] cs = codes.split("\\D+");
			Logger.getLogger(LoggingErrorHandlerImpl.class.getName()).info("Configuring log to ignore servlet response codes "+Arrays.toString(cs));
			for(String c: cs){
				filterCodes.add(Integer.valueOf(c));
			}
		}
	}

	public boolean handleError(HttpServletRequest request, HttpServletResponse response, GroovityError groovyError) throws ServletException, IOException {
		if(filterCodes.contains(response.getStatus())){
			if(response.isCommitted()){
				groovyError.getLogger().warning("WARNING: error "+groovyError+" occurred after response was committed, an improper status code or incomplete response may have been returned");
				return true;
			}
			return false;
		}
		String refString = "";
		String referer = request.getHeader("Referer");
		if(referer!=null && !referer.trim().equals("")){
			refString = "\n ---> referer "+referer;
		}
		String client = request.getHeader("X-Forwarded-For");
		if(client == null || client.trim().equals("")){
			client = request.getRemoteAddr();
		}
		String ua = request.getHeader("User-Agent");
		String clientString = "\n ---> "+client+" : "+ua;
		StackTraceElement[] stackTrace;
		if(groovyError.getCause()!=null){
			stackTrace = groovyError.getCause().getStackTrace();
		}
		else{
			stackTrace = Thread.currentThread().getStackTrace();
		}
		String trace = null;
		int traceSpecificity = 0;
		for(int i=0;i<stackTrace.length;i++){
			StackTraceElement elem = stackTrace[i];
			if(elem!=null && elem.getFileName() != null && !elem.getClassName().startsWith("com.disney.groovity.servlet.error.")){
				//System.out.println("Checking "+elem.getFileName()+" for "+scriptName);
				boolean isSource = elem.getFileName().endsWith(".grvt");
				boolean isFramework = elem.getClassName().startsWith("com.disney.groovity.");
				int specificity = isSource ? 3 : isFramework ? 2 : 1;
				if(elem.getLineNumber()==-1){
					specificity--;
				}
				if(trace==null || specificity > traceSpecificity){
					trace = elem.getFileName()+":"+elem.getLineNumber();
					traceSpecificity = specificity;
				}
				if(specificity==3){
					break;
				}
			}
		}
		String reason = groovyError.getReason();
		if(reason==null){
			reason="";
		}
		String message = groovyError.getMessage();
		if(message==null){
			message="";
		}
		groovyError.getLogger().severe(groovyError.getStatus()+" Error in source file "+groovyError.getScriptPath()+" for URI "+groovyError.getUri()+"\n ---> "+reason+": "+message+" @ "+trace+clientString+refString);
		if(response.isCommitted()){
			groovyError.getLogger().warning("previous error occurred after response was committed, an improper status code or incomplete response may have been returned");
			return true;
		}
		return false;
	}

	public Collection<Integer> getFilterCodes() {
		return filterCodes;
	}

	public void setFilterCodes(Collection<Integer> filterCodes) {
		this.filterCodes = filterCodes;
	}

}

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
package com.disney.groovity.tags;

import groovy.lang.Closure;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.disney.groovity.Taggable;
import com.disney.groovity.compile.GroovityClassLoader;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.stats.GroovityStatistics;
import com.disney.groovity.util.ScriptHelper;
/**
 * Log a message at level debug, info, warn or error, along with Throwables
 * <p>
 * log( <ul>	
 *	<li><i>debug</i>: 
 *	a debug level message,</li>	
 *	<li><i>info</i>: 
 *	an info level message,</li>
 *	<li><i>warn</i>: 
 *	a warn level message,</li>	
 *	<li><i>error</i>: 
 *	an error level message,</li>
 *  <li><i>thrown</i>: 
 *	A Throwable to log along with a message,</li>
 *	</ul>{});
 *	
 *	<p>Sample
 *	<pre>
 *	log(info:&quot;Current time is ${System.currentTimeMillis()}&quot;)
 *	</pre>	
 * 
 * @author Alex Vigdor
 */ 
@Tag(
		info = "Log a message at level debug, info, warn or error, along with Throwables",
		sample="log(info:\"Current time is ${System.currentTimeMillis()}\")",
		attrs = { 
			@Attr(
					name = "debug", 
					info="a debug level message",
					required = false
			),
			@Attr(
					name = "info", 
					info="an info level message",
					required = false
			),
			@Attr(
					name = "warn", 
					info="a warn level message",
					required = false
			),
			@Attr(
					name = "error", 
					info="an error level message",
					required = false
			),
			@Attr(
					name = "thrown", 
					info="A Throwable to log along with a message",
					required = false
			) 			
		} 
	)
public class Log implements Taggable {

	@SuppressWarnings("rawtypes")
	@Override
	public Object tag(Map attributes, Closure body) throws Exception {
		ScriptHelper helper = getScriptHelper(body);
		GroovityClassLoader classLoader =helper.getClassLoader();
		Logger logger = classLoader.getLogger();
		Throwable thrown = resolve(attributes,"thrown",Throwable.class);
		doLog(attributes,logger,Level.FINE,"debug",thrown);
		doLog(attributes,logger,Level.INFO,"info",thrown);
		doLog(attributes,logger,Level.WARNING,"warn",thrown);
		doLog(attributes,logger,Level.SEVERE,"error",thrown);
		return null;
	}

	@SuppressWarnings("rawtypes")
	protected void doLog(Map attributes, Logger logger, Level level, String att, Throwable thrown){
		if(logger.isLoggable(level)){
			String msg = resolve(attributes,att,String.class);
			if(msg!=null){
				LogRecord record = new LogRecord(level, msg);
				record.setThrown(thrown);
				Object key = GroovityStatistics.currentStackKey();
				if(key!=null) {
					String s = key.toString();
					int dot = s.indexOf(".");
					if(dot>0) {
						record.setSourceClassName(s.substring(0,dot));
						int parn = s.indexOf("(",dot);
						if(parn==-1) {
							parn = s.length();
						}
						record.setSourceMethodName(s.substring(dot+1,parn));
					}
				}
				logger.log(record);
			}
		}
	}

}

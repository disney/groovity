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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.util.AsyncChannel;

import groovy.lang.Closure;
/**
 * Offer a message to acceptor queues for asynchronous processing
 * <p>
 * param( <ul>	
 *	<li><i>value</i>: 
 *	used to specify a message (or error) value; null is only allowed as a termination message indicating a sender closing a channel,</li>	
 *	<li><i>channel</i>: 
 *	The name of a registered channel topic to distribute the message to, or a direct reference to an AsyncChannel opened by an accept() call,</li>	
 *	<li><i>timeout</i>: 
 *	number of seconds to try and enqueue the message to acceptors,</li>
 *  </ul>{
 *	<blockquote>// optional alternative to value attribute, the streaming output or return value of the body closure may be used to produce the message value</blockquote>
 * 	});
 * 
 *	<p>Sample
 *	<pre>
 *		offer(channel:'userLogin',value:authUser)
 *	</pre>	
 * @author Alex Vigdor
 */
@Tag(
		info = "Offer a message to acceptor queues for asynchronous processing",
		body = "optional alternative to value attribute, the streaming output or return value of the body closure may be used to produce the message value",
		sample="offer(channel:'userLogin',value:authUser)",
		attrs = { 
				@Attr(
						name = "value", 
						info="used to specify a message (or error) value; null is only allowed as a termination message indicating a sender closing a channel",
						required = false
				),
				@Attr(
						name = "channel", 
						info="The name of a registered channel topic to distribute the message to, or a direct reference to an AsyncChannel opened by an accept() call",
						required = false
				),
				@Attr(
						name = "timeout", 
						info="number of seconds to try and enqueue the message to acceptors",
						required = false
				)
			} 
	)
public class Offer implements Taggable{

	@SuppressWarnings("rawtypes")
	@Override
	public Object tag(Map attributes, Closure body) throws Exception {
		Object channel = resolve(attributes,"channel");
		if(channel==null){
			throw new RuntimeException("offer() requires channel");
		}
		Object value = resolve(attributes,"value");
		if(value==null){
			value = body;
		}
		final Integer timeoutSeconds = resolve(attributes, "timeout", Integer.class);
		if(channel instanceof AsyncChannel){
			((AsyncChannel)channel).offer(value, timeoutSeconds!=null ? timeoutSeconds : -1, TimeUnit.SECONDS);
		}
		else{
			AsyncChannel.offer(channel,value, timeoutSeconds!=null ? timeoutSeconds : -1, TimeUnit.SECONDS);
		}
		return null;
	}

}

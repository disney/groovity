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

import java.io.CharArrayWriter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.tags.Await.AwaitContext;
import com.disney.groovity.util.AsyncChannel;
import com.disney.groovity.util.DeadlockFreeExecutor;
import com.disney.groovity.util.InterruptFactory;
import com.disney.groovity.util.AsyncChannel.Policy;
import com.disney.groovity.util.ScriptHelper;

import groovy.lang.Binding;
import groovy.lang.Closure;
/**
 * Create a message accepting queue that may optionally subscribe to a named channel
 * <p>
 * param( <ul>	
 *	<li><i>var</i>: 
 *	the name of the variable to bind the AsyncChannel object that can be used to close this acceptor, or check its state,</li>	
 *	<li><i>channel</i>: 
 *	Optionally, the name of a registered channel topic to receive messages from,</li>	
 *	<li><i>completed</i>: 
 *	optional callback to execute when the channel is completed after being closed or halted, it is passed the last result of the handler call and may investigate any 'error', and should return the final result for the channel,</li>
 *	<li><i>q</i>: 
 *	Optionally, a maximum queue size for this acceptor, defaults to unbounded. Behavior with a full queue is determined by policy</li>
 *	<li><i>policy</i>: 
 *	what to do with new messages when queue is full, one of (drop|evict|block), defaults to block (backpressure),</li>
 *  </ul>{
 *	<blockquote>// the closure to execute to process each received message on another thread; the incoming variable is bound to 'it'. The closure may call halt() to stop processing and trigger completion</blockquote>
 * 	});
 * 
 * <p><b>returns</b> an AsyncChannel that can be used that can be used to close and unregister this acceptor, or check its state
 *	
 *	<p>Sample
 *	<pre>
 *	accept(channel:'weather',q:1,policy:'drop'){ load('/client/weather').update(it) }
 *	</pre>	
 * @author Alex Vigdor
 */
@Tag(
		info = "Create a message accepting queue that may optionally subscribe to a named channel",
		body = "the closure to execute to process each received message on another thread; "+
				"the incoming variable is bound to 'it'. "+
				"The closure may call halt() to stop processing and trigger completion ",
		sample="accept(channel:'weather',q:1,policy:'drop'){ load('/client/weather').update(it) }",
		returns="an AsyncChannel that can be used that can be used to close and unregister this acceptor, or check its state",
		attrs = { 
				@Attr(
						name = "var", 
						info="the name of the variable to bind the AsyncChannel object that can be used to close this acceptor, or check its state",
						required = false
				),
				@Attr(
						name = "channel", 
						info="Optionally, the name of a registered channel topic to receive messages from",
						required = false
				),
				@Attr(
						name = "completed", 
						info="optional callback to execute when the channel is completed after being closed or halted, "
								+"it is passed the last result of the handler call and may investigate any 'error', "
								+"and should return the final result for the channel",
						required = false
				),
				@Attr(
						name = "q",
						info="Optionally, a maximum queue size for this acceptor, defaults to unbounded. Behavior with a full queue is determined by policy",
						required = false
				),	
				@Attr(
						name = "policy",
						info="what to do with new messages when queue is full, one of (drop|evict|block), defaults to block (backpressure)",
						required = false
				)
			} 
	)
public class Accept implements Taggable, GroovityConstants {
	DeadlockFreeExecutor sharedThreadPool;
	InterruptFactory interruptFactory;

	public void setGroovity(Groovity groovity) {
		this.interruptFactory = groovity.getInterruptFactory();
	}
	
	public void init(){
		sharedThreadPool = new DeadlockFreeExecutor(interruptFactory);
	}

	public void destroy(){
		sharedThreadPool.shutdown();
		try {
			if(!sharedThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
				sharedThreadPool.shutdownNow();
				sharedThreadPool.awaitTermination(60, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			sharedThreadPool.shutdownNow();
		}
	}
	
	@SuppressWarnings({"rawtypes","unchecked"})
	@Override
	public Object tag(Map attributes, Closure body) throws Exception {
		//channel may be null, which will create an anonymous channel that can only be offered to via reference
		Object channel = resolve(attributes,"channel");
		Policy policy = Policy.block;
		String policyStr = resolve(attributes, "policy",String.class);
		if(policyStr!=null){
			policy = Policy.valueOf(policyStr);
		}
		int qSize = -1;
		Object qDef = resolve(attributes, "q");
		if(qDef!=null){
			if(qDef instanceof Number){
				qSize = ((Number)qDef).intValue();
			}
			else{
				qSize = Integer.valueOf(qDef.toString());
			}
		}
		Closure completed = resolve(attributes,"completed",Closure.class);
		final ScriptHelper scriptHelper = getScriptHelper(body);
		final Binding binding = scriptHelper.getBinding();
		final Map variables = binding.getVariables();
		final Map asyncVariables = Async.asyncCopy(variables);
		final AwaitContext asyncContext = AwaitContext.get(variables);
		final CharArrayWriter out = new CharArrayWriter();
		asyncVariables.put(OUT,out);
		if(asyncContext!=null){
			//signal a boundary for sequencing async output
			asyncContext.signalAsync(variables,out);
		}
		Binding asyncBinding = new Binding(asyncVariables);
		Object owner;
		if(body.getThisObject() instanceof Class){
			//if the parent script is not available it is a special case (static) and we may share the context
			owner = body.getOwner();
		}
		else{
			//in normal contexts create a safe copy of the script
			Binding oldThreadBinding = ScriptHelper.THREAD_BINDING.get();
			ScriptHelper.THREAD_BINDING.set(asyncBinding);
			try{
				owner = scriptHelper.load(scriptHelper.getClassLoader().getScriptName());
			}
			finally{
				if(oldThreadBinding==null) {
					ScriptHelper.THREAD_BINDING.remove();
				}
				else {
					ScriptHelper.THREAD_BINDING.set(oldThreadBinding);
				}
			}
		}
		AsyncChannel asyncChan = AsyncChannel.open(sharedThreadPool, channel, qSize, policy, body, completed, owner, asyncBinding);
		String var = resolve(attributes, "var", String.class);
		if(var!=null && var.length()>0){
			bind(body,var,asyncChan);
		}
		if(asyncContext!=null){
			asyncContext.add(asyncChan.getFuture());
		}
		return asyncChan;
	}


}

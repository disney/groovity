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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
import com.disney.groovity.stats.GroovityStatistics;
import com.disney.groovity.stats.GroovityStatistics.Execution;
import com.disney.groovity.tags.Await.AwaitContext;
import com.disney.groovity.util.DeadlockFreeExecutor;
import com.disney.groovity.util.InterruptFactory;
import com.disney.groovity.util.ScriptHelper;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.Script;
/**
 * Submit a closure for execution on another thread with a snapshot of the calling binding
 * <p>
 * param( <ul>	
 *	<li><i>var</i>: 
 *	the name of the variable to bind the future for the body,</li>	
 *	<li><i>timeout</i>: 
 *	number of seconds to allow the body to run before throwing an exception,</li>	
 *	<li><i>pool</i>: 
 *	max number of threads to occupy - allocates an isolated pool unless running in a isolated await,</li>
 *	</ul>{
 *	<blockquote>// the closure to execute in another thread</blockquote>
 * 	});
 * 
 * <p><b>returns</b> a Future that will yield the return value of the body on get()
 *	
 *	<p>Sample
 *	<pre>
 *	async({ load('/lib/data').updateModel() })
 *	</pre>	
 * @author Alex Vigdor
 */
@Tag(
		info = "Submit a closure for execution on another thread with a snapshot of the calling binding",
		body = "the closure to execute in another thread",
		sample="async({ load('/lib/data').updateModel() })",
		returns="a Future that will yield the return value of the body on get()",
		attrs = { 
				@Attr(
						name = GroovityConstants.VAR, 
						info="the name of the variable to bind the future for the body",
						required = false
				),
				@Attr(
						name = GroovityConstants.TIMEOUT, 
						info="number of seconds to allow the body to run before throwing an exception",
						required = false
				),
				@Attr(
						name = GroovityConstants.POOL,
						info="max number of threads to occupy - allocates an isolated pool unless running in a isolated await",
						required = false
				)	
			} 
	)
public class Async implements Taggable, GroovityConstants {
	final static Logger log = Logger.getLogger(Async.class.getName());
	final static String EXECUTOR_BINDING = "..Async$Executor";//use a non-internal prefix to allow copying to async bindings
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
	
	public final DeadlockFreeExecutor getExecutor(Map<String, Object> variables){
		DeadlockFreeExecutor dfe = (DeadlockFreeExecutor) variables.get(EXECUTOR_BINDING);
		if(dfe!=null){
			return dfe;
		}
		return sharedThreadPool;
	}
	public static final Map<String,Object> asyncCopy(final Map<String,Object> variables){
		final HashMap<String,Object> asyncVariables = new HashMap<>(variables.size()*2);
		//we will copy the binding, e.g. to capture loop variables,
		//but NOT out or internal groovity state
		for(Entry<String, Object> entry:variables.entrySet()){
			final String k = entry.getKey();
			if(k.startsWith(INTERNAL_BINDING_PREFIX)){
				continue;
			}
			asyncVariables.put(k,  entry.getValue());
		}
		return asyncVariables;
	}
	@SuppressWarnings({"rawtypes","unchecked"})
	@Override
	public Object tag(Map attributes, final Closure body) throws Exception {
		final Integer timeoutSeconds = resolve(attributes, TIMEOUT, Integer.class);
		final ScriptHelper scriptHelper = getScriptHelper(body);
		final Binding binding = scriptHelper.getBinding();
		final Map variables = binding.getVariables();
		final AwaitContext asyncContext = AwaitContext.get(variables);
		DeadlockFreeExecutor createdThreadPool = null;
		//make a copy of current binding for async
		final Map asyncVariables = asyncCopy(variables);
		if(asyncContext==null || !asyncVariables.containsKey(Async.EXECUTOR_BINDING)){
			Integer numThreads = resolve(attributes,POOL,Integer.class);
			if(numThreads!=null){
				createdThreadPool = new DeadlockFreeExecutor(interruptFactory, numThreads);
				asyncVariables.put(EXECUTOR_BINDING, createdThreadPool);
			}
		}
		final DeadlockFreeExecutor asyncPool = createdThreadPool!=null ? createdThreadPool: ((asyncContext !=null) ? getExecutor(asyncVariables) : sharedThreadPool);
		final boolean shutdownPool = createdThreadPool!=null;
		final CharArrayWriter out = new CharArrayWriter();
		asyncVariables.put(OUT,out);
		if(asyncContext!=null){
			//signal a boundary for sequencing async output
			asyncContext.signalAsync(variables,out);
		}
		final Execution parentStack = asyncContext!=null?asyncContext.getWaitingExecution():null;
		String scriptPath = scriptHelper.getClassLoader().getScriptName();
		final long timeoutTime = timeoutSeconds==null?-1:System.currentTimeMillis()+(timeoutSeconds*1000);
		final Callable<Object> bodyRunner = new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				Binding asyncBinding = new Binding(asyncVariables);
				Binding oldThreadBinding = ScriptHelper.THREAD_BINDING.get();
				ScriptHelper.THREAD_BINDING.set(asyncBinding);
				final Execution restoreStack = parentStack !=null ? GroovityStatistics.registerStack(parentStack) : null;
				try{
					Closure asyncBody;
					if(body.getThisObject() instanceof Class){
						//if the parent script is not available it is a special case (static) and we may share the context
						asyncBody=body;
						asyncBody.setDelegate(asyncBinding);
						asyncBody.setResolveStrategy(Closure.DELEGATE_FIRST);
					}
					else{
						Script asyncInstance = scriptHelper.load(scriptPath);
						asyncBody = body.rehydrate(asyncInstance, asyncInstance, asyncInstance);
					}
					if(timeoutTime>0){
						//use an interrupt to enforce the timeout
						long startTime = System.currentTimeMillis();
						if(startTime>timeoutTime){
							throw new InterruptedException();
						}
						final ScheduledFuture<?> interrupt = interruptFactory.scheduleInterrupt(timeoutTime-startTime);
						try{
							return asyncBody.call();
						}
						finally{
							interrupt.cancel(true);
						}
					}
					return asyncBody.call();
				}
				catch(Throwable e){
					if(asyncContext==null){
						//with no known waiters this exception could get lost, let's log it
						log.log(Level.SEVERE, "Error in async", e);
					}
					throw e;
				}
				finally{
					if(oldThreadBinding==null){
						ScriptHelper.THREAD_BINDING.remove();
					}
					else{
						ScriptHelper.THREAD_BINDING.set(oldThreadBinding);
					}
					if(parentStack!=null){
						GroovityStatistics.registerStack(restoreStack);
					}
					if(shutdownPool){
						sharedThreadPool.submit(new Runnable() {
							@Override
							public void run() {
								asyncPool.shutdown();
								try {
									if(!asyncPool.awaitTermination(60, TimeUnit.SECONDS)) {
										asyncPool.shutdownNow();
										asyncPool.awaitTermination(60, TimeUnit.SECONDS);
									}
								} catch (InterruptedException e) {
									asyncPool.shutdownNow();
								}
							}
						});
					}
				}
			}
		};
		
		Future<Object> future = asyncPool.submit(bodyRunner); 
		if(asyncContext!=null){
			asyncContext.add(future);
		}
		String var = resolve(attributes, VAR, String.class);
		if(var!=null && var.length()>0){
			variables.put(var, future);
		}
		return future;
	}
}

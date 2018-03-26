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
package com.disney.groovity.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A threadpool implementation that supports recursive task spawning without
 * deadlock using a lazy inlining mechanism.  While not as outright fast as a ForkJoinPool for non-blocking computational tasks,
 * this is meant to offer a general-purpose threadpool that offers benefits for potentially blocking and/or recursive general-purpose workloads 
 *
 * @author Alex Vigdor
 */
public class DeadlockFreeExecutor extends ThreadPoolExecutor {
	private final InterruptFactory interruptFactory;
	
	public DeadlockFreeExecutor(InterruptFactory interruptFactory) {
		this(interruptFactory, Runtime.getRuntime().availableProcessors()*8);
	}
	
	public DeadlockFreeExecutor(InterruptFactory interruptFactory, int maxThreads) {
		this(interruptFactory, maxThreads,maxThreads*100);
	}

	public DeadlockFreeExecutor(InterruptFactory interruptFactory, int maxThreads, int maxBacklog) {
		super((int)Math.min(Runtime.getRuntime().availableProcessors(),maxThreads),
				maxThreads,
				1l,
				TimeUnit.MINUTES,
				new LinkedBlockingQueue<Runnable>(maxBacklog),
				new ThreadFactory() {	
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setName("DeadlockFree"+t.getName());
						return t;
					}
				},
				new ThreadPoolExecutor.CallerRunsPolicy());
		this.interruptFactory=interruptFactory;
	}

	protected <T> RunnableFuture<T>	newTaskFor(Callable<T> callable){
		return new DeadlockFreeFuture<T>(callable);
	}
	
	protected <T> RunnableFuture<T>	newTaskFor(Runnable runnable, T value){
		return new DeadlockFreeFuture<T>(runnable,value);
	}
	
	public final class DeadlockFreeFuture<V> extends FutureTask<V> implements RunnableFuture<V>{
		private volatile boolean running = false;
		
		public DeadlockFreeFuture(final Callable<V> callable) {
			super(callable);
		}
		
		public DeadlockFreeFuture(final Runnable runnable, final V result) {
			super(runnable,result);
		}
		
		public V get() throws InterruptedException, ExecutionException{
			//prevent deadlocks by running on this thread if it hasn't already started
			if(!running && !getQueue().isEmpty()){
				run();
			}
			return super.get();
		}
		
		public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException{
			//prevent deadlocks by running on this thread if it hasn't already started
			//use an interrupt to enforce timeout
			if(!running && !getQueue().isEmpty()){
				ScheduledFuture<?> interrupt = interruptFactory.scheduleInterrupt(timeout, unit);
				try{
					run();
				}
				finally{
					interrupt.cancel(true);
				}
			}
			return super.get(timeout,unit);
		}
		
		public void run(){
			running = true;
			super.run();
		}
	}
	
}

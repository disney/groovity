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

import com.disney.groovity.GroovityConstants;
import groovy.lang.Binding;
import groovy.lang.Closure;

import java.io.Closeable;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an asynchronous message queue
 *
 * @author Alex Vigdor
 */
public class AsyncChannel implements Closeable, GroovityConstants{
	public static enum Policy{ drop, evict, block }
	public static final ConcurrentHashMap<Object, Collection<AsyncChannel>> ASYNC_CHANNEL_ROUTING = new ConcurrentHashMap<>();
	private static final Logger log = Logger.getLogger(AsyncChannel.class.getName());
	final Object key;
	final public LinkedBlockingQueue<AsyncMessage> messageQueue;
	final public Policy policy;
	final AtomicBoolean closed = new AtomicBoolean(false);
	final AtomicBoolean halted = new AtomicBoolean(false);
	@SuppressWarnings("rawtypes")
	final Closure handler;
	@SuppressWarnings("rawtypes")
	final Closure closer;
	final Binding binding;
	@SuppressWarnings("rawtypes")
	final Map variables;
	final AtomicBoolean dirty = new AtomicBoolean(false);
	final ReentrantLock dirtyLock = new ReentrantLock();
	final DeadlockFreeExecutor asyncChannelExecutor;
	public final AsyncChannelManager asyncChannelManager;
	public static final AsyncChannelAnonymousManager asyncChannelAnonymousManager =
			new AsyncChannelAnonymousManager();

	final CompletableFuture<Object> completionFuture = new CompletableFuture<Object>(){
		public boolean cancel(boolean mayInterruptIfRunning){
			if(!closed.get()){
				errorCondition(new InterruptedException(), true);
			}
			return super.cancel(mayInterruptIfRunning);
		}
		public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException{
			try {
				return super.get(timeout, unit);
			} catch (InterruptedException | TimeoutException e) {
				errorCondition(e, true);
				throw e;
			}
		}
	};
	Thread runningThread;
	Object lastResult;
	Throwable error;
	//we'll keep the Runnable private so nobody can call externally
	final Runnable processor = new Runnable(){
		
		/**
		 * To be called by messaging threads, do the work of invoking the handler on messages in the queue,
		 * does NOT wait for work, just handles already queued work and closes the acceptor if either the 
		 * queue contains a shutdown signal OR the handler returns a falsy value
		 * 
		 * @throws Exception
		 */
		@SuppressWarnings("unchecked")
		@Override
		public synchronized void run() {
			runningThread = Thread.currentThread();
			Binding oldThreadBinding = ScriptHelper.THREAD_BINDING.get();
			ScriptHelper.THREAD_BINDING.set(binding);
			try {
				Object oldIt = variables.get("it");
				try{
					AsyncMessage message = null;
					while(!halted.get() && (message = poll())!=null){
						try{
							long startTime = System.currentTimeMillis();
							variables.put("it",message.payload);
							lastResult = handler.call(message.payload);
							long endTime = System.currentTimeMillis();
							long processingTime = endTime-startTime;
							if(asyncChannelManager!=null){
								asyncChannelManager.updateMBeanForMessageProcessed();
								asyncChannelManager.updateMBeanForMessageProcessingTime(processingTime);
							}
						}
						finally{
							message.consumed();
						}
					}
				}
				catch(Exception e){
					//on unhandled error we will shut the channel and clear the queue to avoid resource leakage
					errorCondition(e, false);
				}
				finally{
					variables.put("it",oldIt);
				}
				if(closed.get()){
					if(closer!=null){
						//give handler the shutdown signal, a chance to clean resources, flush buffers, etc.
						lastResult = closer.call(lastResult);
					}
					if(error!=null){
						completionFuture.completeExceptionally(error);
					}
					else{
						completionFuture.complete(lastResult);
					}
					//deregister if the MBean is not null
					if(asyncChannelManager != null) {
						asyncChannelManager.deRegisterChannelMBean();
					}
					else{
						//update anonymous MBean tracking for anonymous channel closed
						if(log.isLoggable(Level.FINEST)){
							log.log(Level.INFO,"anonymous channel closed");
						}
						asyncChannelAnonymousManager.incrementNumberOfTotalChannelsClose();
					}
				}
			} catch (Exception e) {
				completionFuture.completeExceptionally(e);
				log.log(Level.SEVERE, "Error processing accept queue", e);
			}
			finally{
				runningThread=null;
				if(oldThreadBinding==null){
					ScriptHelper.THREAD_BINDING.remove();
				}
				else{
					ScriptHelper.THREAD_BINDING.set(oldThreadBinding);
				}
			}
		}
		
	};

	/**
	 * Returns a future that can be used to block and wait for this channel to close
	 * @return
	 */
	public Future<Object> getFuture(){
		return completionFuture;
	}
	
	public Throwable getError(){
		return error;
	}
	
	public Object getLastResult(){
		return lastResult;
	}
	
	@SuppressWarnings("rawtypes")
	private AsyncChannel(DeadlockFreeExecutor asyncChannelExecutor, Object key, int queueSize, Policy policy, Closure handler, Closure closer, Object owner, Binding binding){
		this.asyncChannelExecutor = asyncChannelExecutor;
		this.messageQueue = new LinkedBlockingQueue<>(queueSize>0?queueSize:Integer.MAX_VALUE);
		this.policy=policy;
		this.key=key;
		this.handler = handler.rehydrate(this,owner, owner);
		this.closer=closer !=null ? closer.rehydrate(this,owner, owner) : null;
		this.binding=binding;
		this.variables = binding.getVariables();
		if(key!=null) {
			this.asyncChannelManager = new AsyncChannelManager(this);
			//register MBean
			asyncChannelManager.registerChannelMBean();
		}
		else{
			this.asyncChannelManager = null;
		}
	}

	public Object getKey(){
		return key;
	}
	
	@SuppressWarnings("rawtypes")
	public static AsyncChannel open(DeadlockFreeExecutor asyncChannelExecutor, Object key, int queueSize, Policy policy, Closure handler, Closure closer, Object owner, Binding binding){

		AsyncChannel channel = new AsyncChannel(asyncChannelExecutor, key, queueSize, policy, handler, closer, owner, binding);
		if(key!=null){
			Collection<AsyncChannel> channels = ASYNC_CHANNEL_ROUTING.get(key);
			if(channels==null) {
				channels = ConcurrentHashMap.newKeySet();
				Collection<AsyncChannel> oldChannels = ASYNC_CHANNEL_ROUTING.putIfAbsent(key, channels);
				if(oldChannels!=null) {
					channels = oldChannels;
				}
			}
			channels.add(channel);
		}else{
			//update AnonymousChannel MBean when anonymous channel is opened
			asyncChannelAnonymousManager.incrementNumberOfTotalChannelsOpened();
		}
		return channel;
	}

	@Override
	public void close() {
		close(true,false);
	}
	
	public void close(@SuppressWarnings("rawtypes") Closure closeAfter){
		try{
			closeAfter.call();
			close();
		}
		catch(Throwable th){
			errorCondition(th, true);
		}
	}
	
	private void close(boolean markDirty, boolean clearQueue) {
		if(closed.compareAndSet(false, true)){
			if(key!=null){
				Collection<AsyncChannel> as = ASYNC_CHANNEL_ROUTING.get(key);
				if(as!=null){
					as.remove(this);
					if(as.isEmpty()){
						ASYNC_CHANNEL_ROUTING.remove(key);
					}
				}
			}
			if(clearQueue){
				AsyncMessage m;
				while((m = messageQueue.poll())!=null){
					m.dropped();
				}
			}
			if(markDirty){
				markDirty();
			}
		}
	}
	/**
	 * Close this channel and discard any remaining queued tasks.  Typically invoked within an accept handler if it 
	 * decides it no longer needs to consume any messages.  
	 */
	public void halt(){
		if(halted.compareAndSet(false, true)){
			close(true,true);
		}
	}
	
	private void markDirty(){
		//lock to force consistency of dirty state
		boolean submit = false;
		dirtyLock.lock();
		try{
			submit = dirty.compareAndSet(false, true);
		}
		finally{
			dirtyLock.unlock();
		}
		if(submit){
			//schedule this acceptor for processing
			asyncChannelExecutor.submit(processor);
		}
	}
	//Poll for the head of the queue, and if the queue is clear set dirty to false
	private AsyncMessage poll(){
		dirtyLock.lock();
		try{
			AsyncMessage o = messageQueue.poll();
			if(o==null){
				dirty.compareAndSet(true, false);
			}
			return o;
		}
		finally{
			dirtyLock.unlock();
		}
	}
	
	public boolean isClosed(){
		return closed.get();
	}
	
	private void errorCondition(Throwable error, boolean markDirty){
		this.error=error;
		log.log(Level.SEVERE, "Error in async channel", error);
		this.close(markDirty,true);
		if(markDirty){
			Thread rt = runningThread;
			if(rt!=null){
				rt.interrupt();
			}
		}
	}
	private static Object unwrapMessage(Object message){
		if(message instanceof Closure){
			try{
				@SuppressWarnings("rawtypes")
				Closure c = (Closure) message;
				ScriptHelper helper = ScriptHelper.get(c);
				Binding bodyBinding = helper.getBinding();
				Object oldOut = bodyBinding.getVariables().get(OUT);
				StringWriter writer = new StringWriter();
				bodyBinding.setVariable(OUT, writer);
				try{
					message = c.call();
				}
				finally{
					if(oldOut!=null){
						bodyBinding.setVariable(OUT, oldOut);
					}
					else{
						bodyBinding.getVariables().remove(OUT);
					}
				}
				String s = writer.toString();
				if(s.length()>0){
					message = s;
				}
			}
			catch(Exception e){
				message = e;
			}
		}
		return message;
	}
	
	
	/**
	 * Enqueue a message for an acceptor, will only block if the queue is full and the policy allows it
	 * 
	 * @param message
	 * @param timeout how long to wait for a spot in the queue before throwing an exception
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 */
	public boolean offer(Object message, long timeout, TimeUnit unit) throws InterruptedException{
		message = unwrapMessage(message);
		AsyncMessage am = new AsyncMessage(message, 1, timeout, unit);
		return offerAsync(am);
	}
	private boolean offerAsync(AsyncMessage message) throws InterruptedException{
		if(!closed.get()){
			Object payload = unwrapMessage(message.payload);
			if(payload==null){
				close(true,false);
				return true;
			}
			if(payload instanceof Throwable){
				//producer sent an exception to the channel, let's close it
				errorCondition((Throwable) payload,true);
				return true;
			}
			if(!messageQueue.offer(message)){
				if(policy==Policy.drop){
					message.dropped();
					//update message dropped on MBean
					updateMBeanForMessageDropped();
					return false;
				}
				if(policy==Policy.evict){
					while(!messageQueue.offer(message)){
						AsyncMessage discard = messageQueue.poll();
						if(discard!=null){
							discard.dropped();
							//update message evicted on MBean
							updateMBeanForMessageEvicted();
						}
					}
				}
				else{
					//we've come to blocking
					try{
						if(message.timeout>0) {
							if(!messageQueue.offer(message, message.timeout, message.unit)) {
								throw new InterruptedException();
							}
						}
						else{
							messageQueue.put(message);
						}
					}
					catch (InterruptedException e) {
						message.dropped();
						//update message dropped on MBean
						updateMBeanForMessageDropped();
						//failing to offer on blocking channel is a fatal error, let's close the channel
						errorCondition(e, true);
						throw e;
					}
				}
			}
			if(asyncChannelManager!=null) {
				asyncChannelManager.updateMBeanForMessageEnqueued();
			}
			markDirty();
			return true;
		}
		//update message dropped on MBean
		updateMBeanForMessageDropped();
		message.dropped();
		return false;
	}

	//helper function to update MBean Object when message is dropped
	private void updateMBeanForMessageDropped(){
		if(asyncChannelManager!=null) {
			asyncChannelManager.updateMBeanForMessagesDropped();
		}
	}

	//helper function to update MBean Object when message is evicted
	private void updateMBeanForMessageEvicted(){
		if(asyncChannelManager!=null) {
			asyncChannelManager.updateMBeanForMessagesEvicted();
		}
	}

	public boolean offer(Object message) throws InterruptedException{
		return offer(message,-1,TimeUnit.SECONDS);
	}
	
	public static boolean offer(Object channelKey, Object message) throws InterruptedException{
		return offer(channelKey,message,-1,TimeUnit.SECONDS);
	}
	
	public static boolean offer(Object channelKey, Object message, long timeout, TimeUnit unit) throws InterruptedException{
		Collection<AsyncChannel> as = ASYNC_CHANNEL_ROUTING.get(channelKey);
		if(as!=null && as.size()>0){
			message = unwrapMessage(message);
			AsyncMessage am = new AsyncMessage(message, as.size(), timeout, unit);
			boolean taken = false;
			for(AsyncChannel sub: as){
				taken = sub.offerAsync(am) || taken;
			}
			return taken;
		}
		return false;
	}
	
	private static class AsyncMessage{
		final Object payload;
		final AtomicInteger consumers;
		final long timeout;
		final TimeUnit unit;
		public AsyncMessage(Object payload, int numConsumers, long timeout, TimeUnit unit){
			this.payload=payload;
			this.consumers = new AtomicInteger(numConsumers);
			this.timeout=timeout;
			this.unit=unit;
		}
		public void consumed(){
			this.consumers.decrementAndGet();
		}
		public void dropped(){
			if(this.consumers.decrementAndGet()<=0){
				//consumption complete, now let's cleanup any callback channels
				cleanObject(payload);
			}
		}
		@SuppressWarnings("rawtypes")
		private void cleanObject(Object o){
			if(o instanceof Map){
				for(Object v: ((Map)o).values()){
					cleanObject(v);
				}
			}
			if(o instanceof AsyncChannel){
				AsyncChannel ac = (AsyncChannel) o;
				if(!ac.isClosed()){
					ac.close();
				}
			}
		}
	}
	
}

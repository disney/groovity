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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Factory for timed thread interrupts
 * 
 * @author Alex Vigdor
 */
public class InterruptFactory {
	final ScheduledExecutorService interruptService;
	
	public InterruptFactory() {
		interruptService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("Groovity Interrupt "+t.getName());
				return t;
			}
		});
	}
	
	public void destroy() {
		interruptService.shutdown();
		try {
			if(!interruptService.awaitTermination(60, TimeUnit.SECONDS)) {
				interruptService.shutdownNow();
				interruptService.awaitTermination(20, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			interruptService.shutdownNow();
		}
	}
	
	public ScheduledFuture<?> scheduleInterrupt(long delay) {
		return scheduleInterrupt(Thread.currentThread(), delay, TimeUnit.MILLISECONDS);
	}
	
	public ScheduledFuture<?> scheduleInterrupt(long delay, TimeUnit unit) {
		return scheduleInterrupt(Thread.currentThread(), delay, unit);
	}
	
	public ScheduledFuture<?> scheduleInterrupt(Thread thread, long delay) {
		return scheduleInterrupt(thread, delay, TimeUnit.MILLISECONDS);
	}
	
	public ScheduledFuture<?> scheduleInterrupt(Thread thread, long delay, TimeUnit unit) {
		return interruptService.schedule(new Runnable() {
			@Override
			public void run() {
				thread.interrupt();
			}
		}, delay, unit);
	}
}

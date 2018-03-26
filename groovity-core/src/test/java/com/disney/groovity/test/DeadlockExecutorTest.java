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
package com.disney.groovity.test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;

import com.disney.groovity.util.DeadlockFreeExecutor;
import com.disney.groovity.util.InterruptFactory;
/**
 * Prove that DeadlockFreeExecutor can gracefully handle a recursive async chain that overflows the number of allowed threads
 *
 * @author Alex Vigdor
 */
public class DeadlockExecutorTest {
	ExecutorService executor = new DeadlockFreeExecutor(new InterruptFactory(),3);
	
	@Test public void testDeadlockPrevention() throws InterruptedException, ExecutionException{
		//a  normal 3-thread pool would deadlock on this call chain
		Future<Integer> ft = executor.submit(new NestedCall(new NestedCall(new NestedCall(new NestedCall(new NestedCall(new NestedCall(new NestedCall())))))));
		Assert.assertEquals(7l,ft.get().longValue());
	}
	
	private class NestedCall implements Callable<Integer>{
		private Callable<Integer> nested;
		NestedCall(){
			
		}
		
		NestedCall(Callable<Integer> nested){
			this.nested = nested;
		}

		@Override
		public Integer call() throws Exception {
			Integer rval = 1;
			if(nested!=null){
				Future<Integer> ft = executor.submit(nested);
				rval += ft.get();
			}
			return rval;
		}
		
	}
}

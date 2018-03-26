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
package com.disney.groovity.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.model.Model;
import com.disney.groovity.model.ModelConsumer;

/**
 * Gather statistics on groovity script execution, and monitor for stuck threads
 * 
 * @author Alex Vigdor
 *
 */
public class GroovityStatistics implements GroovityConstants{
	private static Log log = LogFactory.getLog(GroovityStatistics.class);
	private static final ConcurrentHashMap<Object, Statistics> timingMap = new ConcurrentHashMap<Object, Statistics>();
	private static final ConcurrentHashMap<Thread,CurrentExecution> threadStackMap = new ConcurrentHashMap<>();
	public static long lastReset=System.currentTimeMillis();
	private static final long stuckThreadTimeout=60000000000l;

	public static void warnStuckThreads() {
		long tooOld = System.nanoTime()-stuckThreadTimeout;
		for(Entry<Thread, CurrentExecution> entry: threadStackMap.entrySet()){
			Execution stack = entry.getValue().current;
			while(stack!=null){
				if(stack.startTime < tooOld){
					log.warn("Thread "+entry.getKey().getName()+" may be stuck, it has been executing "+stack.key+" for "+((System.nanoTime()-stack.startTime)/1000000000l)+" seconds");
					break;
				}
				stack=stack.parent;
			}
		}
	}
	
	/**
	 * return a list of thread profiles describing busy threads, their running time and stack
	 * @return
	 */
	public static Iterable<GroovityThreadProfile> getThreadProfiles(){
		long curTime = System.nanoTime();
		ArrayList<GroovityThreadProfile> profiles = new ArrayList<GroovityThreadProfile>();
		for(Entry<Thread, CurrentExecution> entry: threadStackMap.entrySet()){
			final GroovityThreadProfile profile = new GroovityThreadProfile();
			profile.setName(entry.getKey().getName());
			
			Execution stack = entry.getValue().current;
			final List<String> stackElements = new ArrayList<>();
			while(stack!=null){
				if(stack.parent==null){
					long runTime = curTime - stack.startTime;
					profile.setRunTime(runTime/1000000);
				}
				stackElements.add(stack.key.toString());
				stack=stack.parent;
			}
			profile.setStack(stackElements);
			profiles.add(profile);
		}
		return profiles;
	}
	
	public static void reset(){
		timingMap.clear();
		lastReset=System.currentTimeMillis();
	}
	
	public final static void startExecution(final Object key){
		long time = System.nanoTime();
		final Thread thread = Thread.currentThread();
		CurrentExecution stack = threadStackMap.get(thread);
		Statistics statistics = timingMap.get(key);
		if(statistics==null){
			statistics = new Statistics(key);
			final Statistics oldStats = timingMap.putIfAbsent(key, statistics);
			if(oldStats!=null){
				statistics = oldStats;
			}
		}
		Execution parent = stack!=null?stack.current:null;
		Execution exec = new Execution(parent,key,statistics,time);
		if(stack!=null){
			stack.current=exec;
		}
		else{
			CurrentExecution holder = new CurrentExecution();
			holder.current= exec;
			threadStackMap.put(thread,holder);
		}
	}
	
	public final static void endExecution(){
		final Thread thread = Thread.currentThread();
		final CurrentExecution stack = threadStackMap.get(thread);
		if(stack!=null){
			Execution exec = stack.current;
			if(exec.parent!=null){
				stack.current=exec.parent;
			}
			else{
				threadStackMap.remove(thread);
			}
			final Object key = exec.key;
			final Statistics statistics = exec.statistics;
			statistics.executionCount.incrementAndGet();
			AtomicLong t = null;
			if(exec.parent!=null){
				final ConcurrentHashMap<Object,AtomicLong> parentStats = exec.parent.statistics.callees;
				t = parentStats.get(key);
				if(t==null){
					t = new AtomicLong();
					final AtomicLong p = parentStats.putIfAbsent(key,t);
					if(p!=null){
						t = p;
					}
				}
			}
			final long totalTime = System.nanoTime()-exec.startTime;
			if(t!=null){
				t.addAndGet(totalTime);
				exec.parent.calleeTime.addAndGet(totalTime);
			}
			statistics.grossTime.addAndGet(totalTime);
			long netTime = totalTime-exec.calleeTime.get();
			if(netTime< 0){
				netTime=0;
			}
			statistics.netTime.addAndGet(netTime);
			boolean done = false;
			while(!done){
				long prevMax = statistics.maxTime.get();
				if(totalTime>prevMax){
					done = statistics.maxTime.compareAndSet(prevMax, totalTime);
				}
				else{
					done = true;
				}
			}
		}
	}
	
	public final static class Statistics implements Comparable<Statistics>, Model{
		public final AtomicLong executionCount = new AtomicLong();
		public final AtomicLong grossTime = new AtomicLong();
		public final AtomicLong netTime = new AtomicLong();
		public final AtomicLong maxTime = new AtomicLong();
		public final ConcurrentHashMap<Object,AtomicLong> callees = new ConcurrentHashMap<Object,AtomicLong>();
		public final Object key;
		public Statistics(Object key){
			this.key=key;
		}
		public final int compareTo(Statistics o) {
			long me = grossTime.longValue();
			long oe = o.grossTime.longValue();
			if(me > oe){
				return -1;
			}
			if(me < oe){
				return 1;
			}
			return 0;
		}
		
		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
		@Override
		public void each(ModelConsumer c) {
			c.call("key",key.toString());
			c.call("executionCount",executionCount.get());
			c.call("grossTime",grossTime.get());
			c.call("netTime",netTime.get());
			c.call("maxTime",maxTime.get());
			c.call("callees",callees);
		}
	}
	
	public final static class CurrentExecution {
		public Execution current;
	}
	
	public final static class Execution {
		public final Object key;
		public final long startTime;
		public final Statistics statistics;
		public final AtomicLong calleeTime;
		public final Execution parent;
		
		public Execution(final Execution parent, final Object key, final Statistics stats, final long startTime){
			this.parent=parent;
			this.key=key;
			this.statistics=stats;
			this.startTime = startTime;
			this.calleeTime=new AtomicLong(0);
		}
		
	}

	public static List<Statistics> getStatistics(){
		ArrayList<Statistics> list = new ArrayList<Statistics>(timingMap.values());
		Collections.sort(list);
		return list;
	}
	
	public static Object currentStackKey(){
		CurrentExecution fs = threadStackMap.get(Thread.currentThread());
		if(fs!=null){
			return fs.current.key;
		}
		return null;
	}
	
	public static Execution snapshot(){
		CurrentExecution fs = threadStackMap.get(Thread.currentThread());
		return fs !=null ? fs.current : null;
	}
	
	public static Execution registerStack(Execution stack){
		final Thread t = Thread.currentThread();
		if(stack==null){
			CurrentExecution rm = threadStackMap.remove(t);
			return rm !=null ? rm.current : null;
		}
		CurrentExecution fs = threadStackMap.get(t);
		if(fs!=null){
			Execution old = fs.current;
			fs.current=stack;
			return old;
		}
		CurrentExecution curr = new CurrentExecution();
		curr.current=stack;
		threadStackMap.put(t, curr);
		return null;
	}

}

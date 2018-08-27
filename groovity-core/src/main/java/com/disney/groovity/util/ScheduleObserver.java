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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityObserver;

import groovy.lang.Binding;
import groovy.lang.Script;

public class ScheduleObserver implements GroovityObserver.Field{
	Logger log = Logger.getLogger(ScheduleObserver.class.getName());
	Pattern schedulePattern = Pattern.compile("([\\d.]+)(ms|s|m|h|d)?", Pattern.CASE_INSENSITIVE);
	ScheduledExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors()*4, new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName("Groovity Schedule thread "+t.getName());
			return t;
		}
	});
	ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

	@Override
	public String getField() {
		return "schedule";
	}

	@Override
	public void scriptStart(Groovity groovity, String scriptPath, Class<Script> scriptClass, Object fieldValue) {
		long time;
		TimeUnit unit = TimeUnit.MILLISECONDS;
		if(fieldValue instanceof Number) {
			time = ((Number)fieldValue).longValue();
		}
		else {
			Matcher m = schedulePattern.matcher(fieldValue.toString());
			if(!m.matches()) {
				throw new IllegalArgumentException("Can't parse schedule pattern "+fieldValue);
			}
			String num = m.group(1);
			String u = m.group(2);
			time = Integer.parseInt(num);
			if(u!=null) {
				if("s".equalsIgnoreCase(u)) {
					unit = TimeUnit.SECONDS;
				}
				else if("m".equalsIgnoreCase(u)) {
					unit = TimeUnit.MINUTES;
				}
				else if("h".equalsIgnoreCase(u)) {
					unit = TimeUnit.HOURS;
				}
				else if("d".equalsIgnoreCase(u)) {
					unit = TimeUnit.DAYS;
				}
			}
		}
		ScheduledFuture<?> f = executor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					groovity.run(scriptPath, new Binding());
				}
				catch(Throwable th) {
					log.log(Level.SEVERE, "Error executing scheduled script "+scriptPath, th);
				}
			}
		}, time, time, unit);
		log.info("Submitted script schedule: '"+scriptPath+"' to run every "+time+" "+unit);
		scheduledTasks.put(scriptPath, f);
	}

	@Override
	public void scriptDestroy(Groovity groovity, String scriptPath, Class<Script> scriptClass, Object fieldValue) {
		ScheduledFuture<?> f = scheduledTasks.remove(scriptPath);
		if(f!=null) {
			log.info("Cancelled script schedule: '"+scriptPath+"'");
			f.cancel(true);
		}
	}

	@Override
	public void destroy(Groovity groovity) {
		if(!scheduledTasks.isEmpty()) {
			log.info("Stopping "+scheduledTasks.size()+" scheduled tasks on groovity destroy");
		}
		executor.shutdown();
		try {
			if(!executor.awaitTermination(60, TimeUnit.SECONDS)) {
				executor.shutdownNow();
				executor.awaitTermination(60, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
		}
	}

}

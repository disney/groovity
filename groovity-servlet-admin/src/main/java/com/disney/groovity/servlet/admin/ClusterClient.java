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
package com.disney.groovity.servlet.admin;

import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates the necessary components to join and communicate with a cluster; 
 * a ClusterProvider is required, to which this ClusterClient joins itself upon init() and
 * leaves upon destroy().  The LocalHostResolver is used to discover this hosts address to join the cluster,
 * along with the port it can be reached at.
 * 
 * This client sends a ping to the cluster once per minute; this way a clusterProvider can detect members 
 * who disappear without formally leaving.
 * 
 * @author Alex Vigdor
 *
 */
public class ClusterClient {
	private ScheduledExecutorService executor;
	private ClusterProvider clusterProvider;
	ClusterMember localMember;
	private int port;
	private LocalHostResolver localHostResolver = new DefaultLocalHostResolver();
	//private Groovity groovity;
	
	public void init() throws UnknownHostException{
		localMember = new ClusterMember(getLocalHostResolver().getLocalHost(),port,UUID.randomUUID());
		clusterProvider.join(localMember);
		executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("Groovity Cluster Client "+t.getName());
				return t;
			}
		});
		executor.scheduleWithFixedDelay(new Runnable(){
				public void run() {
					clusterProvider.ping(localMember);
				}
			}, 60, 60, TimeUnit.SECONDS);
	}
	public void destroy(){
		executor.shutdown();
		try {
			if(!executor.awaitTermination(60, TimeUnit.SECONDS)) {
				executor.shutdownNow();
				executor.awaitTermination(20, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
		}
		clusterProvider.leave(localMember);
	}
	public ClusterProvider getClusterProvider() {
		return clusterProvider;
	}
	public void setClusterProvider(ClusterProvider clusterProvider) {
		this.clusterProvider = clusterProvider;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}

	public LocalHostResolver getLocalHostResolver() {
		return localHostResolver;
	}
	public void setLocalHostResolver(LocalHostResolver localHostResolver) {
		this.localHostResolver = localHostResolver;
	}
}

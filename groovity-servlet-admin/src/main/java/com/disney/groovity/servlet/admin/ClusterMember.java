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

import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;


/**
 * Represents an individual cluster member by InetAddress and port; a UUID is generated when a ClusterClient 
 * joins a ClusterProvider.
 * 
 * @author Alex Vigdor
 */
public class ClusterMember implements Serializable {
	private static final long serialVersionUID = 913543582522628938L;
	private InetAddress address;
	private int port = 8080;
	private UUID uuid;
	
	public ClusterMember(){
		
	}
	public ClusterMember(InetAddress address, int port, UUID uuid){
		this.address=address;
		this.uuid=uuid;
		this.port=port;
	}
	public InetAddress getAddress() {
		return address;
	}
	public void setAddress(InetAddress address) {
		this.address = address;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	public String toString(){
		return "ClusterMember["+(address!=null?address.toString():"")+":"+port+":"+(uuid!=null?uuid.toString():"")+"]";
	}
	public int hashCode(){
		int hc = 987654;
		hc-=port;
		if(address!=null){
			hc-=address.hashCode();
		}
		if(uuid!=null){
			hc+=uuid.hashCode();
		}
		return hc;
	}
	public boolean equals(Object o){
		if(o instanceof ClusterMember){
			ClusterMember cm = (ClusterMember)o;
			if(port!=cm.port){
				return false;
			}
			if(address==null){
				if(cm.address!=null){
					return false;
				}
			}
			else{
				if(cm.address==null || !address.equals(cm.address)){
					return false;
				}
			}
			if(uuid==null){
				if(cm.uuid!=null){
					return false;
				}
			}
			else{
				if(cm.uuid==null || !uuid.equals(cm.uuid)){
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
}

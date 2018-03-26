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

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Track cluster members in memory
 * 
 * @author Alex Vigdor
 */
public class DefaultClusterProvider implements ClusterProvider {
	private ConcurrentHashMap<ClusterMember,ClusterMemberState> memberStates = new ConcurrentHashMap<ClusterMember, ClusterMemberState>();

	public Iterable<ClusterMember> getMembers() {
		ArrayList<ClusterMember> out = new ArrayList<ClusterMember>();
		for(ClusterMemberState memberState:memberStates.values()){
			//check if member is active or left in the last hour
			long oneHourAgo = System.currentTimeMillis()-3600000;
			if(memberState.timeJoined>oneHourAgo || memberState.timePinged>oneHourAgo || memberState.timeLeft > oneHourAgo){
				out.add(memberState.member);
			}
		}
		return out;
	}

	public void join(ClusterMember member) {
		getState(member).timeJoined = System.currentTimeMillis();
	}
	
	private ClusterMemberState getState(ClusterMember member){
		ClusterMemberState state = memberStates.get(member);
		if(state==null){
			state = new ClusterMemberState();
			state.member=member;
			memberStates.put(member, state);
		}
		return state;
	}

	public void leave(ClusterMember member) {
		getState(member).timeLeft = System.currentTimeMillis();
	}

	public void ping(ClusterMember member) {
		ClusterMemberState state = getState(member);
		state.timePinged = System.currentTimeMillis();
		state.numPings++;
	}

}

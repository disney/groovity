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

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements {@link AsyncChannelAnonymousManagerMBean}
 *
 * @author Avi Herbstman
 */
public class AsyncChannelAnonymousManager implements AsyncChannelAnonymousManagerMBean {
    /**
     * stores aggregate total of anonymous channels opened
     */
    private static final AtomicInteger aggregateTotalChannelsOpened = new AtomicInteger(0);
    /**
     * stores aggregate total of anonymous channels closed
     */
    private static final AtomicInteger aggregateTotalChannelsClosed = new AtomicInteger(0);
    /**
     * stores object name of MBean
     */
    private final ObjectName mbeanObjectName;
    /**
     * log
     */
    private static final Logger log = Logger.getLogger(AsyncChannel.class.getName());

    public AsyncChannelAnonymousManager() {
        this.mbeanObjectName = getObjectName();
        this.registerAnonymousChannelMBean();
    }

    @Override
    public int getNumberOfCurrentlyOpenChannels(){
        return aggregateTotalChannelsOpened.get()- aggregateTotalChannelsClosed.get();
    }

    @Override
    public int getNumberOfTotalChannelsOpened(){
        return aggregateTotalChannelsOpened.get();
    }

    /**
     * returns the aggregate total anonymous channels closed
     */
    public int getNumberOfTotalChannelsClosed(){
        return aggregateTotalChannelsClosed.get();
    }

    /**
     * increments the aggregate total anonymous channels closed
     */
    public void incrementNumberOfTotalChannelsClose(){
        aggregateTotalChannelsClosed.incrementAndGet();
    }

    /**
     * increments the aggregate total anonymous channels opened
     */
    public void incrementNumberOfTotalChannelsOpened(){
        aggregateTotalChannelsOpened.incrementAndGet();
    }

    /**
     * connects to MBean Server and registers MBean
     */
    public void registerAnonymousChannelMBean() {
        try {
            //Get the MBean server
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            //register the MBean
            if(!mbs.isRegistered(mbeanObjectName)) {
		        	mbs.registerMBean(this, mbeanObjectName);
		        	if(log.isLoggable(Level.FINE)){
		        		log.log(Level.FINE, "MBean succesfully registered for Anonymous Channel with name: AnonymousChannel");
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * returns object name to be used for MBean registration
     */
    private static ObjectName getObjectName() {
        try {
            return new ObjectName("com.disney.groovity:name=AnonymousChannel,type=AsyncChannelAnonymousManager");
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}

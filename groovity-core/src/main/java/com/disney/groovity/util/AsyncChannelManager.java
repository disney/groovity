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

import com.disney.groovity.stats.GroovityStatistics;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements {@link AsyncChannelManagerMBean}
 *
 * @author Avi Herbstman
 */
public class AsyncChannelManager implements AsyncChannelManagerMBean {
    /**
     * stores total number of named channels
     */
    private static final AtomicLong totalAsyncChannelCount = new AtomicLong(0);
    /**
     * stores name given to a AsyncChannel object
     */
    private final String channelName;
    /**
     * stores the LocalDateTime when a named channel is opened
     */
    private final LocalDateTime channelDateTimeOpened;
    /**
     * stores the LocalDateTime when the last message was enqueued in a named channel
     */
    private final AtomicReference<LocalDateTime> lastMessageEnqueuedTime;
    /**
     * stores the LocalDateTime when the first message was processed in a named channel
     */
    private final AtomicReference<LocalDateTime> firstMessageProcessedTime;
    /**
     * stores the LocalDateTime when the last message was processed in a named channel
     */
    private final AtomicReference<LocalDateTime> lastMessageProcessedTime;
    /**
     * stores the LocalDateTime when the first message was dropped from a named channel
     */
    private final AtomicReference<LocalDateTime> firstMessageDroppedTime;
    /**
     * stores the LocalDateTime when the last message was dropped from a named channel
     */
    private final AtomicReference<LocalDateTime> lastMessageDroppedTime;
    /**
     * stores the LocalDateTime when the first message was evicted from a named channel
     */
    private final AtomicReference<LocalDateTime> firstMessageEvictedTime;
    /**
     * stores the LocalDateTime when the last message was evicted from a named channel
     */
    private final AtomicReference<LocalDateTime> lastMessageEvictedTime;
    /**
     * stores the AsyncChannel object of a named channel
     */
    private final AsyncChannel asc;
    /**
     * stores the objectName of the MBean object
     */
    private final ObjectName mbeanObjectName;
    /**
     * stores the current total messages enqueued in a named channel
     */
    private final AtomicInteger totalMessagesEnqueued = new AtomicInteger(0);
    /**
     * stores the number of total messages processed in a named channel
     */
    private final AtomicInteger totalMessagesProcessed = new AtomicInteger(0);
    /**
     * stores the number of total messages dropped from a named channel
     */
    private final AtomicInteger totalMessagesDropped = new AtomicInteger(0);
    /**
     * stores the number of total messages evicted from a named channel
     */
    private final AtomicInteger totalMessagesEvicted = new AtomicInteger(0);
    /**
     * stores the capacity of the queue of a named channel
     */
    private final int queueCapacity;
    /**
     * stores in millis the total message processing time from a named channel
     */
    private final AtomicLong totalMessageProcessingTimeToMillis = new AtomicLong(0);
    /**
     * stores in millis the processing time of the last message
     */
    private final AtomicLong lastMessageProcessingTimeToMillis = new AtomicLong(0);
    /**
     * stores in millis the max processing time of a message from a named channel
     */
    private final AtomicLong maxMessageProcessingTimeToMillis = new AtomicLong(0);
    /**
     * log
     */
    private static final Logger log = Logger.getLogger(AsyncChannel.class.getName());
    /**
     * stores stack key of a channel context to be used to append to MBean name
     */
    private final String channelContext = GroovityStatistics.currentStackKey().toString();
    
    private ObjectInstance mbeanRegistration = null;
    
    public AsyncChannelManager(AsyncChannel asyncChannel) {
        this.asc = asyncChannel;
        this.channelName = asc.getKey().toString();
        this.channelDateTimeOpened = LocalDateTime.now();
        this.firstMessageProcessedTime = new AtomicReference<>(null);
        this.lastMessageProcessedTime = new AtomicReference<>(null);
        this.lastMessageEnqueuedTime = new AtomicReference<>(null);
        this.firstMessageDroppedTime = new AtomicReference<>(null);
        this.lastMessageDroppedTime = new AtomicReference<>(null);
        this.firstMessageEvictedTime = new AtomicReference<>(null);
        this.lastMessageEvictedTime = new AtomicReference<>(null);
        this.queueCapacity = asc.messageQueue.size()+asc.messageQueue.remainingCapacity();
        String mBeanName = asc.getKey().toString();
		try {
			mBeanName = URLEncoder.encode(mBeanName,"UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
        this.mbeanObjectName = getObjectName(mBeanName);
    }

    @Override
    public LocalDateTime getChannelOpenDateTime(){
        return this.channelDateTimeOpened;
    }

    @Override
    public String getChannelName() {
        return this.channelName;
    }

    @Override
    public int getCapacityOfQueue() {
        return queueCapacity;
    }

    @Override
    public int getCurrentNumOfMessages(){
        return asc.messageQueue.size();
    }
    @Override
    public LocalDateTime getLastMessageProcessedTime(){
        return lastMessageProcessedTime.get();
    }

    @Override
    public LocalDateTime getLastMessageEnqueuedTime(){
        return lastMessageEnqueuedTime.get();
    }

    @Override
    public LocalDateTime getFirstMessageProcessedTime(){
        return firstMessageProcessedTime.get();
    }

    @Override
    public LocalDateTime getLastMessageDroppedTime(){
        return lastMessageDroppedTime.get();
    }

    @Override
    public LocalDateTime getFirstMessageDroppedTime(){
        return firstMessageDroppedTime.get();
    }

    @Override
    public LocalDateTime getLastMessageEvictedTime(){
        return lastMessageEvictedTime.get();
    }

    @Override
    public LocalDateTime getFirstMessageEvictedTime(){
        return firstMessageEvictedTime.get();
    }

    @Override
    public int getTotalMessagesEnqueued(){
        return totalMessagesEnqueued.get();
    }

    @Override
    public int getTotalMessagesProcessed(){
        return totalMessagesProcessed.get();
    }

    @Override
    public double getTotalMessageProcessingTimeToMillis() {
        return (double)totalMessageProcessingTimeToMillis.get();
    }

    @Override
    public double getLastMessageProcessingTimeToMillis() {
        return (double)lastMessageProcessingTimeToMillis.get();
    }

    @Override
    public double getMaxMessageProcessingTimeToMillis() {
        return (double)maxMessageProcessingTimeToMillis.get();
    }

    @Override
    public double getMeanMessageProcessingTimeToMillis() {
        return (double) totalMessageProcessingTimeToMillis.get()/totalMessagesProcessed.get();
    }

    @Override
    public int getTotalMessagesDropped(){
        return totalMessagesDropped.get();
    }

    @Override
    public int getTotalMessagesEvicted(){
        return totalMessagesEvicted.get();
    }

    @Override
    public String getChannelContext(){
        return channelContext;
    }

    /**
     * sets the LocalDateTime of the last message processed in a named channel
     */
    public void setLastMessageProcessedTime(LocalDateTime ldt){
        this.lastMessageProcessedTime.set(ldt);
    }

    /**
     * updates when latest message was Enqueued to a named channel
     */
    public void setLastMessageEnqueuedTime(LocalDateTime current){
        this.lastMessageEnqueuedTime.set(current);
    }

    /**
     * sets the LocalDateTime of the first message processed in a named channel
     */
    public void setFirstMessageProcessedTime(LocalDateTime current){
        this.firstMessageProcessedTime.set(current);
    }

    /**
     * updates when latest message dropped from a named channel
     */
    public void setLastMessageDroppedTime(LocalDateTime current){
        this.lastMessageDroppedTime.set(current);
    }

    /**
     * sets the DateTime of the first message dropped from a named channel
     */
    public void setFirstMessageDroppedTime(LocalDateTime current){
        this.firstMessageDroppedTime.set(current);
    }

    /**
     * updates when latest message is evicted from a named channel
     */
    public void setLastMessageEvictedTime(LocalDateTime current){
        this.lastMessageEvictedTime.set(current);
    }

    /**
     * sets the DateTime of the first message is evicted from a named channel
     */
    public void setFirstMessageEvictedTime(LocalDateTime current){
        this.firstMessageEvictedTime.set(current);
    }

    /**
     * increments the total number of messages Enqueued to a named channel after a message is offered
     */
    public void incrementTotalMessagesEnqueued(){
        totalMessagesEnqueued.incrementAndGet();
    }

    /**
     * increments the total number of messages accepted to a named channel
     * after a message is accepted
     */
    public void incrementTotalMessagesProcessed(){
        totalMessagesProcessed.incrementAndGet();
    }

    /**
     * updates the totalMessageProcessingTime
     */
    public void addTotalMessageProcessingTimeMillis(long l){
        totalMessageProcessingTimeToMillis.addAndGet(l);
    }

    /**
     * updates the lastMessageProcessingTime
     */
    public void setLastMessageProcessingTimeToMillis(long l){
        lastMessageProcessingTimeToMillis.set(l);
    }

    /**
     * updates the maxMessageProcessingTime
     */
    public void setMaxMessageProcessingTimeToMillis(long l){
        maxMessageProcessingTimeToMillis.set(l);
    }

    /**
     * increments the total number of messages dropped from a named channel
     */
    public void incrementTotalMessagesDropped(){
        totalMessagesDropped.incrementAndGet();
    }

    /**
     * increments the total number of messages evicted from a named channel
     */
    public void incrementTotalMessagesEvicted(){
        totalMessagesEvicted.incrementAndGet();
    }

    /**
     * method to update maxMessageProcessing Time, totalMessageProcessingTime
     * and lastMessageProcessing time after a message is processed
     */
    public void updateMBeanForMessageProcessingTime(long l){
        if(l > maxMessageProcessingTimeToMillis.get()){
            maxMessageProcessingTimeToMillis.set(l);
        }
        addTotalMessageProcessingTimeMillis(l);
        setLastMessageProcessingTimeToMillis(l);
    }

    /**
     * updates mBean values for firstMessageProcessedTime, totalMessagesProcessed
     * and lastMessageProcessedTime
     * after an accept occurs to a named channel
     */
    public void updateMBeanForMessageProcessed(){
        //updating time of LastMessage in MBean and number of Messages in Queue
        LocalDateTime current = LocalDateTime.now();
        incrementTotalMessagesProcessed();
        setLastMessageProcessedTime(current);
        if(getTotalMessagesProcessed()==1){
            this.setFirstMessageProcessedTime(current);
        }
    }

    /**
     * updates mBean values for totalMessagesEnqueued after an offer occurs to a named channel
     */
    public void updateMBeanForMessageEnqueued(){
        //updating time of LastMessage in MBean and number of Messages in Queue
            LocalDateTime current = LocalDateTime.now();
            incrementTotalMessagesEnqueued();
            setLastMessageEnqueuedTime(current);
    }

    /**
     * updates time of LastMessage dropped in MBean and number of dropped Messages from the Queue
     * if it is the first message dropped then that is set
     */
    public void updateMBeanForMessagesDropped(){
        //updating time of LastMessage failure in MBean and number of failed Messages to reach Queue
        LocalDateTime current = LocalDateTime.now();
        incrementTotalMessagesDropped();
        setLastMessageDroppedTime(current);
        if(getTotalMessagesDropped()==1){
            setFirstMessageDroppedTime(current);
        }
    }

    /**
     * updates time of LastMessage evicted in MBean and number of evicted Messages from the Queue
     * if it is the first message evicted then that is set
     */
    public void updateMBeanForMessagesEvicted(){
        //updating time of LastMessage failure in MBean and number of failed Messages to reach Queue
        LocalDateTime current = LocalDateTime.now();
        incrementTotalMessagesEvicted();
        setLastMessageEvictedTime(current);
        if(getTotalMessagesEvicted()==1){
            setFirstMessageEvictedTime(current);
        }
    }

    /**
     * registers MBean when a named channel is opened
     */
    public void registerChannelMBean() {
	    	if(mbeanRegistration!=null) {
	    		return;
	    	}
        try {
            //Get the MBean server
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            //register the MBean
            mbeanRegistration = mbs.registerMBean(this, mbeanObjectName);
            if(log.isLoggable(Level.FINE)){
                log.log(Level.FINE, "MBean succesfully registered with name: "+asc.getKey().toString());
            }
        }
        catch (Exception e) {
        		log.log(Level.SEVERE, "Error registering mbean "+mbeanObjectName, e);
        }
    }

    /**
     * de-registers the MBean
     */
    public void deRegisterChannelMBean(){
	    	if(mbeanRegistration==null) {
	    		return;
	    	}
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.unregisterMBean(mbeanObjectName);
            if(log.isLoggable(Level.FINE)){
                log.log(Level.FINE, "MBean successfully deRegistered with name: "+asc.getKey().toString());
            }
        }
        catch (Exception e) {
        		log.log(Level.SEVERE, "Error deregistering mbean "+mbeanObjectName, e);
        }
        finally {
        		mbeanRegistration = null;
        }
    }

    /**
     * method to adjust the ObjectName by concatenating
     * an increment of 1 to the channel name of each new MBean
     */
    private static ObjectName getObjectName(String mBeanName) {
        try {
            long chanNum = totalAsyncChannelCount.incrementAndGet();
            return new ObjectName("com.disney.groovity:name=" +
                    mBeanName + "-" + chanNum + ",type=AsyncChannelManager");
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
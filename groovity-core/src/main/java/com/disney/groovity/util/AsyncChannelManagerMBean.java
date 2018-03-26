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

import java.time.LocalDateTime;

/**
 * MBean interface to provide functionality for {@link AsyncChannel} management
 *
 * @author Avi Herbstman
 */
public interface AsyncChannelManagerMBean {

    /**
     * returns DateTime when a named channel is opened
     */
    public LocalDateTime getChannelOpenDateTime();

    /**
     * returns name of channel
     */
    public String getChannelName();

    /**
     * returns the capacity of the queue of the AsynChannel object
     */
    public int getCapacityOfQueue();

    /**
     * returns the current number of messages on the queue as an int
     */
    public int getCurrentNumOfMessages();

    /**
     * returns the LocalDateTime of the first messaged processed in a named channel
     */
    public LocalDateTime getFirstMessageProcessedTime();

    /**
     * returns the LocalDateTime of the last messaged processed in a named channel
     */
    public LocalDateTime getLastMessageProcessedTime();

    /**
     * returns the DateTime of the first message dropped from a named channel
     */
    public LocalDateTime getFirstMessageDroppedTime();

    /**
     * returns the DateTime of the latest message dropped from a named channel
     */
    public LocalDateTime getLastMessageDroppedTime();

    /**
     * returns the DateTime of the first message evicted from a named channel
     */
    public LocalDateTime getFirstMessageEvictedTime();

    /**
     * returns the DateTime of the latest message evicted from a named channel
     */
    public LocalDateTime getLastMessageEvictedTime();
    /**
     * returns the DateTime of the latest message offered to a named channel
     */
    public LocalDateTime getLastMessageEnqueuedTime();

    /**
     * returns the total number of messages offered to a named channel
     */
    public int getTotalMessagesEnqueued();

    /**
     * //returns total number of messages offered to a named channel
     */
    public int getTotalMessagesProcessed();

    /**
     * returns totalMessageProcessingTime in Milliseconds
     */
    public double getTotalMessageProcessingTimeToMillis();

    /**
     * returns lastMessageProcessingTime in Milliseconds
     */
    public double getLastMessageProcessingTimeToMillis();

    /**
     * returns maxMessageProcessingTime in Milliseconds
     */
    public double getMaxMessageProcessingTimeToMillis();

    /**
     * returns meanMessageProcessingTime in Milliseconds
     */
    public double getMeanMessageProcessingTimeToMillis();

    /**
     * returns the total number of dropped messages from a named channel
     */
    public int getTotalMessagesDropped();

    /**
     * returns the total number of evicted messages from a named channel
     */
    public int getTotalMessagesEvicted();

    /**
     * returns stack key of a channel context to be used to append to MBean name
     */
    public String getChannelContext();
}
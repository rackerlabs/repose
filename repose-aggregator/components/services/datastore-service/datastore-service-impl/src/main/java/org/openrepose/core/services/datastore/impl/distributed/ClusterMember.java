/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.datastore.impl.distributed;

import java.net.InetSocketAddress;

public class ClusterMember {

    private static final int REQUIRED_VALIDATION_PASSES = 4;
    private final InetSocketAddress memberAddress;
    private final int droppedMemberRestTime, requiredValidationPasses;
    private long droppedTime, restPeriod;
    private int validationPass;
    private boolean online;

    public ClusterMember(InetSocketAddress memberAddress, int droppedMemberRestTime) {
        this(REQUIRED_VALIDATION_PASSES, memberAddress, droppedMemberRestTime);
    }

    public ClusterMember(int requiredValidationPasses, InetSocketAddress memberAddress, int droppedMemberRestTime) {
        this.memberAddress = memberAddress;
        this.droppedMemberRestTime = droppedMemberRestTime;
        this.requiredValidationPasses = requiredValidationPasses;

        online = true;
        validationPass = 0;
    }

    private static long nowInMilliseconds() {
        return System.currentTimeMillis();
    }

    public InetSocketAddress getMemberAddress() {
        return memberAddress;
    }

    public boolean shouldRetry() {
        final long nowInMilliseconds = nowInMilliseconds();
        final boolean retry = nowInMilliseconds - droppedTime > restPeriod;

        if (retry) {
            logMemberRetry(nowInMilliseconds);
        }

        return retry;
    }

    private void logMemberRetry(long nowInMilliseconds) {
        if (validationPass++ < requiredValidationPasses) {
            restPeriod = (long) droppedMemberRestTime / validationPass;
            droppedTime = nowInMilliseconds;
        } else {
            validationPass = 0;
            droppedTime = 0;

            online = true;
        }
    }

    public void setOffline() {
        droppedTime = nowInMilliseconds();
        restPeriod = droppedMemberRestTime;
        validationPass = 0;

        online = false;
    }

    public boolean isOnline() {
        return online;
    }

    public boolean isOffline() {
        return !online;
    }
}

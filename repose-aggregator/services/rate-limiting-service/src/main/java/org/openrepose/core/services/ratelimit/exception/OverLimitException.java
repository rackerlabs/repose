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
package org.openrepose.core.services.ratelimit.exception;

import java.util.Date;

public class OverLimitException extends Exception {

    private final String user;
    private final Date nextAvailableTime;
    private final int currentLimitAmount;
    private final String configuredLimit;

    public OverLimitException(String msg, String user, Date nextAvailableTime, int currentLimitAmount, String configuredLimit) {
        super(msg);
        this.user = user;
        this.nextAvailableTime = (Date) nextAvailableTime.clone();
        this.currentLimitAmount = currentLimitAmount;
        this.configuredLimit = configuredLimit;
    }

    public String getUser() {
        return user;
    }

    public Date getNextAvailableTime() {
        return (Date) nextAvailableTime.clone();
    }

    public int getCurrentLimitAmount() {
        return currentLimitAmount;
    }

    public String getConfiguredLimit() {
        return configuredLimit;
    }
}

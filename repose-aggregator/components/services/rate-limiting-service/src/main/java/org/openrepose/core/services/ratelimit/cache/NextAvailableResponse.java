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
package org.openrepose.core.services.ratelimit.cache;

import org.apache.commons.lang3.tuple.Pair;
import org.openrepose.core.services.ratelimit.config.ConfiguredRatelimit;

import java.util.Date;

/**
 * @author jhopper
 */
public class NextAvailableResponse {

    private final Pair<ConfiguredRatelimit, CachedRateLimit> limitPair;

    public NextAvailableResponse(Pair<ConfiguredRatelimit, CachedRateLimit> limitPair) {
        this.limitPair = limitPair;
    }

    public Date getResetTime() {
        return new Date(limitPair.getValue().getNextExpirationTime());
    }

    public boolean hasRequestsRemaining() {
        return (limitPair == null) || (limitPair.getValue().maxAmount() - limitPair.getValue().amount() >= 0);
    }

    public int getCurrentLimitAmount() {
        return limitPair.getValue().amount();
    }

    public Pair<ConfiguredRatelimit, CachedRateLimit> getLimitPair() {
        return limitPair;
    }

    @Override
    public String toString() {
        return "NextAvailableResponse{" +
                "hasRequests=" + hasRequestsRemaining() +
                ", resetTime=" + getResetTime().getTime() +
                ", currentLimitAmount=" + getCurrentLimitAmount() +
                '}';
    }
}

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
import org.openrepose.core.services.datastore.Patchable;
import org.openrepose.core.services.ratelimit.config.ConfiguredRatelimit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 1/28/14
 * Time: 9:33 AM
 */
public class UserRateLimit implements Patchable<UserRateLimit, UserRateLimit.Patch> {

    private final Pair<ConfiguredRatelimit, CachedRateLimit> leastRemainingLimit;

    private ConcurrentHashMap<String, CachedRateLimit> limitMap = new ConcurrentHashMap<>();

    public UserRateLimit() {
        this.limitMap = new ConcurrentHashMap<>();
        this.leastRemainingLimit = null;
    }

    public UserRateLimit(Map<String, CachedRateLimit> limitMap) {
        this.limitMap = new ConcurrentHashMap<>(limitMap);
        this.leastRemainingLimit = null;
    }

    private UserRateLimit(Map<String, CachedRateLimit> limitMap, Pair<ConfiguredRatelimit, CachedRateLimit> lowestLimit) {
        this.limitMap = new ConcurrentHashMap<>(limitMap);
        this.leastRemainingLimit = lowestLimit;
    }

    public ConcurrentMap<String, CachedRateLimit> getLimitMap() {
        return limitMap;
    }

    public Pair<ConfiguredRatelimit, CachedRateLimit> getLowestLimit() {
        return leastRemainingLimit;
    }

    @Override
    public UserRateLimit applyPatch(Patch patch) {
        HashMap<String, CachedRateLimit> returnLimits = new HashMap<>();
        Pair<ConfiguredRatelimit, CachedRateLimit> lowestLimit = null;

        for (Pair<String, ConfiguredRatelimit> limitEntry : patch.getLimitMap()) {
            CachedRateLimit rateLimit = adjustLimit(limitEntry);
            returnLimits.put(limitEntry.getKey(), rateLimit);
            if (lowestLimit == null || (rateLimit.maxAmount() - rateLimit.amount() < lowestLimit.getValue().maxAmount() - lowestLimit.getValue().amount())) {
                lowestLimit = Pair.of(limitEntry.getValue(), rateLimit);
            }
            if (rateLimit.amount() > rateLimit.maxAmount()) {
                break;
            }
        }

        return new UserRateLimit(returnLimits, lowestLimit);
    }

    private CachedRateLimit adjustLimit(Pair<String, ConfiguredRatelimit> limitEntry) {
        CachedRateLimit returnRateLimit;

        while (true) {
            CachedRateLimit newRateLimit = new CachedRateLimit(limitEntry.getValue(), 1);
            CachedRateLimit oldRateLimit = limitMap.putIfAbsent(limitEntry.getKey(), newRateLimit);

            if (oldRateLimit == null) {
                return newRateLimit;
            }

            if ((System.currentTimeMillis() - oldRateLimit.timestamp()) > oldRateLimit.unit()) {
                returnRateLimit = newRateLimit;
            } else {
                returnRateLimit = new CachedRateLimit(limitEntry.getValue(), oldRateLimit.amount() + 1, oldRateLimit.timestamp());
            }

            if (limitMap.replace(limitEntry.getKey(), oldRateLimit, returnRateLimit)) {
                return returnRateLimit;
            }
        }
    }

    public static class Patch implements org.openrepose.core.services.datastore.Patch<UserRateLimit> {

        private List<Pair<String, ConfiguredRatelimit>> limitMap;

        public Patch(List<Pair<String, ConfiguredRatelimit>> patchMap) {
            this.limitMap = new ArrayList<>(patchMap);
        }

        @Override
        public UserRateLimit newFromPatch() {
            UserRateLimit newUserLimit = new UserRateLimit();

            return newUserLimit.applyPatch(this);
        }

        public List<Pair<String, ConfiguredRatelimit>> getLimitMap() {
            return limitMap;
        }
    }
}

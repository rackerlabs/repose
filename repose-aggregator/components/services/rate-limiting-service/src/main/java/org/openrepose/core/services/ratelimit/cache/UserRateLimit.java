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

import java.io.Serializable;
import java.util.*;

public class UserRateLimit implements Serializable {

    private final Pair<ConfiguredRatelimit, CachedRateLimit> leastRemainingLimit;
    private final Map<String, CachedRateLimit> limitMap;

    public UserRateLimit() {
        this.limitMap = Collections.emptyMap();
        this.leastRemainingLimit = null;
    }

    public UserRateLimit(Map<String, CachedRateLimit> limitMap) {
        this.limitMap = Collections.unmodifiableMap(new HashMap<>(limitMap));
        this.leastRemainingLimit = null;
    }

    private UserRateLimit(Map<String, CachedRateLimit> limitMap, Pair<ConfiguredRatelimit, CachedRateLimit> lowestLimit) {
        this.limitMap = Collections.unmodifiableMap(new HashMap<>(limitMap));
        this.leastRemainingLimit = lowestLimit;
    }

    public Map<String, CachedRateLimit> getLimitMap() {
        return limitMap;
    }

    public Pair<ConfiguredRatelimit, CachedRateLimit> getLowestLimit() {
        return leastRemainingLimit;
    }

    public static class Patch implements org.openrepose.core.services.datastore.Patch<UserRateLimit> {

        private final List<Pair<String, ConfiguredRatelimit>> applicableLimits;

        public Patch(List<Pair<String, ConfiguredRatelimit>> patchMap) {
            this.applicableLimits = Collections.unmodifiableList(new ArrayList<>(patchMap));
        }

        @Override
        public UserRateLimit newFromPatch() {
            UserRateLimit newUserLimit = new UserRateLimit();

            return applyPatch(newUserLimit);
        }

        @Override
        public UserRateLimit applyPatch(UserRateLimit currentValue) {
            HashMap<String, CachedRateLimit> returnLimits = new HashMap<>();
            Pair<ConfiguredRatelimit, CachedRateLimit> lowestLimit = null;

            Map<String, CachedRateLimit> currentLimits = currentValue.getLimitMap();
            for (Pair<String, ConfiguredRatelimit> applicableLimit : applicableLimits) {
                CachedRateLimit rateLimit = getUpdatedLimit(currentLimits, applicableLimit);
                returnLimits.put(applicableLimit.getKey(), rateLimit);
                if (lowestLimit == null || (rateLimit.maxAmount() - rateLimit.amount() < lowestLimit.getValue().maxAmount() - lowestLimit.getValue().amount())) {
                    lowestLimit = Pair.of(applicableLimit.getValue(), rateLimit);
                }
                if (rateLimit.amount() > rateLimit.maxAmount()) {
                    break;
                }
            }

            return new UserRateLimit(returnLimits, lowestLimit);
        }

        private CachedRateLimit getUpdatedLimit(Map<String, CachedRateLimit> currentLimits, Pair<String, ConfiguredRatelimit> limitEntry) {
            CachedRateLimit oldRateLimit = currentLimits.get(limitEntry.getKey());
            if (oldRateLimit == null || (System.currentTimeMillis() - oldRateLimit.timestamp()) > oldRateLimit.unit()) {
                return new CachedRateLimit(limitEntry.getValue(), 1);
            } else {
                return new CachedRateLimit(limitEntry.getValue(), oldRateLimit.amount() + 1, oldRateLimit.timestamp());
            }
        }
    }
}

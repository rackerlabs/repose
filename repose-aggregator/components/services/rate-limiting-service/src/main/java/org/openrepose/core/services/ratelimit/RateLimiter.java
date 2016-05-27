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
package org.openrepose.core.services.ratelimit;

import org.apache.commons.lang3.tuple.Pair;
import org.openrepose.core.services.ratelimit.cache.NextAvailableResponse;
import org.openrepose.core.services.ratelimit.cache.RateLimitCache;
import org.openrepose.core.services.ratelimit.config.ConfiguredRatelimit;
import org.openrepose.core.services.ratelimit.config.TimeUnit;
import org.openrepose.core.services.ratelimit.exception.CacheException;
import org.openrepose.core.services.ratelimit.exception.OverLimitException;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

/* Responsible for updating user rate limits and flagging if a user exceeds a limit */
public class RateLimiter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimiter.class);
    private final RateLimitCache cache;

    public RateLimiter(RateLimitCache cache) {
        this.cache = cache;
    }

    public void handleRateLimit(String user, List<Pair<String, ConfiguredRatelimit>> matchingLimits, TimeUnit largestUnit, int datastoreWarnLimit) throws OverLimitException {

        // Get the next, shortest available time that a user has to wait for
        try {
            NextAvailableResponse nextAvailable = cache.updateLimit(user, matchingLimits, largestUnit, datastoreWarnLimit);

            if (nextAvailable != null && !nextAvailable.hasRequestsRemaining()) {
                throw new OverLimitException("User rate limited!", user, nextAvailable.getResetTime(), nextAvailable.getCurrentLimitAmount(), nextAvailable.getLimitPair().getLeft().toString());
            }
        } catch (IOException ioe) {
            LOG.error("IOException caught during cache commit for rate limit user: " + user + " Reason: " + ioe.getMessage(), ioe);

            throw new CacheException("IOException caught during cache commit for rate limit.", ioe);
        }
    }
}

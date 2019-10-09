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
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.ratelimit.cache.util.TimeUnitConverter;
import org.openrepose.core.services.ratelimit.config.ConfiguredRatelimit;
import org.openrepose.core.services.ratelimit.config.TimeUnit;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* Responsible for updating and querying ratelimits in cache */
public class ManagedRateLimitCache implements RateLimitCache {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ManagedRateLimitCache.class);

    private final Datastore datastore;

    public ManagedRateLimitCache(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public Map<String, CachedRateLimit> getUserRateLimits(String user) {
        final Map<String, CachedRateLimit> accountRateLimitMap = getUserRateLimitMap(user);

        return Collections.unmodifiableMap(accountRateLimitMap);
    }

    private Map<String, CachedRateLimit> getUserRateLimitMap(String user) {
        final Object element = datastore.get(user);

        return (element == null) ? new HashMap<>() : ((UserRateLimit) element).getLimitMap();
    }

    @Override
    public NextAvailableResponse updateLimit(String user, List<Pair<String, ConfiguredRatelimit>> matchingLimits, TimeUnit largestUnit, int datastoreWarnLimit) throws IOException {
        UserRateLimit patchResult = datastore.patch(user, new UserRateLimit.Patch(matchingLimits), 1, TimeUnitConverter.fromSchemaTypeToConcurrent(largestUnit));

        if (patchResult.getLimitMap().keySet().size() >= datastoreWarnLimit) {
            LOG.warn("Large amount of limits recorded.  Repose Rate Limited may be misconfigured, keeping track of rate limits for user: " + user + ". Please review capture groups in your rate limit configuration.  If using clustered datastore, you may experience network latency.");
        }

        return new NextAvailableResponse(patchResult.getLowestLimit());
    }
}

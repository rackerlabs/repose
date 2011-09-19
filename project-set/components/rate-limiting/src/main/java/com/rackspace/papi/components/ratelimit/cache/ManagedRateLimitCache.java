package com.rackspace.papi.components.ratelimit.cache;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.components.limits.schema.HttpMethod;
import com.rackspace.papi.components.ratelimit.config.ConfiguredRatelimit;
import com.rackspace.papi.components.ratelimit.util.TimeUnitConverter;
import com.rackspace.papi.service.datastore.Datastore;

import com.rackspace.papi.service.datastore.StoredElement;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedRateLimitCache implements RateLimitCache {

    private final Datastore datastore;

    public ManagedRateLimitCache(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public Map<String, CachedRateLimit> getUserRateLimits(String account) {
        final HashMap<String, CachedRateLimit> accountRateLimitMap = getUserRateLimitMap(account);

        return Collections.unmodifiableMap(accountRateLimitMap);
    }
    
    private HashMap<String, CachedRateLimit> getUserRateLimitMap(String user) {
        final StoredElement element = datastore.get(user);
        
        return element.elementIsNull() ? new HashMap<String, CachedRateLimit>() : element.elementAs(HashMap.class);
    }

    @Override
    public NextAvailableResponse updateLimit(HttpMethod method, String user, String limitKey, ConfiguredRatelimit rateCfg) throws IOException {
        final HashMap<String, CachedRateLimit> userRateLimitMap = getUserRateLimitMap(user);

        CachedRateLimit currentLimit = userRateLimitMap != null ? userRateLimitMap.get(limitKey) : null;

        if (currentLimit == null) {
            currentLimit = new CachedRateLimit(rateCfg.getUriRegex());
            userRateLimitMap.put(limitKey, currentLimit);
        }
        
        final boolean hasRequests = currentLimit.amount(method) < rateCfg.getValue();

        if (hasRequests) {
            currentLimit.logHit(method, rateCfg.getUnit());
            datastore.put(user, ObjectSerializer.instance().writeObject(userRateLimitMap), 1, TimeUnitConverter.fromSchemaTypeToConcurrent(rateCfg.getUnit()));
        }

        return new NextAvailableResponse(hasRequests, new Date(currentLimit.getEarliestExpirationTime(method)));
    }
}

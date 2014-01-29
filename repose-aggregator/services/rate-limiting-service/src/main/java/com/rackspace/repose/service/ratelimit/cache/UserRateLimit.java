package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.papi.components.datastore.Patchable;
import com.rackspace.papi.components.datastore.distributed.SerializablePatch;
import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 1/28/14
 * Time: 9:33 AM
 */
public class UserRateLimit implements Serializable, Patchable<UserRateLimit, UserRateLimit.Patch> {

    private boolean withinLimit;
    private HashMap<String, CachedRateLimit> limitMap = new HashMap<String, CachedRateLimit>();

    public UserRateLimit(HashMap<String, CachedRateLimit> limitMap, boolean withinLimit) {
        this.limitMap = limitMap;
        this.withinLimit = withinLimit;
    }

    public HashMap<String, CachedRateLimit> getLimitMap() {
        return limitMap;
    }

    /**
     * Used when a patch has been done to describe if the patch was still within the limit.
     * @return true is the patch was successful
     */
    public boolean getWithinLimit() {
        return withinLimit;
    }

    @Override
    public UserRateLimit applyPatch(Patch in) {
        //todo: Write me
        throw new UnsupportedOperationException("com.rackspace.repose.service.ratelimit.cache.UserRateLimit.applyPatch hasn't been written yet");
    }

    public static class Patch implements SerializablePatch<UserRateLimit> {

        private String limitKey;
        private ConfiguredRatelimit configuredRateLimit;
        private HttpMethod method;

        public Patch(String limitKey, HttpMethod method, ConfiguredRatelimit configuredRateLimit) {
            this.limitKey = limitKey;
            this.configuredRateLimit = configuredRateLimit;
            this.method = method;
        }

        @Override
        public UserRateLimit newFromPatch() {
            HashMap<String, CachedRateLimit> newLimitMap = new HashMap<String, CachedRateLimit>();
            CachedRateLimit cachedRateLimit = new CachedRateLimit(configuredRateLimit.getUriRegex());
            cachedRateLimit.logHit(method, configuredRateLimit.getUnit());
            newLimitMap.put(limitKey, cachedRateLimit);
            return new UserRateLimit(newLimitMap, false);
        }
    }
}

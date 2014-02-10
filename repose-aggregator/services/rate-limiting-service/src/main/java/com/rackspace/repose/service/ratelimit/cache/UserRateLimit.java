package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.papi.components.datastore.Patchable;
import com.rackspace.papi.components.datastore.distributed.SerializablePatch;
import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 1/28/14
 * Time: 9:33 AM
 */
public class UserRateLimit implements Serializable, Patchable<UserRateLimit, UserRateLimit.Patch> {

    private HashMap<String, CachedRateLimit> limitMap = new HashMap<String, CachedRateLimit>();

    public UserRateLimit(HashMap<String, CachedRateLimit> limitMap) {
        this.limitMap = limitMap;
    }

    public HashMap<String, CachedRateLimit> getLimitMap() {
        return limitMap;
    }

    @Override
    public UserRateLimit applyPatch(Patch patch) {
        HashMap<String, CachedRateLimit> returnedLimitMap = new HashMap<String, CachedRateLimit>();
        CachedRateLimit cachedRateLimit = limitMap.get(patch.getLimitKey());
        if(cachedRateLimit == null) {
            cachedRateLimit = new CachedRateLimit(patch.getConfiguredRateLimit().getUriRegex());
            limitMap.put(patch.getLimitKey(), cachedRateLimit);
        }

        CachedRateLimit returnedRateLimit = new CachedRateLimit(patch.getConfiguredRateLimit().getUriRegex());
        returnedLimitMap.put(patch.getLimitKey(), returnedRateLimit);

        for(HttpMethod method : patch.getConfiguredRateLimit().getHttpMethods()) {
            if(cachedRateLimit.amount(method) < patch.getConfiguredRateLimit().getValue()) {
                cachedRateLimit.logHit(method, patch.getConfiguredRateLimit().getUnit());
                returnedRateLimit.getUsageMap().put(method, new Vector<Long>(cachedRateLimit.getUsageMap().get(method)));
            }
        }

        return new UserRateLimit(returnedLimitMap);
    }

    public static class Patch implements SerializablePatch<UserRateLimit> {

        private String limitKey;
        private ConfiguredRatelimit configuredRateLimit;

        public Patch(String limitKey, ConfiguredRatelimit configuredRateLimit) {
            this.limitKey = limitKey;
            this.configuredRateLimit = configuredRateLimit;
        }

        @Override
        public UserRateLimit newFromPatch() {
            HashMap<String, CachedRateLimit> newLimitMap = new HashMap<String, CachedRateLimit>();
            CachedRateLimit cachedRateLimit = new CachedRateLimit(configuredRateLimit.getUriRegex());
            if(configuredRateLimit.getValue() > 0) {
                for(HttpMethod method : configuredRateLimit.getHttpMethods()) {
                    cachedRateLimit.logHit(method, configuredRateLimit.getUnit());
                }
            }
            newLimitMap.put(limitKey, cachedRateLimit);
            return new UserRateLimit(newLimitMap);
        }

        public String getLimitKey() {
            return limitKey;
        }

        public ConfiguredRatelimit getConfiguredRateLimit() {
            return configuredRateLimit;
        }
    }
}

package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.papi.components.datastore.Patchable;
import com.rackspace.papi.components.datastore.distributed.SerializablePatch;

import java.io.Serializable;
import java.util.HashMap;

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
    public UserRateLimit applyPatch(Patch in) {
        //todo: Write me
        throw new UnsupportedOperationException("com.rackspace.repose.service.ratelimit.cache.UserRateLimit.applyPatch hasn't been written yet");
    }

    public static class Patch implements SerializablePatch<UserRateLimit> {
        @Override
        public UserRateLimit newFromPatch() {
            //todo: Write me
            throw new UnsupportedOperationException("com.rackspace.repose.service.ratelimit.cache.UserRateLimit.Patch.newFromPatch hasn't been written yet");
        }
    }
}

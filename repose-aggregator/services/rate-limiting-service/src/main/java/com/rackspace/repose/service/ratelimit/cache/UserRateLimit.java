package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.papi.components.datastore.Patchable;
import com.rackspace.papi.components.datastore.distributed.SerializablePatch;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 1/28/14
 * Time: 9:33 AM
 */
public class UserRateLimit implements Serializable, Patchable<UserRateLimit, UserRateLimit.Patch> {

    private final Pair<ConfiguredRatelimit, CachedRateLimit> leastRemainingLimit;

    private ConcurrentHashMap<String, CachedRateLimit> limitMap = new ConcurrentHashMap<String, CachedRateLimit>();

    public UserRateLimit() {
        this.limitMap = new ConcurrentHashMap<String, CachedRateLimit>();
        this.leastRemainingLimit = null;
    }

    public UserRateLimit(Map<String, CachedRateLimit> limitMap) {
        this.limitMap = new ConcurrentHashMap<String, CachedRateLimit>(limitMap);
        this.leastRemainingLimit = null;
    }

    private UserRateLimit(Map<String, CachedRateLimit> limitMap, Pair<ConfiguredRatelimit, CachedRateLimit> lowestLimit) {
        this.limitMap = new ConcurrentHashMap<String, CachedRateLimit>(limitMap);
        this.leastRemainingLimit = lowestLimit;
    }

    public ConcurrentHashMap<String, CachedRateLimit> getLimitMap() {
        return limitMap;
    }

    public Pair<ConfiguredRatelimit, CachedRateLimit> getLowestLimit() {
        return leastRemainingLimit;
    }

    @Override
    public UserRateLimit applyPatch(Patch patch) {
        HashMap<String, CachedRateLimit> returnLimits = new HashMap<String, CachedRateLimit>();
        Pair<ConfiguredRatelimit, CachedRateLimit> lowestLimit = null;

        for (Pair<String, ConfiguredRatelimit> limitEntry : patch.getLimitMap()) {
            CachedRateLimit rateLimit = adjustLimit(limitEntry);
            returnLimits.put(limitEntry.getKey(), rateLimit);
            if (lowestLimit == null || (rateLimit.maxAmount() - rateLimit.amount() < lowestLimit.getValue().maxAmount() - lowestLimit.getValue().amount())) {
                lowestLimit = Pair.of(limitEntry.getValue(), rateLimit);
            }
            if (rateLimit.amount() > rateLimit.maxAmount()) break;
        }

        return new UserRateLimit(returnLimits, lowestLimit);
    }

    private CachedRateLimit adjustLimit(Pair<String, ConfiguredRatelimit> limitEntry) {
        CachedRateLimit returnRateLimit;

        while (true) {
            CachedRateLimit newRateLimit = new CachedRateLimit(limitEntry.getValue(), 1);
            CachedRateLimit oldRateLimit = limitMap.putIfAbsent(limitEntry.getKey(), newRateLimit);

            if (oldRateLimit == null) { oldRateLimit = newRateLimit; }

            if (oldRateLimit == newRateLimit || ((System.currentTimeMillis() - oldRateLimit.timestamp()) > oldRateLimit.unit())) {
                returnRateLimit = newRateLimit;
            } else {
                returnRateLimit = new CachedRateLimit(limitEntry.getValue(), oldRateLimit.amount() + 1, oldRateLimit.timestamp());
            }

            if ((oldRateLimit == returnRateLimit) || limitMap.replace(limitEntry.getKey(), oldRateLimit, returnRateLimit)) {
                return returnRateLimit;
            }
        }
    }

    public static class Patch implements SerializablePatch<UserRateLimit> {

        private List< Pair<String, ConfiguredRatelimit> > limitMap;

        public Patch(List< Pair<String, ConfiguredRatelimit> > patchMap) {
            this.limitMap = new ArrayList< Pair<String, ConfiguredRatelimit> >(patchMap);
        }

        @Override
        public UserRateLimit newFromPatch() {
            UserRateLimit returnLimit = new UserRateLimit();

            returnLimit.applyPatch(this);

            return returnLimit;
        }

        public List< Pair<String, ConfiguredRatelimit> > getLimitMap() {
            return limitMap;
        }
    }
}

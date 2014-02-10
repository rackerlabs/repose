package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.limits.schema.TimeUnit;
import com.rackspace.repose.service.ratelimit.cache.util.TimeUnitConverter;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Vector;

/**
 *
 * @author jhopper
 */
public class CachedRateLimit implements Serializable {
    private final Map<HttpMethod, Vector<Long>> usageMap;
    private final int regexHashcode;

    public CachedRateLimit(String regex) {
        this.regexHashcode = regex.hashCode();
        this.usageMap = new EnumMap<HttpMethod, Vector<Long>>(HttpMethod.class);
    }

    public long now() {
        return System.currentTimeMillis();
    }

    public int getRegexHashcode() {
        return regexHashcode;
    }

    public Map<HttpMethod, Vector<Long>> getUsageMap() {
        return usageMap;
    }

    private void vacuum() {
        final long now = now();

        for (Map.Entry<HttpMethod, Vector<Long>> entry : usageMap.entrySet()) {
            final Vector<Long> usageQueue = entry.getValue();

            while (!usageQueue.isEmpty() && usageQueue.get(0) < now) {
                usageQueue.remove(0);
            }
        }
    }

    public void logHit(HttpMethod method, long time) {
        Vector<Long> usageQueue = usageMap.get(method);

        if (usageQueue == null) {
            usageQueue = new Vector<Long>();
            usageMap.put(method, usageQueue);
        }

        usageQueue.add(time);
    }

    public void logHit(HttpMethod method, TimeUnit timeInterval) {
        vacuum();

        logHit(method, now() + TimeUnitConverter.toLong(timeInterval));
    }

    public int amount(HttpMethod method) {
        vacuum();

        final Vector<Long> usageQueue = usageMap.get(method);
        return usageQueue != null ? usageQueue.size() : 0;
    }

    public long getEarliestExpirationTime(HttpMethod method) {
        vacuum();

        final Vector<Long> usageQueue = usageMap.get(method);
        return usageQueue != null && !usageQueue.isEmpty() ? usageQueue.get(0) : now();
    }
}

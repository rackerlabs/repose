package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.limits.schema.TimeUnit;
import com.rackspace.repose.service.ratelimit.cache.util.TimeUnitConverter;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 * @author jhopper
 */
public class CachedRateLimit implements Serializable {
    private final Map<HttpMethod, LinkedList<Long>> usageMap;
    private final int regexHashcode;

    public CachedRateLimit(String regex) {
        this.regexHashcode = regex.hashCode();
        this.usageMap = new EnumMap<HttpMethod, LinkedList<Long>>(HttpMethod.class);
    }

    public long now() {
        return System.currentTimeMillis();
    }

    public int getRegexHashcode() {
        return regexHashcode;
    }

    private void vacuum() {
        final long now = now();

        for (Map.Entry<HttpMethod, LinkedList<Long>> entry : usageMap.entrySet()) {
            final LinkedList<Long> usageQueue = entry.getValue();

            while (!usageQueue.isEmpty() && usageQueue.peek() < now) {
                usageQueue.poll();
            }
        }
    }

    public void logHit(HttpMethod method, long time) {
        LinkedList<Long> usageQueue = usageMap.get(method);

        if (usageQueue == null) {
            usageQueue = new LinkedList<Long>();
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

        final LinkedList<Long> usageQueue = usageMap.get(method);
        return usageQueue != null ? usageQueue.size() : 0;
    }

    public long getEarliestExpirationTime(HttpMethod method) {
        vacuum();

        final LinkedList<Long> usageQueue = usageMap.get(method);
        return usageQueue != null && !usageQueue.isEmpty() ? usageQueue.peek() : now();
    }

    public long getLatestExpirationTime(HttpMethod method) {
        vacuum();

        final LinkedList<Long> usageQueue = usageMap.get(method);
        return usageQueue != null && !usageQueue.isEmpty() ? usageQueue.peekLast() : now();
    }
}

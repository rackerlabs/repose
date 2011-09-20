package com.rackspace.papi.components.ratelimit.cache;

import com.rackspace.papi.components.limits.schema.HttpMethod;
import com.rackspace.papi.components.limits.schema.TimeUnit;
import com.rackspace.papi.components.ratelimit.util.TimeUnitConverter;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 * @author jhopper
 */
public class CachedRateLimit implements Serializable {
    private final Map<HttpMethod, LinkedList<LoggedRequest>> usageMap;
    private final int regexHashcode;

    public CachedRateLimit(String regex) {
        this.regexHashcode = regex.hashCode();
        this.usageMap = new EnumMap<HttpMethod, LinkedList<LoggedRequest>>(HttpMethod.class);
    }

    public long now() {
        return System.currentTimeMillis();
    }

    public int getRegexHashcode() {
        return regexHashcode;
    }

    private void vacuum() {
        final long now = now();

        for (Map.Entry<HttpMethod, LinkedList<LoggedRequest>> entry : usageMap.entrySet()) {
            final LinkedList<LoggedRequest> usageQueue = entry.getValue();

            while (!usageQueue.isEmpty() && usageQueue.peek().getTimestamp() < now) {
                usageQueue.poll();
            }
        }
    }

    public void logHit(HttpMethod method, long time) {
        LinkedList<LoggedRequest> usageQueue = usageMap.get(method);

        if (usageQueue == null) {
            usageQueue = new LinkedList<LoggedRequest>();
            usageMap.put(method, usageQueue);
        }

        usageQueue.add(new LoggedRequest(time));
    }

    public void logHit(HttpMethod method, TimeUnit timeInterval) {
        vacuum();

        logHit(method, now() + TimeUnitConverter.toLong(timeInterval));
    }

    public int amount(HttpMethod method) {
        vacuum();

        final LinkedList<LoggedRequest> usageQueue = usageMap.get(method);
        return usageQueue != null ? usageQueue.size() : 0;
    }

    public long getEarliestExpirationTime(HttpMethod method) {
        vacuum();

        final LinkedList<LoggedRequest> usageQueue = usageMap.get(method);
        return usageQueue != null && !usageQueue.isEmpty() ? usageQueue.peek().getTimestamp() : now();
    }

    public long getLatestExpirationTime(HttpMethod method) {
        vacuum();

        final LinkedList<LoggedRequest> usageQueue = usageMap.get(method);
        return usageQueue != null && !usageQueue.isEmpty() ? usageQueue.peekLast().getTimestamp() : now();
    }
}

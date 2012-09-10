package com.rackspace.papi.service.reporting.destinations;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

public class DestinationInfoStore {

    private static final long LONG_ZERO = 0l;
    private final String destinationId;
    private final long startTime;
    private long totalRequests = LONG_ZERO;
    private long totalResponses = LONG_ZERO;
    private Map<Integer, Long> statusCodeCounts = new HashMap<Integer, Long>();
    private long accumulatedResponseTime = LONG_ZERO;

    public DestinationInfoStore(String destinationId) {
        this.destinationId = destinationId;
        this.startTime = System.currentTimeMillis();
    }

    public DestinationInfoStore(DestinationInfoStore destinationInfoStore) {
        this(destinationInfoStore.destinationId, destinationInfoStore.startTime,
             destinationInfoStore.totalRequests, destinationInfoStore.totalResponses,
             destinationInfoStore.statusCodeCounts, destinationInfoStore.accumulatedResponseTime);
    }

    private DestinationInfoStore(String destinationId, long startTime, long totalRequests, long totalResponses,
                                 Map<Integer, Long> statusCodeCounts, long accumulatedResponseTime) {
        this.destinationId = destinationId;
        this.startTime = startTime;
        this.totalRequests = totalRequests;
        this.totalResponses = totalResponses;
        this.statusCodeCounts = deepCopyStatusCodeCounts(statusCodeCounts);
        this.accumulatedResponseTime = accumulatedResponseTime;
    }
    
    private static Map<Integer, Long> deepCopyStatusCodeCounts(Map<Integer, Long> statusCodeCounts) {
        return ImmutableMap.copyOf(statusCodeCounts);
    }

    public String getDestinationId() {
        return destinationId;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public long getTotalResponses() {
        return totalResponses;
    }

    public long getAccumulatedResponseTime() {
        return accumulatedResponseTime;
    }

    protected void setTotalResponses(long totalResponses) {
        this.totalResponses = totalResponses;
    }

    protected Map<Integer, Long> getStatusCodeCounts() {
        return statusCodeCounts;
    }

    protected void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    protected void setAccumulatedResponseTime(long accumulatedResponseTime) {
        this.accumulatedResponseTime = accumulatedResponseTime;
    }    

    @Override
    public boolean equals(Object o) {

        if (o == this) {
            return true;
        }

        if (o instanceof DestinationInfoStore) {
            DestinationInfoStore other = (DestinationInfoStore) o;

            return Objects.equal(this.destinationId, other.destinationId) &&
                   Objects.equal(this.accumulatedResponseTime, other.accumulatedResponseTime) &&
                   Objects.equal(this.startTime, other.startTime) &&
                   Objects.equal(this.totalRequests, other.totalRequests) &&
                   Objects.equal(this.totalResponses, other.totalResponses) &&
                   Objects.equal(this.statusCodeCounts, other.statusCodeCounts);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(destinationId, accumulatedResponseTime, startTime, totalRequests, totalResponses, statusCodeCounts);
    }
}

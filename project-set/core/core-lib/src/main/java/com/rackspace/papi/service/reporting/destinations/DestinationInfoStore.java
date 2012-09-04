package com.rackspace.papi.service.reporting.destinations;

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

    protected DestinationInfoStore(DestinationInfoStore destinationInfoStore) {
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
        final Map<Integer, Long> newStatusCodeCounts = new HashMap<Integer, Long>();

        for (Map.Entry<Integer, Long> entry : statusCodeCounts.entrySet()) {
            newStatusCodeCounts.put(entry.getKey(), entry.getValue());
        }

        return newStatusCodeCounts;
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

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        DestinationInfoStore that = (DestinationInfoStore) o;

        if (accumulatedResponseTime != that.accumulatedResponseTime) {
            return false;
        }

        if (startTime != that.startTime) {
            return false;
        }

        if (totalRequests != that.totalRequests) {
            return false;
        }

        if (totalResponses != that.totalResponses) {
            return false;
        }

        if (!destinationId.equals(that.destinationId)) {
            return false;
        }

        if (!statusCodeCounts.equals(that.statusCodeCounts)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = destinationId.hashCode();

        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + (int) (totalRequests ^ (totalRequests >>> 32));
        result = 31 * result + (int) (totalResponses ^ (totalResponses >>> 32));
        result = 31 * result + statusCodeCounts.hashCode();
        result = 31 * result + (int) (accumulatedResponseTime ^ (accumulatedResponseTime >>> 32));

        return result;
    }
}

package com.rackspace.papi.service.reporting.repose;

import java.util.HashMap;
import java.util.Map;

public class ReposeInfoStore {

    private static final long LONG_ZERO = 0l;
    private Map<Integer, Long> statusCodeCounts = new HashMap<Integer, Long>();
    private long totalRequests = LONG_ZERO;
    private long totalResponses = LONG_ZERO;
    long accumulatedRequestSize = LONG_ZERO;
    long accumulatedResponseSize = LONG_ZERO;
    private long minRequestSize = LONG_ZERO;
    private long maxRequestSize = LONG_ZERO;
    private long minResponseSize = LONG_ZERO;
    private long maxResponseSize = LONG_ZERO;


    public ReposeInfoStore() {
    }

    protected ReposeInfoStore(ReposeInfoStore reposeInfoStore) {
        this.statusCodeCounts = deepCopyStatusCodeCounts(reposeInfoStore.statusCodeCounts);
    }

    private Map<Integer, Long> deepCopyStatusCodeCounts(Map<Integer, Long> statusCodeCounts) {
        Map<Integer, Long> newStatusCodeCounts = new HashMap<Integer, Long>();

        for (Map.Entry<Integer, Long> entry : statusCodeCounts.entrySet()) {
            newStatusCodeCounts.put(entry.getKey(), entry.getValue());
        }

        return newStatusCodeCounts;
    }

    public Map<Integer, Long> getStatusCodeCounts() {
        return statusCodeCounts;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public long getTotalResponses() {
        return totalResponses;
    }

    public long getAccumulatedRequestSize() {
        return accumulatedRequestSize;
    }

    public long getAccumulatedResponseSize() {
        return accumulatedResponseSize;
    }

    public long getMinRequestSize() {
        return minRequestSize;
    }

    public long getMaxRequestSize() {
        return maxRequestSize;
    }

    public long getMinResponseSize() {
        return minResponseSize;
    }

    public long getMaxResponseSize() {
        return maxResponseSize;
    }

    protected void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    protected void setTotalResponses(long totalResponses) {
        this.totalResponses = totalResponses;
    }

    protected void setAccumulatedRequestSize(long accumulatedRequestSize) {
        this.accumulatedRequestSize = accumulatedRequestSize;
    }

    protected void setAccumulatedResponseSize(long accumulatedResponseSize) {
        this.accumulatedResponseSize = accumulatedResponseSize;
    }

    protected void setMinRequestSize(long minRequestSize) {
        this.minRequestSize = minRequestSize;
    }

    protected void setMaxRequestSize(long maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    protected void setMinResponseSize(long minResponseSize) {
        this.minResponseSize = minResponseSize;
    }

    protected void setMaxResponseSize(long maxResponseSize) {
        this.maxResponseSize = maxResponseSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ReposeInfoStore)) {
            return false;
        }

        ReposeInfoStore that = (ReposeInfoStore) o;

        if (maxRequestSize != that.maxRequestSize) {
            return false;
        }

        if (maxResponseSize != that.maxResponseSize) {
            return false;
        }

        if (minRequestSize != that.minRequestSize) {
            return false;
        }

        if (minResponseSize != that.minResponseSize) {
            return false;
        }

        if (accumulatedRequestSize != that.accumulatedRequestSize) {
            return false;
        }

        if (totalRequests != that.totalRequests) {
            return false;
        }

        if (accumulatedResponseSize != that.accumulatedResponseSize) {
            return false;
        }

        if (totalResponses != that.totalResponses) {
            return false;
        }

        if (!statusCodeCounts.equals(that.statusCodeCounts)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = statusCodeCounts.hashCode();

        result = 31 * result + (int) (totalRequests ^ (totalRequests >>> 32));
        result = 31 * result + (int) (totalResponses ^ (totalResponses >>> 32));
        result = 31 * result + (int) (accumulatedRequestSize ^ (accumulatedRequestSize >>> 32));
        result = 31 * result + (int) (accumulatedResponseSize ^ (accumulatedResponseSize >>> 32));
        result = 31 * result + (int) (minRequestSize ^ (minRequestSize >>> 32));
        result = 31 * result + (int) (maxRequestSize ^ (maxRequestSize >>> 32));
        result = 31 * result + (int) (minResponseSize ^ (minResponseSize >>> 32));
        result = 31 * result + (int) (maxResponseSize ^ (maxResponseSize >>> 32));

        return result;
    }
}

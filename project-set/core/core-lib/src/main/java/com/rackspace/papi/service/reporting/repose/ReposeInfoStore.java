package com.rackspace.papi.service.reporting.repose;

import com.rackspace.papi.service.reporting.StatusCodeResponseStore;
import java.util.HashMap;
import java.util.Map;

public class ReposeInfoStore {

    private static final long LONG_ZERO = 0l;
    private Map<Integer, StatusCodeResponseStore> statusCodeCounts = new HashMap<Integer, StatusCodeResponseStore>();
    private long totalRequests = LONG_ZERO;
    private long totalResponses = LONG_ZERO;
    private long accumulatedRequestSize = LONG_ZERO;
    private long accumulatedResponseSize = LONG_ZERO;
    private long minRequestSize = LONG_ZERO;
    private long maxRequestSize = LONG_ZERO;
    private long minResponseSize = LONG_ZERO;
    private long maxResponseSize = LONG_ZERO;

    public ReposeInfoStore() {
    }

    protected ReposeInfoStore(ReposeInfoStore reposeInfoStore) {
        this.statusCodeCounts = deepCopyStatusCodeCounts(reposeInfoStore.statusCodeCounts);
    }

    private Map<Integer, StatusCodeResponseStore> deepCopyStatusCodeCounts(Map<Integer, StatusCodeResponseStore> statusCodeCounts) {
        Map<Integer, StatusCodeResponseStore> copy = new HashMap<Integer, StatusCodeResponseStore>();
        for (Map.Entry<Integer, StatusCodeResponseStore> entry: statusCodeCounts.entrySet()) {
            copy.put(entry.getKey(), new StatusCodeResponseStore(entry.getValue()));
        }
        
        return copy;
    }

    public Map<Integer, StatusCodeResponseStore> getStatusCodeCounts() {
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
    
    protected void processRequestSize(long requestSize) {
        this.accumulatedRequestSize += requestSize;
        if (requestSize < minRequestSize || minRequestSize == 0) {
            minRequestSize = requestSize;
        }
        if (requestSize > maxRequestSize) {
            maxRequestSize = requestSize;
        }
    }

    protected void processResponseSize(long responseSize) {
        this.accumulatedResponseSize += responseSize;
        if (responseSize < minResponseSize || minResponseSize == 0) {
            minResponseSize = responseSize;
        }
        if (responseSize > maxResponseSize) {
            maxResponseSize = responseSize;
        }
    }

}

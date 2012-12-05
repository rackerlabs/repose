package com.rackspace.papi.service.reporting;

public class StatusCodeResponseStore {

    private static final long LONG_ZERO = 0l;
    private long totalCount = LONG_ZERO;
    private long accumulatedResponseTime = LONG_ZERO;

    public StatusCodeResponseStore(StatusCodeResponseStore store) {
        this.totalCount = store.totalCount;
        this.accumulatedResponseTime = store.accumulatedResponseTime;
    }

    public StatusCodeResponseStore(long totalCount, long accumulatedResponseTime) {
        this.totalCount = totalCount;
        this.accumulatedResponseTime = accumulatedResponseTime;
    }

    public StatusCodeResponseStore update(long count, long time) {
        this.totalCount += count;
        this.accumulatedResponseTime += time;
        return this;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public Long getAccumulatedResponseTime() {
        return accumulatedResponseTime;
    }
    private static final int HASH = 67;
    private static final int SHIFT = 32;

    @Override
    public int hashCode() {
        int hash = HASH;
        hash = HASH * hash + (int) (this.totalCount ^ (this.totalCount >>> SHIFT));
        hash = HASH * hash + (int) (this.accumulatedResponseTime ^ (this.accumulatedResponseTime >>> SHIFT));
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StatusCodeResponseStore)) {
            return false;
        }

        StatusCodeResponseStore other = (StatusCodeResponseStore) o;

        return other.accumulatedResponseTime == accumulatedResponseTime && other.totalCount == totalCount;
    }
}

package com.rackspace.papi.service.reporting.repose;

public interface ReposeInfo {

    long getTotalStatusCode(int statusCode);
    void incrementStatusCodeCount(int statusCode, long time);
    void incrementRequestCount();
    void incrementResponseCount();
    void processRequestSize(long requestSize);
    void processResponseSize(long responseSize);
    /*
    void accumulateRequestSize(long requestSize);
    void updateMinMaxRequestSize(long requestSize);
    void accumulateResponseSize(long responseSize);
    void updateMinMaxResponseSize(long responseSize);
    */
    long getMinimumRequestSize();
    long getMaximumRequestSize();
    long getMinimumResponseSize();
    long getMaximumResponseSize();
    double getAverageRequestSize();
    double getAverageResponseSize();

    ReposeInfo copy();
}

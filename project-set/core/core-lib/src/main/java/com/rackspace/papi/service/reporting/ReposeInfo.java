package com.rackspace.papi.service.reporting;

public interface ReposeInfo {

    long getTotalStatusCode(int statusCode);
    void incrementStatusCodeCount(int statusCode, long time);
    void incrementRequestCount();
    void incrementResponseCount();
    void processRequestSize(long requestSize);
    void processResponseSize(long responseSize);
    long getMinimumRequestSize();
    long getMaximumRequestSize();
    long getMinimumResponseSize();
    long getMaximumResponseSize();
    double getAverageRequestSize();
    double getAverageResponseSize();

    ReposeInfo copy();
}

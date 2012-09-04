package com.rackspace.papi.service.reporting;

import com.rackspace.papi.service.reporting.destinations.DestinationInfo;
import com.rackspace.papi.service.reporting.repose.ReposeInfo;

public interface ReportingService {

    DestinationInfo getDestinationInfo(String destinationId);
    void incrementRequestCount(String destinationId);
    void incrementResponseCount(String destinationId);
    void incrementDestinationStatusCodeCount(String destinationId, int statusCode);
    void accumulateResponseTime(String destinationId, long responseTime);   

    ReposeInfo getReposeInfo();
    void incrementReposeStatusCodeCount(int statusCode);
    void incrementReposeRequestCount();
    void incrementReposeResponseCount();
    void accumulateReposeRequestSize(long requestSize);
    void accumulateReposeResponseSize(long responseSize);
}

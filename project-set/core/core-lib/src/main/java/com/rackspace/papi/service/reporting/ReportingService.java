package com.rackspace.papi.service.reporting;

import com.rackspace.papi.service.reporting.destinations.DestinationInfo;
import com.rackspace.papi.service.reporting.repose.ReposeInfo;

import java.util.Date;
import java.util.List;

public interface ReportingService {

    void shutdown();
    void updateConfiguration(List<String> destinationIds, int seconds);
    Date getLastReset();

    DestinationInfo getDestinationInfo(String destinationId);
    List<DestinationInfo> getDestinations();
    void recordServiceResponse(String destinationId, int statusCode, long responseTime);
    void incrementRequestCount(String destinationId);
    //void incrementResponseCount(String destinationId);
    //void incrementDestinationStatusCodeCount(String destinationId, int statusCode);
    //void accumulateResponseTime(String destinationId, long responseTime);   

    ReposeInfo getReposeInfo();
    void incrementReposeStatusCodeCount(int statusCode, long time);
    void incrementReposeRequestCount();
    void incrementReposeResponseCount();
    void processReposeRequestSize(long requestSize);
    void processReposeResponseSize(long responseSize);
    //void accumulateReposeRequestSize(long requestSize);
    //void accumulateReposeResponseSize(long responseSize);
}

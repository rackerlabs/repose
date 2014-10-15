package org.openrepose.core.services.reporting;

import org.openrepose.core.services.reporting.destinations.DestinationInfo;

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

    ReposeInfo getReposeInfo();
    void incrementReposeStatusCodeCount(int statusCode, long time);
    void incrementReposeRequestCount();
    void incrementReposeResponseCount();
    void processReposeRequestSize(long requestSize);
    void processReposeResponseSize(long responseSize);
}

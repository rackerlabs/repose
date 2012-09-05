package com.rackspace.papi.service.reporting;

import com.rackspace.papi.service.reporting.destinations.DestinationInfo;
import com.rackspace.papi.service.reporting.destinations.DestinationInfoLogic;
import com.rackspace.papi.service.reporting.repose.ReposeInfo;
import com.rackspace.papi.service.reporting.repose.ReposeInfoLogic;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("reportingService")
public class ReportingServiceImpl implements ReportingService {

    private Map<String, DestinationInfo> destinations = new HashMap<String, DestinationInfo>();
    private ReposeInfo reposeInfo;
    private Timer timer;

    public ReportingServiceImpl() {
    }

    @Override
    public synchronized void updateConfiguration(List<String> destinationIds, int seconds) {
        for (String id : destinationIds) {
            final DestinationInfo destinationInfo = new DestinationInfoLogic(id);
            destinations.put(id, destinationInfo);
        }

        reposeInfo = new ReposeInfoLogic();

        timer = new Timer();
        long initialDelayInMilliseconds = seconds * 1000;
        timer.scheduleAtFixedRate(new ReportingTimerTask(), initialDelayInMilliseconds, seconds * 1000);
    }

    @Override
    public synchronized DestinationInfo getDestinationInfo(String destinationId) {
        return destinations.get(destinationId).copy();
    }

    @Override
    public List<DestinationInfo> getDestinations() {
        final List<DestinationInfo> newDestinations = new ArrayList<DestinationInfo>();

        for (Map.Entry<String, DestinationInfo> entry : destinations.entrySet()) {
            newDestinations.add(entry.getValue().copy());
        }        

        return newDestinations;
    }

    @Override
    public synchronized void incrementRequestCount(String destinationId) {
        destinations.get(destinationId).incrementRequestCount();
    }

    @Override
    public synchronized void incrementResponseCount(String destinationId) {
        destinations.get(destinationId).incrementResponseCount();
    }

    @Override
    public synchronized void incrementDestinationStatusCodeCount(String destinationId, int statusCode) {
        destinations.get(destinationId).incrementStatusCodeCount(statusCode);
    }

    @Override
    public synchronized void accumulateResponseTime(String destinationId, long responseTime) {
        destinations.get(destinationId).accumulateResponseTime(responseTime);
    }

    @Override
    public synchronized ReposeInfo getReposeInfo() {
        return reposeInfo.copy();
    }

    @Override
    public synchronized void incrementReposeStatusCodeCount(int statusCode) {
        reposeInfo.incrementStatusCodeCount(statusCode);
    }

    @Override
    public synchronized void incrementReposeRequestCount() {
        reposeInfo.incrementRequestCount();
    }

    @Override
    public synchronized void incrementReposeResponseCount() {
        reposeInfo.incrementResponseCount();
    }

    @Override
    public synchronized void accumulateReposeRequestSize(long requestSize) {
        reposeInfo.accumulateRequestSize(requestSize);
    }

    @Override
    public synchronized void accumulateReposeResponseSize(long responseSize) {
        reposeInfo.accumulateResponseSize(responseSize);
    }

    private synchronized void reset() {
        final Map<String, DestinationInfo> newDestinations = new HashMap<String, DestinationInfo>();

        for (Map.Entry<String, DestinationInfo> entry : destinations.entrySet()) {
            final String destinationId = entry.getValue().getDestinationId();
            final DestinationInfo destinationInfo = new DestinationInfoLogic(destinationId);

            newDestinations.put(destinationId, destinationInfo);
        }

        destinations.clear();
        destinations = newDestinations;

        reposeInfo = new ReposeInfoLogic();
    }

    private class ReportingTimerTask extends TimerTask {

        @Override
        public void run() {
            reset();
        }
    }
}

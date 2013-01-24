package com.rackspace.papi.service.reporting.impl;

import com.rackspace.papi.service.reporting.ReportingService;
import com.rackspace.papi.service.reporting.destinations.DestinationInfo;
import com.rackspace.papi.service.reporting.destinations.impl.DestinationInfoLogic;
import com.rackspace.papi.service.reporting.ReposeInfo;
import com.rackspace.papi.service.reporting.repose.ReposeInfoLogic;
import java.util.*;
import org.springframework.stereotype.Component;

@Component("reportingService")
public class ReportingServiceImpl implements ReportingService {

    private static final String TIMER_THREAD_NAME = "Repose JMX Reset Timer Thread";
    private static final int ONE_THOUSAND = 1000;
    private final Map<String, DestinationInfo> destinations = new HashMap<String, DestinationInfo>();
    private ReposeInfo reposeInfo;
    private Date lastReset;
    private Timer timer;
    private ReportingTimerTask reportingTimerTask;

    public ReportingServiceImpl() {
        timer = new Timer(TIMER_THREAD_NAME);
        reportingTimerTask = new ReportingTimerTask();
    }

    @Override
    public synchronized Date getLastReset() {
        return (Date)lastReset.clone();
    }

    @Override
    public void shutdown() {
        timer.cancel();
    }

    @Override
    public synchronized void updateConfiguration(List<String> destinationIds, int seconds) {

        destinations.clear();
        for (String id : destinationIds) {
            final DestinationInfo destinationInfo = new DestinationInfoLogic(id);
            destinations.put(id, destinationInfo);
        }
        reposeInfo = new ReposeInfoLogic();

        manageTimer(seconds);
    }

    private void manageTimer(int seconds) {

        reportingTimerTask.cancel();
        timer.purge();

        reportingTimerTask = new ReportingTimerTask();
        long initialDelayInMilliseconds = seconds * ONE_THOUSAND;
        lastReset = new Date(System.currentTimeMillis());
        timer.scheduleAtFixedRate(reportingTimerTask, initialDelayInMilliseconds, seconds * ONE_THOUSAND);
    }

    @Override
    public synchronized DestinationInfo getDestinationInfo(String destinationId) {
        return destinations.get(destinationId).copy();
    }

    @Override
    public synchronized List<DestinationInfo> getDestinations() {
        final List<DestinationInfo> newDestinations = new ArrayList<DestinationInfo>();

        for (Map.Entry<String, DestinationInfo> entry : destinations.entrySet()) {
            newDestinations.add(entry.getValue().copy());
        }

        return newDestinations;
    }

    @Override
    public synchronized void incrementRequestCount(String destinationId) {
        if (destinations.get(destinationId) != null) {
            destinations.get(destinationId).incrementRequestCount();
        }
    }

    @Override
    public synchronized void recordServiceResponse(String destinationId, int statusCode, long responseTime) {
        incrementReposeResponseCount();
        if (destinations.get(destinationId) != null) {
            DestinationInfo destination = destinations.get(destinationId);
            destination.incrementResponseCount();
            destination.incrementStatusCodeCount(statusCode, responseTime);
            destination.accumulateResponseTime(responseTime);
        }
    }
    
    @Override
    public synchronized ReposeInfo getReposeInfo() {
        return reposeInfo.copy();
    }

    @Override
    public synchronized void incrementReposeStatusCodeCount(int statusCode, long time) {
        reposeInfo.incrementStatusCodeCount(statusCode, time);
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
    public synchronized void processReposeRequestSize(long requestSize) {
        reposeInfo.processRequestSize(requestSize);
    }

    @Override
    public synchronized void processReposeResponseSize(long responseSize) {
        reposeInfo.processResponseSize(responseSize);
    }

    private synchronized void reset() {
        final Map<String, DestinationInfo> newDestinations = new HashMap<String, DestinationInfo>();

        for (Map.Entry<String, DestinationInfo> entry : destinations.entrySet()) {
            final String destinationId = entry.getValue().getDestinationId();
            final DestinationInfo destinationInfo = new DestinationInfoLogic(destinationId);

            newDestinations.put(destinationId, destinationInfo);
        }

        destinations.clear();
        destinations.putAll(newDestinations);

        reposeInfo = new ReposeInfoLogic();
        lastReset = new Date(System.currentTimeMillis());
    }

    private class ReportingTimerTask extends TimerTask {

        @Override
        public void run() {
            reset();
        }
    }
}

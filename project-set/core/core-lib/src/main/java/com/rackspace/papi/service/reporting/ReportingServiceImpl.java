package com.rackspace.papi.service.reporting;

import com.rackspace.papi.service.reporting.destinations.DestinationInfo;
import com.rackspace.papi.service.reporting.destinations.DestinationInfoLogic;
import com.rackspace.papi.service.reporting.repose.ReposeInfo;
import com.rackspace.papi.service.reporting.repose.ReposeInfoLogic;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("reportingService")
public class ReportingServiceImpl implements ReportingService {

    private static final String TIMER_THREAD_NAME = "Repose JMX Reset Timer Thread";
    private static final int ONE_THOUSAND = 1000;
    private Map<String, DestinationInfo> destinations = new HashMap<String, DestinationInfo>();
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
        return lastReset;
    }
    
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
    public List<DestinationInfo> getDestinations() {
        final List<DestinationInfo> newDestinations = new ArrayList<DestinationInfo>();

        synchronized (destinations) {
            for (Map.Entry<String, DestinationInfo> entry : destinations.entrySet()) {
                newDestinations.add(entry.getValue().copy());
            }
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
    public synchronized void incrementResponseCount(String destinationId) {
        if (destinations.get(destinationId) != null) {
            destinations.get(destinationId).incrementResponseCount();
        }
    }

    @Override
    public synchronized void incrementDestinationStatusCodeCount(String destinationId, int statusCode) {

        if (destinations.get(destinationId) != null) {
            destinations.get(destinationId).incrementStatusCodeCount(statusCode);
        }
    }

    @Override
    public synchronized void accumulateResponseTime(String destinationId, long responseTime) {
        
        if (destinations.get(destinationId) != null) {
            destinations.get(destinationId).accumulateResponseTime(responseTime);
        }
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
            lastReset = new Date(System.currentTimeMillis());
        }
    }
}

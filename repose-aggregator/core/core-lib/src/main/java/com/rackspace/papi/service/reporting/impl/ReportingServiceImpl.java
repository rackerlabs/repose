package com.rackspace.papi.service.reporting.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.model.*;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.reporting.ReportingService;
import com.rackspace.papi.service.reporting.ReposeInfo;
import com.rackspace.papi.service.reporting.destinations.DestinationInfo;
import com.rackspace.papi.service.reporting.destinations.impl.DestinationInfoLogic;
import com.rackspace.papi.service.reporting.repose.ReposeInfoLogic;
import javax.inject.Inject;
import javax.inject.Named;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.URL;
import java.util.*;

@Named
public class ReportingServiceImpl implements ReportingService {
    private static final String TIMER_THREAD_NAME = "Repose JMX Reset Timer Thread";
    private static final int ONE_THOUSAND = 1000;
    private static final int DEFAULT_JMX_RESET_TIME_SECONDS = 15;

    private final Object jmxResetTimeKey = new Object();
    private final List<String> destinationIds = new ArrayList<>();
    private final Map<String, DestinationInfo> destinations = new HashMap<>();
    private final SystemModelListener systemModelListener = new SystemModelListener();
    private final ContainerConfigurationListener containerConfigurationListener = new ContainerConfigurationListener();
    private final ConfigurationService configurationService;

    private int jmxResetTime = DEFAULT_JMX_RESET_TIME_SECONDS;
    private ReposeInfo reposeInfo;
    private Date lastReset;
    private Timer timer;
    private ReportingTimerTask reportingTimerTask;

    @Inject
    public ReportingServiceImpl(ConfigurationService configurationService) {
        this.configurationService = configurationService;

        timer = new Timer(TIMER_THREAD_NAME);
        reportingTimerTask = new ReportingTimerTask();
    }

    @PostConstruct
    public void afterPropertiesSet() {
        URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        URL containerXsdURL = getClass().getResource("/META-INF/schema/container/container-configuration.xsd");
        configurationService.subscribeTo("system-model.cfg.xml", xsdURL, systemModelListener, SystemModel.class);
        configurationService.subscribeTo("container.cfg.xml", containerXsdURL, containerConfigurationListener, ContainerConfiguration.class);
    }

    @PreDestroy
    public void destroy() {
        shutdown();
        configurationService.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
        configurationService.unsubscribeFrom("container.cfg.xml", containerConfigurationListener);
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

    /**
     * Listens for updates to the container.cfg.xml file which holds the jmx-reset-time.
     */
    private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {
        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContainerConfiguration configurationObject) {
            if (configurationObject.getDeploymentConfig() != null) {
                synchronized (jmxResetTimeKey) {
                    jmxResetTime = configurationObject.getDeploymentConfig().getJmxResetTime();
                }

                updateConfiguration(destinationIds, jmxResetTime);
            }

            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    /**
     * Listens for updates to the system-model.cfg.xml file which holds the destination ids.
     */
    private class SystemModelListener implements UpdateListener<SystemModel> {
        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(SystemModel systemModel) {
            final List<String> endpointIds = new ArrayList<String>();

            for (ReposeCluster reposeCluster : systemModel.getReposeCluster()) {
                final DestinationList destinations = reposeCluster.getDestinations();

                for (DestinationEndpoint endpoint : destinations.getEndpoint()) {
                    endpointIds.add(endpoint.getId());
                }

                for (DestinationCluster destinationCluster : destinations.getTarget()) {
                    endpointIds.add(destinationCluster.getId());
                }
            }

            synchronized (destinationIds) {
                destinationIds.clear();
                destinationIds.addAll(endpointIds);
            }

            updateConfiguration(destinationIds, jmxResetTime);
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}

/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.reporting.impl;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.reporting.ReportingService;
import org.openrepose.core.services.reporting.ReposeInfo;
import org.openrepose.core.services.reporting.destinations.DestinationInfo;
import org.openrepose.core.services.reporting.destinations.impl.DestinationInfoLogic;
import org.openrepose.core.services.reporting.repose.ReposeInfoLogic;
import org.openrepose.core.systemmodel.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Named
public class ReportingServiceImpl implements ReportingService {

    private static final String TIMER_THREAD_NAME = "Repose JMX Reset Timer Thread";
    private static final int DEFAULT_JMX_RESET_TIME_SECONDS = 15;

    private final Map<String, DestinationInfo> destinations = new HashMap<>();
    private final Object jmxResetTimeKey = new Object();
    private final List<String> destinationIds = new ArrayList<>();
    private final ConfigurationService configurationService;
    private final ContainerConfigurationListener containerConfigurationListener;
    private final SystemModelListener systemModelListener;

    private int jmxResetTime = DEFAULT_JMX_RESET_TIME_SECONDS;
    private ReposeInfo reposeInfo;
    private Date lastReset;
    private Timer timer;
    private ReportingTimerTask reportingTimerTask;

    @Inject
    public ReportingServiceImpl(ConfigurationService configurationService) {
        this.configurationService = configurationService;
        this.containerConfigurationListener = new ContainerConfigurationListener();
        this.systemModelListener = new SystemModelListener();

        timer = new Timer(TIMER_THREAD_NAME);
        reportingTimerTask = new ReportingTimerTask();
    }

    @PostConstruct
    public void init() {
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
        return (Date) lastReset.clone();
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
        long delayInMilliseconds = TimeUnit.SECONDS.toMillis(seconds);
        lastReset = new Date(System.currentTimeMillis());
        timer.scheduleAtFixedRate(reportingTimerTask, delayInMilliseconds, delayInMilliseconds);
    }

    @Override
    public synchronized DestinationInfo getDestinationInfo(String destinationId) {
        return destinations.get(destinationId).copy();
    }

    @Override
    public synchronized List<DestinationInfo> getDestinations() {
        final List<DestinationInfo> newDestinations = new ArrayList<>();

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
        // TODO: Do we need to deep copy the info for every read?
        // From what I can tell, the concern is with the HashMap in ReposeInfoStore which can be replaced by
        // a ConcurrentHashMap to ensure thread safety. Also, our deep copy is not a full copy -- most fields
        // will not retain their value. Judging from where this method is called, it seems like we just want to
        // read the current status code count(s).
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

    @SuppressWarnings("squid:S3398")
    private synchronized void reset() {
        // this method is only called by an inner-class, but it's synchronized so I'm leaving it here

        final Map<String, DestinationInfo> newDestinations = new HashMap<>();

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
            final List<String> endpointIds = new ArrayList<>();

            for (ReposeCluster reposeCluster : systemModel.getReposeCluster()) {
                final DestinationList destinationList = reposeCluster.getDestinations();

                for (DestinationEndpoint endpoint : destinationList.getEndpoint()) {
                    endpointIds.add(endpoint.getId());
                }

                for (DestinationCluster destinationCluster : destinationList.getTarget()) {
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

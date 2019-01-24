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
package org.openrepose.core.services.jmx;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.systemmodel.config.Filter;
import org.openrepose.core.systemmodel.config.SystemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Named("reposeConfigurationInformation")
@ManagedResource(description = "Repose configuration information MBean.")
public class ConfigurationInformation implements ConfigurationInformationMBean {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationInformation.class);

    private final ConfigurationService configurationService;
    private final CopyOnWriteArrayList<FilterInformation> filterInformation = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, Boolean> nodeStatus = new ConcurrentHashMap<>();

    private SystemModelListener systemModelListener;

    @Inject
    public ConfigurationInformation(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    @ManagedOperation(description = "Gets all available filter information")
    public List<CompositeData> getFilterInformation() throws OpenDataException {
        List<CompositeData> dataList = new ArrayList<>();
        for (FilterInformation filterInfo : filterInformation) {
            dataList.add(new ConfigurationInformationCompositeDataBuilder(filterInfo).toCompositeData());
        }

        return dataList;
    }

    @Override
    @ManagedAttribute(description = "tells you if this node is ready")
    public boolean isNodeReady(String nodeId) {
        Boolean result = nodeStatus.get(nodeId);
        if (result == null) {
            return false;
        } else {
            return result;
        }
    }

    /**
     * Used by things to indicate if a node is ready to work or not...
     *
     * @param nodeId    the node ID we care about
     * @param ready     Is this node ready?
     */
    public void updateNodeStatus(String nodeId, boolean ready) {
        nodeStatus.put(nodeId, ready);
    }

    @PostConstruct
    public void init() {
        LOG.info("Created ConfigurationInformation MBean");

        systemModelListener = new SystemModelListener();
        configurationService.subscribeTo("system-model.cfg.xml", systemModelListener, SystemModel.class);
    }

    @PreDestroy
    public void destroy() {
        configurationService.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
    }

    private class SystemModelListener implements UpdateListener<SystemModel> {

        private boolean initialized = false;

        @Override
        public void configurationUpdated(SystemModel systemModel) {
            initialized = false;

            filterInformation.clear();
            if (systemModel.getFilters() != null) {
                List<FilterInformation> filterList = new ArrayList<>();
                for (Filter filter : systemModel.getFilters().getFilter()) {
                    FilterInformation info = new FilterInformation(filter.getId(),
                        filter.getName(),
                        filter.getUriRegex(),
                        filter.getConfiguration(),
                        false);
                    filterList.add(info);
                }
                filterInformation.addAll(filterList);
            }

            initialized = true;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }
}

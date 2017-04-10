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
package org.openrepose.nodeservice.request;

import org.apache.commons.lang3.StringUtils;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.core.filter.SystemModelInterrogator;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.healthcheck.HealthCheckService;
import org.openrepose.core.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.core.services.healthcheck.Severity;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;
import java.util.Optional;

@Named
public class RequestHeaderServiceImpl implements RequestHeaderService {

    public static final String SYSTEM_MODEL_CONFIG_HEALTH_REPORT = "SystemModelConfigError";
    private static final Logger LOG = LoggerFactory.getLogger(RequestHeaderServiceImpl.class);
    private final ContainerConfigurationService containerConfigurationService;
    private final SystemModelListener systemModelListener;
    private final ConfigurationService configurationService;
    private final String clusterId;
    private final String nodeId;
    private final String reposeVersion;

    private String hostname;
    private HealthCheckServiceProxy healthCheckServiceProxy;

    @Inject
    public RequestHeaderServiceImpl(ConfigurationService configurationService,
                                    ContainerConfigurationService containerConfigurationService,
                                    HealthCheckService healthCheckService,
                                    @Value(ReposeSpringProperties.NODE.CLUSTER_ID) String clusterId,
                                    @Value(ReposeSpringProperties.NODE.NODE_ID) String nodeId,
                                    @Value(ReposeSpringProperties.CORE.REPOSE_VERSION) String reposeVersion) {
        this.configurationService = configurationService;
        this.containerConfigurationService = containerConfigurationService;
        this.clusterId = clusterId;
        this.nodeId = nodeId;
        this.reposeVersion = reposeVersion;

        this.systemModelListener = new SystemModelListener();
        healthCheckServiceProxy = healthCheckService.register(); //Sometimes we might deregister before we get to init
    }

    @PostConstruct
    public void init() {
        URL systemModelXsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        configurationService.subscribeTo("system-model.cfg.xml", systemModelXsdURL, systemModelListener, SystemModel.class);
    }

    @PreDestroy
    public void destroy() {
        healthCheckServiceProxy.deregister();
        configurationService.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
    }

    @Override
    public void setXForwardedFor(HttpServletRequestWrapper request) {
        request.addHeader(CommonHttpHeader.X_FORWARDED_FOR, request.getRemoteAddr());
    }

    @Override
    public void setVia(HttpServletRequestWrapper request) {
        final StringBuilder builder = new StringBuilder();
        this.appendProtocolVersion(builder, request);
        final Optional<String> requestVia = containerConfigurationService.getRequestVia();
        if (requestVia.isPresent() && StringUtils.isNotBlank(requestVia.get())) {
            builder.append(requestVia.get());
        } else {
            builder.append(hostname == null ? "Repose" : hostname)
                    .append(":").append(request.getLocalPort());
        }
        builder.append(" (Repose/").append(reposeVersion).append(")");
        request.addHeader(CommonHttpHeader.VIA, builder.toString());
    }

    /**
     * Listens for updates to the system-model.cfg.xml file which holds the
     * hostname.
     */
    private class SystemModelListener implements UpdateListener<SystemModel> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(SystemModel systemModel) {
            final SystemModelInterrogator interrogator = new SystemModelInterrogator(clusterId, nodeId);
            Optional<Node> ln = interrogator.getLocalNode(systemModel);

            if (ln.isPresent()) {
                hostname = ln.get().getHostname();
                isInitialized = true;
                healthCheckServiceProxy.resolveIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT);
            } else {
                LOG.error("Unable to identify the local host in the system model - please check your system-model.cfg.xml");
                healthCheckServiceProxy.reportIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT, "Unable to identify the " +
                        "local host in the system model - please check your system-model.cfg.xml", Severity.BROKEN);
            }
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}

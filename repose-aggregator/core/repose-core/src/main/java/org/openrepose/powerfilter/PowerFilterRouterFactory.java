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
package org.openrepose.powerfilter;

import org.openrepose.core.filter.routing.DestinationLocationBuilder;
import org.openrepose.core.services.reporting.ReportingService;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.services.routing.RoutingService;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.core.systemmodel.config.Destination;
import org.openrepose.core.systemmodel.config.DestinationEndpoint;
import org.openrepose.core.systemmodel.config.Node;
import org.openrepose.core.systemmodel.config.ReposeCluster;
import org.openrepose.nodeservice.request.RequestHeaderService;
import org.openrepose.nodeservice.response.ResponseHeaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import java.util.*;

// @TODO: This class is OBE'd with REP-7231
@Deprecated
@Named
public class PowerFilterRouterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PowerFilterRouterFactory.class);
    private final Optional<MetricsService> metricsService;
    private final ReportingService reportingService;
    private final RequestHeaderService requestHeaderService;
    private final ResponseHeaderService responseHeaderService;
    private final RoutingService routingService;
    private final String nodeId;
    private final String clusterId;

    @Inject
    public PowerFilterRouterFactory(
        @Value(ReposeSpringProperties.NODE.NODE_ID) String nodeId,
        @Value(ReposeSpringProperties.NODE.CLUSTER_ID) String clusterId,
        ReportingService reportingService,
        RequestHeaderService requestHeaderService,
        ResponseHeaderService responseHeaderService,
        RoutingService routingService,
        Optional<MetricsService> metricsService
    ) {
        LOG.info("Creating Repose Router Factory!");
        this.metricsService = metricsService;
        this.reportingService = reportingService;
        this.requestHeaderService = requestHeaderService;
        this.responseHeaderService = responseHeaderService;
        this.routingService = routingService;
        this.nodeId = nodeId;
        this.clusterId = clusterId;
    }

    public PowerFilterRouter getPowerFilterRouter(ReposeCluster domain,
                                                  Node localhost,
                                                  ServletContext servletContext,
                                                  String defaultDestination) throws PowerFilterChainException {
        LOG.info("{}:{} -- Reticulating Splines - Building Power Filter Router", clusterId, nodeId);
        if (LOG.isDebugEnabled()) {
            String cluster = domain.getId();
            //Build a list of the nodes in this cluster, just so we know what we're doing
            List<String> clusterNodes = new LinkedList<>();
            for (Node n : domain.getNodes().getNode()) {
                clusterNodes.add(n.getId() + "-" + n.getHostname());
            }
            LOG.debug("{}:{} - Cluster nodes from cluster {} for this router: {}", clusterId, nodeId, cluster, clusterNodes);
            List<String> destinations = new LinkedList<>();
            for (DestinationEndpoint endpoint : domain.getDestinations().getEndpoint()) {
                destinations.add(endpoint.getId() + "-" + endpoint.getHostname() + ":" + endpoint.getPort());
            }
            LOG.debug("{}:{} - Cluster destinations from cluster {} for this router: {}", clusterId, nodeId, cluster, destinations);
        }
        if (localhost == null || domain == null) {
            //TODO: THIS IS STOOPID
            throw new PowerFilterChainException("Domain and localhost cannot be null");
        }

        DestinationLocationBuilder locationBuilder = new DestinationLocationBuilder(routingService, localhost);
        Map<String, Destination> destinations = new HashMap<>();
        //Set up location builder
        if (domain.getDestinations() != null) {
            addDestinations(domain.getDestinations().getEndpoint(), destinations);
            addDestinations(domain.getDestinations().getTarget(), destinations);
        }

        return new PowerFilterRouterImpl(locationBuilder,
            destinations,
            domain,
            defaultDestination,
            servletContext,
            requestHeaderService,
            responseHeaderService,
            reportingService,
            metricsService);
    }

    private void addDestinations(List<? extends Destination> destList, Map<String, Destination> targetList) {
        for (Destination dest : destList) {
            targetList.put(dest.getId(), dest);
        }
    }
}

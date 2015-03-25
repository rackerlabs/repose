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
package org.openrepose.core.filter.routing;

import org.openrepose.core.domain.Port;
import org.openrepose.core.services.routing.RoutingService;
import org.openrepose.core.systemmodel.Destination;
import org.openrepose.core.systemmodel.DestinationCluster;
import org.openrepose.core.systemmodel.DestinationEndpoint;
import org.openrepose.core.systemmodel.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class DestinationLocationBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(DestinationLocationBuilder.class);
    private static final String HTTPS_PROTOCOL = "https";
    private final RoutingService routingService;
    private final Node localhost;

    public DestinationLocationBuilder(RoutingService routingService, Node localhost) {
        this.routingService = routingService;
        this.localhost = localhost;
    }

    private DestinationLocation buildDomainLocation(Destination destination, String uri, HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        if (!(destination instanceof DestinationCluster)) {
            throw new IllegalArgumentException("Destination must be of type DestinationCluster");
        }
        DestinationCluster domain = (DestinationCluster) destination;
        Node node = routingService.getRoutableNode(domain.getCluster().getId());
        if (node == null) {
            LOG.warn("No routable node for domain: " + domain.getId());
            return null;
        }
        int port = HTTPS_PROTOCOL.equalsIgnoreCase(domain.getProtocol()) ? node.getHttpsPort() : node.getHttpPort();
        return new DestinationLocation(
                new URL(domain.getProtocol(), node.getHostname(), port, domain.getRootPath() + uri),
                new URI(domain.getProtocol(), null, node.getHostname(), port, domain.getRootPath() + uri, request.getQueryString(), null));
    }

    private List<Port> localPortList() {
        LinkedList<Port> list = new LinkedList<>();
        if (localhost.getHttpPort() > 0) {
            list.add(new Port("http", localhost.getHttpPort()));
        }

        if (localhost.getHttpsPort() > 0) {
            list.add(new Port("https", localhost.getHttpsPort()));
        }
        return list;
    }

    private DestinationLocation buildEndpointLocation(Destination destination, String uri, HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        List<Port> localPorts = localPortList();

        return new DestinationLocation(
                new EndpointUrlBuilder(localhost, localPorts, destination, uri, request).build(),
                new EndpointUriBuilder(localPorts, destination, uri, request).build());
    }

    public DestinationLocation build(Destination destination, String uri, HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        if (destination == null) {
            throw new IllegalArgumentException("destination cannot be null");
        }
        if (destination instanceof DestinationEndpoint) {
            return buildEndpointLocation(destination, uri, request);
        } else if (destination instanceof DestinationCluster) {
            return buildDomainLocation(destination, uri, request);
        } else {
            throw new IllegalArgumentException("Unknown destination type: " + destination.getClass().getName());
        }
    }
}

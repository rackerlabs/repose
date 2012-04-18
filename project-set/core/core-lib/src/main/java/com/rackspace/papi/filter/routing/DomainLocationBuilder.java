package com.rackspace.papi.filter.routing;

import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DestinationDomain;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.service.routing.RoutingService;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class DomainLocationBuilder implements LocationBuilder {
    private static final String HTTP_PROTOCOL = "http";
    private final DestinationDomain domain;
    private final RoutingService routingService;
    private final String uri;

    public DomainLocationBuilder(RoutingService routingService, Destination destination, String uri) {
        this.routingService = routingService;
        this.uri = uri;
        this.domain = (DestinationDomain) destination;
    }

    @Override
    public DestinationLocation build() throws MalformedURLException, URISyntaxException {
        DomainNode node = routingService.getRoutableNode(domain.getId());
        int port = HTTP_PROTOCOL.equalsIgnoreCase(domain.getProtocol()) ? node.getHttpPort() : node.getHttpsPort();
        return new DestinationLocation(
                new URL(domain.getProtocol(), node.getHostname(), port, domain.getRootPath() + uri),
                new URI(domain.getProtocol(), null, node.getHostname(), port, domain.getRootPath() + uri, null, null));
    }
}

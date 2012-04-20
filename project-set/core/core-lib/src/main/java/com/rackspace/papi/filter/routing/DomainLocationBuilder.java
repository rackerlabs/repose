package com.rackspace.papi.filter.routing;

import com.rackspace.papi.filter.PowerFilterChain;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DestinationDomain;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.service.routing.RoutingService;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainLocationBuilder implements LocationBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(DomainLocationBuilder.class);
    private static final String HTTPS_PROTOCOL = "https";
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
        if (node == null) {
           LOG.warn("No routable node for domain: " + domain.getId());
           return null;
        }
        int port = HTTPS_PROTOCOL.equalsIgnoreCase(domain.getProtocol()) ? node.getHttpsPort() : node.getHttpPort();
        return new DestinationLocation(
                new URL(domain.getProtocol(), node.getHostname(), port, domain.getRootPath() + uri),
                new URI(domain.getProtocol(), null, node.getHostname(), port, domain.getRootPath() + uri, null, null));
    }
}

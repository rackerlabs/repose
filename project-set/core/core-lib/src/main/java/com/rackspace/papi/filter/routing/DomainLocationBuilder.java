package com.rackspace.papi.filter.routing;

import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DestinationCluster;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.service.routing.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Given a destination object which defines an endpoint from the system-model.cfg.xml, creates a
 * {@link com.rackspace.papi.filter.routing.DestinationLocation} which contains the full URI to the endpoint with all
 * necessary query parameters from the request.
 */
@Component("domainLocationBuilder")
public class DomainLocationBuilder implements LocationBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(DomainLocationBuilder.class);
    private static final String HTTPS_PROTOCOL = "https";
    private final RoutingService routingService;

    @Autowired
    public DomainLocationBuilder(@Qualifier("routingService") RoutingService routingService) {
        this.routingService = routingService;
    }

    @Override
    public DestinationLocation build(Destination destination, String uri, HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        if (!(destination instanceof DestinationCluster)) {
            throw new IllegalArgumentException("Destination must be of type DestinationCluster");
        }
        DestinationCluster domain = (DestinationCluster) destination;
        Node node = routingService.getRoutableNode( domain.getCluster().getId() );
        if (node == null) {
           LOG.warn("No routable node for domain: " + domain.getId());
           return null;
        }
        int port = HTTPS_PROTOCOL.equalsIgnoreCase(domain.getProtocol()) ? node.getHttpsPort() : node.getHttpPort();
        return new DestinationLocation(
                new URL(domain.getProtocol(), node.getHostname(), port, domain.getRootPath() + uri),
                new URI(domain.getProtocol(), null, node.getHostname(), port, domain.getRootPath() + uri, request.getQueryString(), null));
    }
}

package com.rackspace.papi.filter.routing;

import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DestinationCluster;
import com.rackspace.papi.model.DestinationEndpoint;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.service.routing.RoutingService;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class DestinationLocationBuilder {
    private final LocationBuilder builder;

    public DestinationLocationBuilder(RoutingService routingService, Node localhost, Destination destination, String uri, HttpServletRequest request) {
        if (localhost == null) {
            throw new IllegalArgumentException("localhost cannot be null");
        }
        if (destination == null) {
            throw new IllegalArgumentException("destination cannot be null");
        }
        if (routingService == null) {
            throw new IllegalArgumentException("routingService cannot be null");
        }

        if (destination instanceof DestinationEndpoint) {
            builder = new EndpointLocationBuilder(localhost, destination, uri, request);
        } else if (destination instanceof DestinationCluster) {
            builder = new DomainLocationBuilder(routingService, destination, uri, request);
        } else {
            throw new IllegalArgumentException("Unknown destination type: " + destination.getClass().getName());
        }
    }
    
    public LocationBuilder getBuilder() {
       return builder;
    }

    public DestinationLocation build() throws MalformedURLException, URISyntaxException {
        return builder.build();
    }
}

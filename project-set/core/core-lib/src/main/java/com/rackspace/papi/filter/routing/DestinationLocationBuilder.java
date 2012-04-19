package com.rackspace.papi.filter.routing;

import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DestinationDomain;
import com.rackspace.papi.model.DestinationEndpoint;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.service.routing.RoutingService;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;

public class DestinationLocationBuilder {
    private final LocationBuilder builder;

    public DestinationLocationBuilder(RoutingService routingService, DomainNode localhost, Destination destination, String uri, HttpServletRequest request) {
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
        } else if (destination instanceof DestinationDomain) {
            builder = new DomainLocationBuilder(routingService, destination, uri);
        } else {
            throw new IllegalArgumentException("Unknown destination type: " + destination.getClass().getName());
        }
    }

    public DestinationLocation build() throws MalformedURLException, URISyntaxException {
        return builder.build();
    }
}

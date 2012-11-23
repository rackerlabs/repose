package com.rackspace.papi.filter.routing;

import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DestinationCluster;
import com.rackspace.papi.model.DestinationEndpoint;
import com.rackspace.papi.model.Node;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("destinationLocationBuilder")
@Scope("prototype")
public class DestinationLocationBuilderImpl implements DestinationLocationBuilder {
    private final EndpointLocationBuilder endpointLocationBuilder;
    private final LocationBuilder domainLocationBuilder;

    @Autowired
    public DestinationLocationBuilderImpl(
            @Qualifier("domainLocationBuilder") DomainLocationBuilder domainLocationBuilder,
            @Qualifier("endpointLocationBuilder") EndpointLocationBuilder endpointLocationBuilder
            ) {
        this.domainLocationBuilder = domainLocationBuilder;
        this.endpointLocationBuilder = endpointLocationBuilder;
    }
    
    @Override
    public void init(Node localhost) {
        endpointLocationBuilder.init(localhost);
    }
    
    // For testing
    public LocationBuilder getBuilder(Destination destination) {
        final LocationBuilder builder;
        
        if (destination instanceof DestinationEndpoint) {
            builder = endpointLocationBuilder; 
        } else if (destination instanceof DestinationCluster) {
            builder = domainLocationBuilder; 
        } else {
            throw new IllegalArgumentException("Unknown destination type: " + destination.getClass().getName());
        }

        return builder;
    }

    @Override
    public DestinationLocation build(Destination destination, String uri, HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        if (destination == null) {
            throw new IllegalArgumentException("destination cannot be null");
        }
        
        return getBuilder(destination).build(destination, uri, request);
    }
}

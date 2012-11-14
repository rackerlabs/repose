package com.rackspace.papi.filter.routing;

import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.Node;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("endpointLocationBuilder")
@Scope("prototype")
public class EndpointLocationBuilder implements LocationBuilder {

    private final List<Port> localPorts = new ArrayList<Port>();
    private Node localhost;

    // Don't forget to add the autowire annotation if we ever require 
    // Spring beans to be injected.
    public EndpointLocationBuilder() {
    }

    public EndpointLocationBuilder init(Node localhost) {
        this.localhost = localhost;
        determineLocalPortsList();
        return this;
    }

    @Override
    public DestinationLocation build(Destination destination, String uri, HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        if (localhost == null) {
            throw new IllegalArgumentException("localhost hasn't been defined");
        }
        
        return new DestinationLocation(
                new EndpointUrlBuilder(localhost, localPorts, destination, uri, request).build(),
                new EndpointUriBuilder(localPorts, destination, uri, request).build());
    }

    private void determineLocalPortsList() {
        localPorts.clear();

        if (localhost.getHttpPort() > 0) {
            localPorts.add(new Port("http", localhost.getHttpPort()));
        }

        if (localhost.getHttpsPort() > 0) {
            localPorts.add(new Port("https", localhost.getHttpsPort()));
        }

    }
}

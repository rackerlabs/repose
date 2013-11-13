package com.rackspace.papi.filter.routing;

import com.rackspace.papi.commons.util.StringUriUtilities;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DestinationEndpoint;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class EndpointUriBuilder {

    private final DestinationEndpoint endpoint;
    private final String uri;
    private final List<Port> localPorts;
    private final HttpServletRequest request;

    EndpointUriBuilder(List<Port> localPorts, Destination destination, String uri, HttpServletRequest request) {
        this.uri = uri;
        this.localPorts = localPorts;
        this.request = request;
        endpoint = (DestinationEndpoint) destination;

    }

    private String determineScheme() {
        String scheme = endpoint.getProtocol();
        if (StringUtilities.isBlank(scheme) || endpoint.getPort() <= 0) {
            // no scheme or port specified means this is an internal dispatch
            return null;
        }
        return scheme;
    }

    private String determineHostname(String scheme) {
        if (StringUtilities.isBlank(scheme)) {
            return null;
        }

        Port port = new Port(scheme, endpoint.getPort());

        if (localPorts.contains(port) && (endpoint.getHostname() == null || "localhost".equalsIgnoreCase(endpoint.getHostname()))) {
            // internal dispatch
            return null;
        }

        return endpoint.getHostname();
    }

    public URI build() throws URISyntaxException {
        String scheme = determineScheme();
        String hostname = determineHostname(scheme);
        String rootPath = endpoint.getRootPath();

        String path = StringUriUtilities.concatUris(rootPath, uri);
        int port = scheme == null || hostname == null ? -1 : endpoint.getPort();

        return new URI(hostname != null ? scheme : null, null, hostname, port, path, request.getQueryString(), null);
    }
}

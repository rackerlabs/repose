package com.rackspace.papi.filter.routing;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DestinationEndpoint;
import com.rackspace.papi.model.DomainNode;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class EndpointLocationBuilder implements LocationBuilder {

    private final Destination destination;
    private final List<Port> localPorts = new ArrayList<Port>();
    private final DomainNode localhost;
    private final String uri;
    private final HttpServletRequest request;

    public EndpointLocationBuilder(DomainNode localhost, Destination destination, String uri, HttpServletRequest request) {
        this.destination = destination;
        this.localhost = localhost;
        this.uri = uri;
        this.request = request;
        determineLocalPortsList();
    }

    @Override
    public DestinationLocation build() throws MalformedURLException, URISyntaxException {
        return new DestinationLocation(
                new EndpointUrlBuilder().build(),
                new EndpointUriBuilder().build());
    }

    private class EndpointUriBuilder {

        private final DestinationEndpoint endpoint;

        private String determineScheme() {
            String scheme = destination.getProtocol();
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

            if (endpoint.getHostname() == null || "localhost".equalsIgnoreCase(endpoint.getHostname())) {
                if (localPorts.contains(port)) {
                    // internal dispatch
                    return null;
                }

                // dispatching to this host, but not our port
                return localhost.getHostname();
            }

            return endpoint.getHostname();
        }

        EndpointUriBuilder() {
            endpoint = (DestinationEndpoint) destination;

        }

        public URI build() throws URISyntaxException {
            String scheme = determineScheme();
            String hostname = determineHostname(scheme);
            String rootPath = endpoint.getRootPath();
            StringBuilder path = new StringBuilder(rootPath);

            if (!rootPath.isEmpty() && !uri.isEmpty()) {
                if (!rootPath.endsWith("/") && !uri.startsWith("/")) {
                    path.append("/");
                    path.append(uri);
                } else if (rootPath.endsWith("/") && uri.startsWith("/")) {
                    path.append(uri.substring(1));
                } else {
                    path.append(uri);
                }
            } else if (!uri.isEmpty()) {
                if (!uri.startsWith("/")) {
                    path.append("/");
                }
                path.append(uri);
            }
            int port = scheme == null || hostname == null ? -1 : endpoint.getPort();

            return new URI(hostname != null ? scheme : null, null, hostname, port, path.toString(), null, null);
        }
    }

    private class EndpointUrlBuilder {

        private final DestinationEndpoint endpoint;

        private Port determineUrlPort() throws MalformedURLException {
            if (!StringUtilities.isBlank(endpoint.getProtocol())) {
                return new Port(endpoint.getProtocol(), endpoint.getPort());
            }

            Port port = new Port(request.getScheme(), request.getLocalPort());
            if (localPorts.contains(port)) {
                return port;
            }

            throw new MalformedURLException("Cannot determine destination port.");
        }

        private String determineHostname() {
            String hostname = endpoint.getHostname();

            if (StringUtilities.isBlank(hostname)) {
                // endpoint is local
                hostname = localhost.getHostname();
            }

            return hostname;
        }

        EndpointUrlBuilder() {
            endpoint = (DestinationEndpoint) destination;

        }

        public URL build() throws MalformedURLException {
            Port port = determineUrlPort();
            String hostname = determineHostname();
            String rootPath = endpoint.getRootPath();
            StringBuilder path = new StringBuilder(rootPath);

            if (!rootPath.isEmpty() && !uri.isEmpty()) {
                if (!rootPath.endsWith("/") && !uri.startsWith("/")) {
                    path.append("/");
                    path.append(uri);
                } else if (rootPath.endsWith("/") && uri.startsWith("/")) {
                    path.append(uri.substring(1));
                } else {
                    path.append(uri);
                }
            } else if (!uri.isEmpty()) {
                if (!uri.startsWith("/")) {
                    path.append("/");
                }
                path.append(uri);
            }

            return new URL(port.getProtocol(), hostname, port.getPort(), path.toString());
        }
    }

    private void determineLocalPortsList() {

        if (localhost.getHttpPort() > 0) {
            localPorts.add(new Port("http", localhost.getHttpPort()));
        }

        if (localhost.getHttpsPort() > 0) {
            localPorts.add(new Port("https", localhost.getHttpsPort()));
        }

    }
}

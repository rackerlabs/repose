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

import org.openrepose.commons.utils.StringUriUtilities;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.core.domain.Port;
import org.openrepose.core.systemmodel.config.Destination;
import org.openrepose.core.systemmodel.config.DestinationEndpoint;
import org.openrepose.core.systemmodel.config.Node;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class EndpointUrlBuilder {

    private final DestinationEndpoint endpoint;
    private final Node localhost;
    private final String uri;
    private final List<Port> localPorts;
    private final HttpServletRequest request;

    EndpointUrlBuilder(Node localhost, List<Port> localPorts, Destination destination, String uri, HttpServletRequest request) {
        this.localhost = localhost;
        this.uri = uri;
        this.localPorts = localPorts;
        this.request = request;
        endpoint = (DestinationEndpoint) destination;

    }

    private int localPortForProtocol(String protocol) {
        for (Port port : localPorts) {
            if (port.getProtocol().equalsIgnoreCase(protocol)) {
                return port.getNumber();
            }
        }

        return 0;
    }

    private Port determineUrlPort() throws MalformedURLException {
        if (!StringUtilities.isBlank(endpoint.getProtocol())) {
            int port = endpoint.getPort() <= 0 ? localPortForProtocol(endpoint.getProtocol()) : endpoint.getPort();
            return new Port(endpoint.getProtocol(), port);
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

    public URL build() throws MalformedURLException {
        Port port = determineUrlPort();
        String hostname = determineHostname();
        String rootPath = endpoint.getRootPath();
        String path = StringUriUtilities.concatUris(rootPath, uri);

        return new URL(port.getProtocol(), hostname, port.getNumber(), path);
    }
}

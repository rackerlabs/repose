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
import org.apache.commons.lang3.StringUtils;
import org.openrepose.core.domain.Port;
import org.openrepose.core.systemmodel.config.Destination;
import org.openrepose.core.systemmodel.config.DestinationEndpoint;

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
        if (StringUtils.isBlank(scheme) || endpoint.getPort() <= 0) {
            // no scheme or port specified means this is an internal dispatch
            return null;
        }
        return scheme;
    }

    private String determineHostname(String scheme) {
        if (StringUtils.isBlank(scheme)) {
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

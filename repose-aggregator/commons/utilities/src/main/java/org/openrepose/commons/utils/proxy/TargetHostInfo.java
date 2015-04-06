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
package org.openrepose.commons.utils.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class TargetHostInfo {

    private static final Logger LOG = LoggerFactory.getLogger(TargetHostInfo.class);
    private final URI proxiedHostUri;
    private final URL proxiedHostUrl;

    public TargetHostInfo(String targetHost) {
        URI targetUri = null;

        try {
            targetUri = new URI(targetHost);
        } catch (URISyntaxException e) {
            LOG.error("Invalid target host url: " + targetHost, e);
        }

        proxiedHostUri = targetUri;
        proxiedHostUrl = asUri(proxiedHostUri);

    }

    private URL asUri(URI host) {
        if (host == null || host.getScheme() == null || host.getHost() == null) {
            return null;
        }
        try {
            return new URL(host.getScheme(), host.getHost(), host.getPort(), "");
        } catch (MalformedURLException ex) {
            LOG.error("Invalid host url: " + host, ex);
        }
        return null;
    }

    public URI getProxiedHostUri() {
        return proxiedHostUri;
    }

    public URL getProxiedHostUrl() {
        return proxiedHostUrl;
    }
}

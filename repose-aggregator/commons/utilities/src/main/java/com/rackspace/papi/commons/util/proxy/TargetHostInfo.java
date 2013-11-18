package com.rackspace.papi.commons.util.proxy;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

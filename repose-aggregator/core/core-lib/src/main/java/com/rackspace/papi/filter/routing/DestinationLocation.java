package com.rackspace.papi.filter.routing;

import java.net.URI;
import java.net.URL;

public class DestinationLocation {

    private final URL url;
    private final URI uri;

    public DestinationLocation(URL url, URI uri) {
        this.url = url;
        this.uri = uri;
    }

    public URL getUrl() {
        return url;
    }

    public URI getUri() {
        return uri;
    }
}

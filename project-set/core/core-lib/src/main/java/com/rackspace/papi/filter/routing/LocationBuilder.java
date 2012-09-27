package com.rackspace.papi.filter.routing;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

public interface LocationBuilder {

    DestinationLocation build() throws MalformedURLException, URISyntaxException;
}

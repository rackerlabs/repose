package com.rackspace.papi.filter.routing;

import com.rackspace.papi.filter.routing.DestinationLocation;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

public interface LocationBuilder {

    public DestinationLocation build() throws MalformedURLException, URISyntaxException;
}

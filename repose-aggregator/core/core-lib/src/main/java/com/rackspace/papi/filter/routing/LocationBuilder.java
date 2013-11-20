package com.rackspace.papi.filter.routing;

import com.rackspace.papi.model.Destination;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;

public interface LocationBuilder {

    DestinationLocation build(Destination destination, String uri, HttpServletRequest request) throws MalformedURLException, URISyntaxException;
}

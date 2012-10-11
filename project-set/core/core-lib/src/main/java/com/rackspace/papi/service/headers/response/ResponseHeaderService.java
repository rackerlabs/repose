package com.rackspace.papi.service.headers.response;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.servlet.http.RouteDestination;
import com.rackspace.papi.service.headers.common.ViaHeaderBuilder;
import javax.servlet.http.HttpServletRequest;

public interface ResponseHeaderService {
    void updateConfig(ViaHeaderBuilder viaHeaderBuilder, LocationHeaderBuilder locationHeaderBuilder);

    void setVia(MutableHttpServletRequest request, MutableHttpServletResponse response);
    void fixLocationHeader(HttpServletRequest originalRequest, MutableHttpServletResponse response, RouteDestination destination, String destinationLocationUri, String requestedContext);
}

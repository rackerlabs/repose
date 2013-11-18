package com.rackspace.papi.service.headers.request;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.service.headers.common.ViaHeaderBuilder;

public interface RequestHeaderService {

    void updateConfig(ViaHeaderBuilder viaHeaderBuilder);

    void setXForwardedFor(MutableHttpServletRequest request);
    void setVia(MutableHttpServletRequest request);
}

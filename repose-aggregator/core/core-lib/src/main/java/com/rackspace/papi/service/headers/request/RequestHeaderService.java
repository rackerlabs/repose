package com.rackspace.papi.service.headers.request;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.service.headers.common.ViaHeaderBuilder;

public interface RequestHeaderService {

    void updateConfig(ViaHeaderBuilder viaHeaderBuilder);

    void setXForwardedFor(MutableHttpServletRequest request);
    void setVia(MutableHttpServletRequest request);
}

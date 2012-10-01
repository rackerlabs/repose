package com.rackspace.papi.service.headers.response;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.service.headers.common.ViaHeaderBuilder;

public interface ResponseHeaderService {
    void updateConfig(ViaHeaderBuilder viaHeaderBuilder, LocationHeaderBuilder locationHeaderBuilder);

    void setVia(MutableHttpServletRequest request, MutableHttpServletResponse response);
    void fixLocationHeader(MutableHttpServletResponse response, String locationUri, String requestHostPath, String rootPath);
}

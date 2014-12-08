package org.openrepose.nodeservice.request;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;

public interface RequestHeaderService {

    void setXForwardedFor(MutableHttpServletRequest request);

    void setVia(MutableHttpServletRequest request);
}

package org.openrepose.core.services.headers.request;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.core.services.headers.common.ViaHeaderBuilder;

public interface RequestHeaderService {

    void setXForwardedFor(MutableHttpServletRequest request);

    void setVia(MutableHttpServletRequest request);
}

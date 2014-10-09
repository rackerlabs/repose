package org.openrepose.core.service.headers.request;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.core.service.headers.common.ViaHeaderBuilder;

public interface RequestHeaderService {

    void updateConfig(ViaHeaderBuilder viaHeaderBuilder);

    void setXForwardedFor(MutableHttpServletRequest request);
    void setVia(MutableHttpServletRequest request);
}

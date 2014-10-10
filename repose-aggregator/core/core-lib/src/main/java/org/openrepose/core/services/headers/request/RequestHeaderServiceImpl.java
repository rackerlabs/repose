package org.openrepose.core.services.headers.request;

import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.core.services.headers.common.ViaHeaderBuilder;
import org.springframework.stereotype.Component;

@Component("requestHeaderService")
public class RequestHeaderServiceImpl implements RequestHeaderService {

    private ViaHeaderBuilder viaHeaderBuilder;

    @Override
    public synchronized void updateConfig(ViaHeaderBuilder viaHeaderBuilder) {
        this.viaHeaderBuilder = viaHeaderBuilder;
    }

    @Override
    public void setXForwardedFor(MutableHttpServletRequest request) {
        request.addHeader(CommonHttpHeader.X_FORWARDED_FOR.toString(), request.getRemoteAddr());
    }

    @Override
    public void setVia(MutableHttpServletRequest request) {
        final String via = viaHeaderBuilder.buildVia(request);
        request.addHeader(CommonHttpHeader.VIA.toString(), via);
    }
}

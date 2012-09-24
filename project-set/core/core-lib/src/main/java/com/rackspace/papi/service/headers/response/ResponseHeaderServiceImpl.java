package com.rackspace.papi.service.headers.response;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.service.headers.common.ViaHeaderBuilder;
import org.springframework.stereotype.Component;

@Component("responseHeaderService")
public class ResponseHeaderServiceImpl implements ResponseHeaderService {

    private ViaHeaderBuilder viaHeaderBuilder;

    @Override
    public synchronized void updateConfig(ViaHeaderBuilder viaHeaderBuilder) {
        this.viaHeaderBuilder = viaHeaderBuilder;
    }

    @Override
    public void setVia(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        final String existingVia = response.getHeader(CommonHttpHeader.VIA.toString());
        final String myVia = viaHeaderBuilder.buildVia(request);
        final String via = StringUtilities.isBlank(existingVia) ? myVia : existingVia + ", " + myVia ;

        response.setHeader(CommonHttpHeader.VIA.toString(), via);
    }
}

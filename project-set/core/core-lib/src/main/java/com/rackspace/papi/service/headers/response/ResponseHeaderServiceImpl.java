package com.rackspace.papi.service.headers.response;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.servlet.http.RouteDestination;
import com.rackspace.papi.service.headers.common.ViaHeaderBuilder;
import java.net.MalformedURLException;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("responseHeaderService")
public class ResponseHeaderServiceImpl implements ResponseHeaderService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ResponseHeaderServiceImpl.class);
    private ViaHeaderBuilder viaHeaderBuilder;
    private LocationHeaderBuilder locationHeaderBuilder;

    @Override
    public synchronized void updateConfig(ViaHeaderBuilder viaHeaderBuilder, LocationHeaderBuilder locationHeaderBuilder) {
        this.viaHeaderBuilder = viaHeaderBuilder;
        this.locationHeaderBuilder = locationHeaderBuilder;
    }

    @Override
    public void setVia(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        final String existingVia = response.getHeader(CommonHttpHeader.VIA.toString());
        final String myVia = viaHeaderBuilder.buildVia(request);
        final String via = StringUtilities.isBlank(existingVia) ? myVia : existingVia + ", " + myVia ;

        response.setHeader(CommonHttpHeader.VIA.name(), via);
    }

    @Override
    public void fixLocationHeader(HttpServletRequest originalRequest, MutableHttpServletResponse response, RouteDestination destination, String destinationLocationUri, String proxiedRootContext) {
        String destinationUri = cleanPath(destinationLocationUri);
        if (!destinationUri.matches("^https?://.*")) {
            // local dispatch
            destinationUri = proxiedRootContext;
        }
        try {
            locationHeaderBuilder.setLocationHeader(originalRequest, response, destinationUri, destination.getContextRemoved(), proxiedRootContext);
        } catch (MalformedURLException ex) {
            LOG.warn("Invalid URL in location header processing", ex);
        }
    }

    private String cleanPath(String uri) {
        return uri == null ? "" : uri.split("\\?")[0];
    }
}

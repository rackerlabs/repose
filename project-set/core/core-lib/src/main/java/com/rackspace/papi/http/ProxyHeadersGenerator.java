package com.rackspace.papi.http;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.net.NetUtilities;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyHeadersGenerator {

    private String viaValue;
    private static final Logger LOG = LoggerFactory.getLogger(ProxyHeadersGenerator.class);
    private final String reposeVersion;

    public ProxyHeadersGenerator(String viaValue, String reposeVersion) {
        this.viaValue = viaValue;
        this.reposeVersion = reposeVersion;
    }

    private String buildVia(MutableHttpServletRequest request) {

        StringBuilder builder = new StringBuilder();

        String requestProtocol = request.getProtocol();
        LOG.info("Request Protocol Received: " + requestProtocol);

        if (!StringUtilities.isBlank(requestProtocol)) {
            builder.append(getProtocolVersion(requestProtocol)).append(getViaValue());
        }

        return builder.toString();
    }

    private String getProtocolVersion(String protocol) {
        final String version;

        if (protocol.contains("1.0")) {
            version = "1.0";
        } else {
            version = "1.1";
        }
        return version;
    }

    private String getViaValue() {

        StringBuilder builder = new StringBuilder(" ");

        if (StringUtilities.isBlank(viaValue)) {

            builder.append("Repose (").append(reposeVersion).append(")");
        } else {
            builder.append(viaValue);
        }

        return builder.toString();
    }

    private void setXForwardedFor(MutableHttpServletRequest request) {

        if (request.getHeader(CommonHttpHeader.X_FORWARDED_FOR.toString()) == null) {
            request.addHeader(CommonHttpHeader.X_FORWARDED_FOR.toString(), request.getRemoteAddr());
        }

        request.addHeader(CommonHttpHeader.X_FORWARDED_FOR.toString(), NetUtilities.getLocalAddress());
    }

    public void setRequestProxyHeaders(MutableHttpServletRequest request) {

        final String via = buildVia(request);
        request.addHeader(CommonHttpHeader.VIA.toString(), via);
        setXForwardedFor(request);
    }

    public void setResponseProxyHeaders(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        final String via = buildVia(request);
        response.addHeader(CommonHttpHeader.VIA.toString(), via);
    }
}
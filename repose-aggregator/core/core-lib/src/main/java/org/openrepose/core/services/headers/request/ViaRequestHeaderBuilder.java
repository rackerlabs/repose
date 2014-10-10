package org.openrepose.core.services.headers.request;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.core.services.headers.common.ViaHeaderBuilder;


public class ViaRequestHeaderBuilder extends ViaHeaderBuilder {

    private final String reposeVersion;
    private final String configuredViaReceivedBy;
    private final String hostname;

    public ViaRequestHeaderBuilder(String reposeVersion, String configuredViaReceivedBy, String hostname) {
        this.reposeVersion = reposeVersion;
        this.configuredViaReceivedBy = configuredViaReceivedBy;
        this.hostname = hostname;
    }

    @Override
    protected String getViaValue(MutableHttpServletRequest request) {
        final StringBuilder builder = new StringBuilder(" ");
        final String viaReceivedBy = StringUtilities.isBlank(configuredViaReceivedBy) ? getHostnamePort(request.getLocalPort()) : configuredViaReceivedBy;

        return builder.append(viaReceivedBy).append(" (Repose/").append(reposeVersion).append(")").toString();
    }

    private String getHostnamePort(int port) {
        return new StringBuilder(hostname).append(":").append(port).toString();
    }
}

package com.rackspace.papi.service.headers.response;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.service.proxy.TargetHostInfo;

import java.util.Collection;
import java.util.Iterator;

import static com.rackspace.papi.commons.util.http.CommonHttpHeader.LOCATION;

public class LocationHeaderBuilder {

    public void setLocationHeader(MutableHttpServletResponse servletResponse, String uri, String requestHostPath, String rootPath) {
        final String proxiedHostUrlRoot = createProxiedHostUrlRoot(uri, rootPath);
        final String translatedLocationUrl = translateLocationUrl(getLocationHeader(servletResponse), proxiedHostUrlRoot, requestHostPath);

        if (translatedLocationUrl != null) {
            servletResponse.setHeader(CommonHttpHeader.LOCATION.name(), translatedLocationUrl);
        }
    }

    private String createProxiedHostUrlRoot(String uri, String rootPath) {
        StringBuilder builder = new StringBuilder(new TargetHostInfo(uri).getProxiedHostUrl().toExternalForm());

        if (StringUtilities.isNotBlank(rootPath) && !"/".equalsIgnoreCase(rootPath)) {
            builder.append(rootPath);
        }

        return builder.toString();
    }

    private String translateLocationUrl(String proxiedRedirectUrl, String proxiedHostUrlRoot, String requestHostPath) {
        if (proxiedRedirectUrl == null) {
            return null;
        }

        if (StringUtilities.isEmpty(proxiedRedirectUrl)) {
            return requestHostPath;
        }

        return proxiedRedirectUrl.replace(proxiedHostUrlRoot, requestHostPath);
    }

    private String getLocationHeader(MutableHttpServletResponse servletResponse) {
        String location = null;

        Collection<String> locations = servletResponse.getHeaders(LOCATION.name());

        if (locations != null) {
            for (Iterator<String> iterator = locations.iterator(); iterator.hasNext();) {
                location = iterator.next();
            }
        }

        return location;
    }
}

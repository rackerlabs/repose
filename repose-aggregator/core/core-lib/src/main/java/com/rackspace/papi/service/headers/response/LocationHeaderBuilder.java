package com.rackspace.papi.service.headers.response;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.proxy.TargetHostInfo;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;

public class LocationHeaderBuilder {

    private static final String HTTPS = "https";
    private static final String HTTP = "http";
    private static final Integer DEFAULT_HTTP_PORT = 80;
    private static final Integer DEFAULT_HTTPS_PORT = 443;

    public void setLocationHeader(HttpServletRequest originalRequest, MutableHttpServletResponse servletResponse, String destinationUri, String requestedContext, String rootPath) throws MalformedURLException {
        final URL locationUrl = getLocationUrl(servletResponse);
        
        if (locationUrl == null) {
            return;
        }
        
        final URL requestedHostUrl = extractHostPath(originalRequest);
        final URL proxiedHostUrl = new TargetHostInfo(destinationUri).getProxiedHostUrl();

        final String translatedLocationUrl = translateLocationUrl(locationUrl, proxiedHostUrl, requestedHostUrl, requestedContext, rootPath);

        if (translatedLocationUrl != null) {
            servletResponse.setHeader(CommonHttpHeader.LOCATION.name(), translatedLocationUrl);
        }
    }

    private int getPort(URL url) {
        if (url.getPort() == -1) {
            return getDefaultPort(url.getProtocol());
        }

        return url.getPort();
    }

    private int getDefaultPort(String scheme) {
        if (HTTPS.equalsIgnoreCase(scheme)) {
            return DEFAULT_HTTPS_PORT;
        }
        if (HTTP.equalsIgnoreCase(scheme)) {
            return DEFAULT_HTTP_PORT;
        }

        return -1;
    }

    private URL extractHostPath(HttpServletRequest request) throws MalformedURLException {
        final StringBuilder myHostName = new StringBuilder(request.getScheme()).append("://").append(request.getServerName());
        myHostName.append(":").append(request.getServerPort());
        myHostName.append(request.getContextPath());
        return new URL(myHostName.toString());
    }

    private URL getLocationUrl(MutableHttpServletResponse servletResponse) throws MalformedURLException {
        String locationHeader = servletResponse.getHeader(CommonHttpHeader.LOCATION.name());
        if (StringUtilities.isNotBlank(locationHeader)) {
            return new URL(locationHeader);
        }

        return null;
    }

    private String getAbsolutePath(String inPath) {
        if (StringUtilities.isBlank(inPath)) {
            return "";
        }
        return !inPath.startsWith("/") ? "/" + inPath : inPath;

    }

    private String fixPathPrefix(String locationPath, String requestedPrefix, String addedPrefix) {
        String prefixToRemove = getAbsolutePath(addedPrefix);
        String prefixToAdd = getAbsolutePath(requestedPrefix);
        String result = locationPath;

        if (locationPath.startsWith(prefixToRemove)) {
            result = prefixToAdd + getAbsolutePath(locationPath.substring(prefixToRemove.length()));
        }

        return result;
    }

    private boolean shouldRewriteLocation(URL locationUrl, URL proxiedHostUrl, URL requestedHost) {
        if (proxiedHostUrl == null || locationUrl.getHost().equals(proxiedHostUrl.getHost()) && getPort(locationUrl) == getPort(proxiedHostUrl)) {
            return true;
        }

        if (locationUrl.getHost().equals(requestedHost.getHost()) && getPort(locationUrl) == getPort(requestedHost)) {
            return true;
        }

        return false;
    }

    private String translateLocationUrl(URL locationUrl, URL proxiedHostUrl, URL requestedHost, String requestedContext, String proxiedRootPath) {
        StringBuilder buffer = new StringBuilder();

        if (locationUrl == null) {
            return null;
        }

        if (StringUtilities.isEmpty(locationUrl.getHost())) {
            return requestedContext;
        }

        if (shouldRewriteLocation(locationUrl, proxiedHostUrl, requestedHost)) {
            // location header contains our host info
            buffer.append(requestedHost.getProtocol()).append("://").append(requestedHost.getHost());
            if (requestedHost.getPort() != DEFAULT_HTTP_PORT) {
                buffer.append(":").append(requestedHost.getPort());
            }
            buffer.append(fixPathPrefix(locationUrl.getPath(), requestedContext, proxiedRootPath));
        }

        return buffer.length() == 0 ? locationUrl.toExternalForm() : buffer.toString();
    }
}

package com.rackspace.papi.service.headers.response;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.service.proxy.TargetHostInfo;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;

public class LocationHeaderBuilder {

    private static final Integer DEFAULT_HTTP_PORT = 80;
    private static final Integer DEFAULT_HTTPS_PORT = 443;

    public void setLocationHeader(HttpServletRequest originalRequest, MutableHttpServletResponse servletResponse, String destinationUri, String requestedContext, String rootPath) throws MalformedURLException {
        final URL requestedHostUrl = extractHostPath(originalRequest);
        final URL proxiedHostUrl = new TargetHostInfo(destinationUri).getProxiedHostUrl();
        final URL locationUrl = getLocationUrl(servletResponse);

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
        if ("https".equalsIgnoreCase(scheme)) {
            return DEFAULT_HTTPS_PORT;
        }
        if ("http".equalsIgnoreCase(scheme)) {
            return DEFAULT_HTTP_PORT;
        }
        
        return -1;
    }

    private URL extractHostPath(HttpServletRequest request) throws MalformedURLException {
        final StringBuilder myHostName = new StringBuilder(request.getScheme()).append("://").append(request.getServerName());

        //if (request.getServerPort() != DEFAULT_HTTP_PORT) {
            myHostName.append(":").append(request.getServerPort());
        //}
            
        

        myHostName.append(request.getContextPath());
        return new URL(myHostName.toString());
    }

    private URL getLocationUrl(MutableHttpServletResponse servletResponse) throws MalformedURLException {
        String locationHeader = getLocationHeader(servletResponse);
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

        if (locationPath.startsWith(prefixToRemove)) {
            locationPath = prefixToAdd + locationPath.substring(prefixToRemove.length());
        }

        return locationPath;
    }

    private boolean shouldRewriteLocation(URL locationUrl, URL proxiedHostUrl, URL requestedHost) {
        if (locationUrl.getHost().equals(proxiedHostUrl.getHost()) && getPort(locationUrl) == getPort(proxiedHostUrl)) {
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

    private String getLocationHeader(MutableHttpServletResponse servletResponse) {
        String location = null;

        Collection<String> locations = servletResponse.getHeaders(CommonHttpHeader.LOCATION.name());

        if (locations != null) {
            for (Iterator<String> iterator = locations.iterator(); iterator.hasNext();) {
                location = iterator.next();
            }
        }

        return location;
    }

}

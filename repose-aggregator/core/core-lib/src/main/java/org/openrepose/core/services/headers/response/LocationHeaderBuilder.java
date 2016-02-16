/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.headers.response;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.proxy.TargetHostInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URL;

public class LocationHeaderBuilder {

    private static final String HTTPS = "https";
    private static final String HTTP = "http";
    private static final Integer DEFAULT_HTTP_PORT = 80;
    private static final Integer DEFAULT_HTTPS_PORT = 443;

    public void setLocationHeader(HttpServletRequest originalRequest, HttpServletResponse servletResponse, String destinationUri, String requestedContext, String rootPath) throws MalformedURLException {
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

    private URL getLocationUrl(HttpServletResponse servletResponse) throws MalformedURLException {
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
            buffer.append(fixPathPrefix(locationUrl.getFile(), requestedContext, proxiedRootPath));
        }

        return buffer.length() == 0 ? locationUrl.toExternalForm() : buffer.toString();
    }
}

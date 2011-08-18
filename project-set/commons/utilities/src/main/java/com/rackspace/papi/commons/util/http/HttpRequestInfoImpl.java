package com.rackspace.papi.commons.util.http;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 6/9/11
 * Time: 4:19 PM
 */
public class HttpRequestInfoImpl implements HttpRequestInfo {
    private final String uri;
    private final String url;
    private final String acceptHeader;

    public HttpRequestInfoImpl(HttpServletRequest request) {
        this.uri = request.getRequestURI();
        this.url = request.getRequestURL().toString();
        
        this.acceptHeader = request.getHeader(CommonHttpHeader.ACCEPT.headerKey());
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getAcceptHeader() {
        return acceptHeader;
    }
}

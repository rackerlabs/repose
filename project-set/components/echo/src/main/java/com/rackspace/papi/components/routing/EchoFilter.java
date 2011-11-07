package com.rackspace.papi.components.routing;

import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import javax.servlet.FilterConfig;
import org.slf4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EchoFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(EchoFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        final Enumeration<String> requestHeaderNames = httpRequest.getHeaderNames();

        while (requestHeaderNames.hasMoreElements()) {
            final String nextHeaderName = requestHeaderNames.nextElement();
            final Enumeration<String> headerValues = httpRequest.getHeaders(nextHeaderName);

            while (headerValues.hasMoreElements()) {
                httpResponse.addHeader(nextHeaderName, headerValues.nextElement());
            }
        }
        
        httpResponse.setStatus(200);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
}

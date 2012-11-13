package com.rackspace.papi.components.routing;

import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import org.slf4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import org.eclipse.jetty.http.HttpStatus;

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
        
        httpResponse.setStatus(HttpStatus.OK_200);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
}

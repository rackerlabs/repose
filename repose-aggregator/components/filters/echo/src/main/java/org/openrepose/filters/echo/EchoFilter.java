package org.openrepose.filters.echo;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

public class EchoFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
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
        
        httpResponse.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
}

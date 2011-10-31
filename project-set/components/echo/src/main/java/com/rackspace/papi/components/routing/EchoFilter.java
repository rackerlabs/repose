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

public class EchoFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(EchoFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

        // TODO
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
}

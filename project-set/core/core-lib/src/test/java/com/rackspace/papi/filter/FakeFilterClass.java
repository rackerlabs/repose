package com.rackspace.papi.filter;

import javax.servlet.*;
import java.io.IOException;

/**
 * @author fran
 */
public class FakeFilterClass implements Filter {
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    }

    @Override
    public void destroy() {
    }
}

package com.rackspace.lefty.tenant;

import javax.servlet.*;
import java.io.IOException;

/**
 * Created by adrian on 6/12/17.
 */
public class TenantCullingFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //do nothing
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        //do nothing
    }
}

package org.openrepose.filters.one.FirstFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by dimi5963 on 1/5/15.
 */
public class FirstFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //nada...
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        ServletRequest request = (ServletRequest)new ClassLoaderServletRequestWrapper((HttpServletRequest)servletRequest);
        filterChain.doFilter(request, servletResponse);

    }

    @Override
    public void destroy() {

    }
}

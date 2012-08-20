package org.openrepose.components.flush;

import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import java.io.IOException;
import javax.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlushOutputFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(FlushOutputFilter.class);
    private FlushOutputHandlerFactory handlerFactory;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {        
        handlerFactory = new FlushOutputHandlerFactory();
    }
}

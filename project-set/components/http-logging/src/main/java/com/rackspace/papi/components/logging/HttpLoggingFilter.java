package com.rackspace.papi.components.logging;

import com.rackspace.papi.components.logging.config.HttpLoggingConfig;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;

import javax.servlet.*;
import java.io.IOException;

/**
 *
 * @author jhopper
 */
public class HttpLoggingFilter implements Filter {

    private ConfigurationService manager;
    private HttpLoggingHandlerFactory handlerFactory;

    @Override
    public void destroy() {
        manager.unsubscribeFrom("http-logging.cfg.xml", handlerFactory);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        handlerFactory = new HttpLoggingHandlerFactory();
        manager = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).configurationService();

        manager.subscribeTo("http-logging.cfg.xml", handlerFactory, HttpLoggingConfig.class);
    }
}

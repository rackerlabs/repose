package org.openrepose.components.routing.servlet;

import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import org.slf4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import org.openrepose.components.routing.servlet.config.RootContextRouterConfiguration;
import org.slf4j.LoggerFactory;

public class RootContextRoutingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(RootContextRoutingFilter.class);
    private RoutingHandlerFactory handlerFactory;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        final ConfigurationService manager = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext()).configurationService();
        handlerFactory = new RoutingHandlerFactory();

        manager.subscribeTo("root-context-router.cfg.xml", handlerFactory, RootContextRouterConfiguration.class);
    }
}

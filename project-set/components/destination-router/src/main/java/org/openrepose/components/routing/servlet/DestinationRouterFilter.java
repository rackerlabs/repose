package org.openrepose.components.routing.servlet;

import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.model.PowerProxy;
import org.slf4j.Logger;
import com.rackspace.papi.domain.Port;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletContext;
import org.openrepose.components.routing.servlet.config.DestinationRouterConfiguration;
import org.slf4j.LoggerFactory;

public class DestinationRouterFilter implements Filter {

    //private static final Logger LOG = LoggerFactory.getLogger(DestinationRouterFilter.class);
    private DestinationRouterHandlerFactory handlerFactory;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        
        final ServletContext servletContext = filterConfig.getServletContext();
        final ConfigurationService manager = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext()).configurationService();
        final List<Port> ports = ServletContextHelper.getServerPorts(servletContext);
        
        handlerFactory = new DestinationRouterHandlerFactory(ports);
        
        manager.subscribeTo("power-proxy.cfg.xml", handlerFactory, PowerProxy.class);
        manager.subscribeTo("destination-router.cfg.xml", handlerFactory, DestinationRouterConfiguration.class);
    }
}

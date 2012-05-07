package org.openrepose.components.routing.servlet;

import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.model.SystemModel;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import javax.servlet.ServletContext;
import org.openrepose.components.routing.servlet.config.DestinationRouterConfiguration;

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
        final ConfigurationService manager = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).configurationService();
        
        handlerFactory = new DestinationRouterHandlerFactory();
        
        manager.subscribeTo("system-model.cfg.xml", handlerFactory, SystemModel.class);
        manager.subscribeTo("destination-router.cfg.xml", handlerFactory, DestinationRouterConfiguration.class);
    }
}

package org.openrepose.components.routing.servlet;

import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import org.openrepose.components.routing.servlet.config.DestinationRouterConfiguration;

import javax.servlet.*;
import java.io.IOException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DestinationRouterFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(DestinationRouterFilter.class);
    private static final String DEFAULT_CONFIG = "destination-router.cfg.xml";
    private String config;
    private DestinationRouterHandlerFactory handlerFactory;
    private ConfigurationService manager;
    private MetricsService metricsService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        manager.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {        
        manager = ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext().configurationService();
        metricsService = ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext()
                .metricsService();
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        handlerFactory = new DestinationRouterHandlerFactory(metricsService);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/destination-router-configuration.xsd");
        manager.subscribeTo(filterConfig.getFilterName(),"system-model.cfg.xml", handlerFactory, SystemModel.class);
        manager.subscribeTo(filterConfig.getFilterName(),config,xsdURL, handlerFactory, DestinationRouterConfiguration.class);
    }
}

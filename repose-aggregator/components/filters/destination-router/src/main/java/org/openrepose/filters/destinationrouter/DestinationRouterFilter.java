package org.openrepose.filters.destinationrouter;

import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.filters.routing.servlet.config.DestinationRouterConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class DestinationRouterFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(DestinationRouterFilter.class);
    private static final String DEFAULT_CONFIG = "destination-router.cfg.xml";
    private String config;
    private DestinationRouterHandlerFactory handlerFactory;
    private final ConfigurationService configurationService;
    private final MetricsService metricsService;

    @Inject
    public DestinationRouterFilter(ConfigurationService configurationService,
                                   MetricsService metricsService) {
        this.configurationService = configurationService;
        this.metricsService = metricsService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationService.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {        
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        handlerFactory = new DestinationRouterHandlerFactory(metricsService);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/destination-router-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(),"system-model.cfg.xml", handlerFactory, SystemModel.class);
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, DestinationRouterConfiguration.class);
    }
}

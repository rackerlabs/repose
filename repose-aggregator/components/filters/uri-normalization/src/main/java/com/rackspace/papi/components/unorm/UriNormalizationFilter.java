package com.rackspace.papi.components.unorm;

import com.rackspace.papi.components.uri.normalization.config.UriNormalizationConfig;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import java.io.IOException;
import java.net.URL;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;

import com.rackspace.papi.service.reporting.metrics.MetricsService;
import org.slf4j.Logger;

@Named
public class UriNormalizationFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(UriNormalizationFilter.class);
    private static final String DEFAULT_CONFIG = "uri-normalization.cfg.xml";
    private String config;
    private UriNormalizationHandlerFactory handlerFactory;
    private final ConfigurationService configurationService;
    private final MetricsService metricsService;

    @Inject
    public UriNormalizationFilter(
            ConfigurationService configurationService,
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
        handlerFactory = new UriNormalizationHandlerFactory(metricsService);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/uri-normalization-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, UriNormalizationConfig.class);
    }
}

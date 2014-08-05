package com.rackspace.papi.components.hnorm;

import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.service.config.ConfigurationService;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.rackspacecloud.api.docs.repose.header_normalization.v1.HeaderNormalizationConfig;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class HeaderNormalizationFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(HeaderNormalizationFilter.class);
    private static final String DEFAULT_CONFIG = "header-normalization.cfg.xml";
    private String config;
    private HeaderNormalizationHandlerFactory handlerFactory;
    private final ConfigurationService configurationService;
    private final MetricsService metricsService;

    @Inject
    public HeaderNormalizationFilter(
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
        handlerFactory = new HeaderNormalizationHandlerFactory(metricsService);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/header-normalization-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, HeaderNormalizationConfig.class);
    }
}

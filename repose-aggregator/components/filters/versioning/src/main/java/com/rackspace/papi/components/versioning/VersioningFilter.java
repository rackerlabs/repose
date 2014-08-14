package com.rackspace.papi.components.versioning;

import com.rackspace.papi.components.versioning.config.ServiceVersionMappingList;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.healthcheck.HealthCheckService;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import org.openrepose.core.service.config.ConfigurationService;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

@Named
public class VersioningFilter implements Filter {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(VersioningFilter.class);
    private static final String DEFAULT_CONFIG = "versioning.cfg.xml";
    private String config;
    private VersioningHandlerFactory handlerFactory;
    private final ConfigurationService configurationService;
    private final MetricsService metricsService;
    private final HealthCheckService healthCheckService;
    private final ReposeInstanceInfo reposeInstanceInfo;

    @Inject
    public VersioningFilter(ConfigurationService configurationService,
                            MetricsService metricsService,
                            HealthCheckService healthCheckService,
                            ReposeInstanceInfo reposeInstanceInfo) {
        this.configurationService = configurationService;
        this.metricsService = metricsService;
        this.healthCheckService = healthCheckService;
        this.reposeInstanceInfo = reposeInstanceInfo;
    }

    @Override
    public void destroy() {
        configurationService.unsubscribeFrom("system-model.cfg.xml", handlerFactory);
        configurationService.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        handlerFactory = new VersioningHandlerFactory(reposeInstanceInfo, metricsService, healthCheckService);
        configurationService.subscribeTo(filterConfig.getFilterName(), "system-model.cfg.xml", handlerFactory, SystemModel.class);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/versioning-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, ServiceVersionMappingList.class);
    }
}

package org.openrepose.filters.versioning;

import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.core.domain.ServicePorts;
import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.context.ContextAdapter;
import org.openrepose.core.services.context.ServletContextHelper;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.filters.versioning.config.ServiceVersionMappingList;
import org.openrepose.services.healthcheck.HealthCheckService;
import org.slf4j.Logger;

import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author jhopper
 */
public class VersioningFilter implements Filter {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(VersioningFilter.class);
    private static final String DEFAULT_CONFIG = "versioning.cfg.xml";
    private String config;
    private VersioningHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;
    private MetricsService metricsService;
    private HealthCheckService healthCheckService;

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom("system-model.cfg.xml", handlerFactory);
        configurationManager.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        final ServletContext servletContext = filterConfig.getServletContext();
        ServletContextHelper contextHelper = ServletContextHelper.getInstance(servletContext);
        final ServicePorts ports = contextHelper.getServerPorts();

        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);

        ContextAdapter contextAdapter = contextHelper.getPowerApiContext();
        configurationManager = contextAdapter.configurationService();
        metricsService = contextAdapter.metricsService();
        healthCheckService = contextAdapter.healthCheckService();

        handlerFactory = new VersioningHandlerFactory(ports, metricsService, healthCheckService);
        configurationManager.subscribeTo(filterConfig.getFilterName(),"system-model.cfg.xml", handlerFactory, SystemModel.class);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/versioning-configuration.xsd");
        configurationManager.subscribeTo(filterConfig.getFilterName(),config,xsdURL, handlerFactory, ServiceVersionMappingList.class);
    }
}

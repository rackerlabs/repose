package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.healthcheck.HealthCheckService;
import com.rackspace.papi.service.healthcheck.HealthCheckServiceProxy;
import com.rackspace.papi.service.healthcheck.Severity;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.rackspace.papi.service.reporting.metrics.config.GraphiteServer;
import com.rackspace.papi.service.reporting.metrics.config.MetricsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;
import java.io.IOException;
import java.net.URL;

/**
 * Manages the {@link com.rackspace.papi.service.reporting.metrics.MetricsService} instance and subscribes to the
 * metrics.cfg.xml configuration file.
 */
@Component("metricsServiceContext")
public class MetricsServiceContext implements ServiceContext<MetricsService> {
    public static final String SERVICE_NAME = "MetricsService";
    public static final String DEFAULT_CONFIG_NAME = "metrics.cfg.xml";

    private static final Logger LOG = LoggerFactory.getLogger(MetricsService.class);
    private static final String metricsServiceConfigReport = "MetricsServiceReport";

    private final MetricsService metricsService;
    private final ServiceRegistry registry;
    private final ConfigurationService configurationService;
    private final MetricsCfgListener metricsCfgListener;

    private HealthCheckServiceProxy healthCheckServiceProxy;

    public MetricsServiceContext(@Qualifier("serviceRegistry") ServiceRegistry registry,
                                 @Qualifier("configurationManager") ConfigurationService configurationService,
                                 @Qualifier("metricsService") MetricsService metricsService,
                                 @Qualifier("healthCheckService") HealthCheckService healthCheckService) {

        this.registry = registry;
        this.configurationService = configurationService;
        this.metricsService = metricsService;
        metricsCfgListener = new MetricsCfgListener();
        this.healthCheckServiceProxy = healthCheckService.register(MetricsServiceContext.class);
    }

    private void register() {
        if (registry != null) {
            registry.addService(this);
        }
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public MetricsService getService() {
        if (metricsService != null && metricsService.isEnabled()) {
            return metricsService;
        }
        return null;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        healthCheckServiceProxy.reportIssue(metricsServiceConfigReport, "Metrics Service Configuration Error", Severity.BROKEN);
        URL xsdURL = getClass().getResource("/META-INF/schema/metrics/metrics.xsd");
        configurationService.subscribeTo(DEFAULT_CONFIG_NAME, xsdURL, metricsCfgListener, MetricsConfiguration.class);

        // The Metrics config is optional so in the case where the configuration listener doesn't mark it iniitalized
        // and the file doesn't exist, this means that the Metrics service will load its own default configuration
        // and the initial health check error should be cleared.
        try {
            if (!metricsCfgListener.isInitialized() && !configurationService.getResourceResolver().resolve("metrics.cfg.xml").exists()) {
                healthCheckServiceProxy.resolveIssue(metricsServiceConfigReport);
            }
        } catch (IOException io) {
            LOG.error("Error attempting to search for " + DEFAULT_CONFIG_NAME);
        }
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

        metricsService.destroy();
        configurationService.unsubscribeFrom(DEFAULT_CONFIG_NAME, metricsCfgListener);
    }

    private class MetricsCfgListener implements UpdateListener<MetricsConfiguration> {

        private boolean initialized = false;

        @Override
        public void configurationUpdated(MetricsConfiguration metricsC) {

            // we are reinitializing the graphite servers
            metricsService.shutdownGraphite();

            if (metricsC.getGraphite() != null) {

                try {

                    for (GraphiteServer gs : metricsC.getGraphite().getServer()) {

                        metricsService.addGraphiteServer(gs.getHost(),
                                gs.getPort().intValue(),
                                gs.getPeriod(),
                                gs.getPrefix());
                    }
                } catch (IOException e) {

                    LOG.debug("Error with the MetricsService", e);
                }
            }

            healthCheckServiceProxy.resolveIssue(metricsServiceConfigReport);
            metricsService.setEnabled(metricsC.isEnabled());
            initialized = true;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }
}

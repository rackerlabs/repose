package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.healthcheck.*;
import com.rackspace.papi.service.httpclient.HttpClientService;
import com.rackspace.papi.service.httpclient.config.HttpConnectionPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContextEvent;
import java.io.IOException;
import java.net.URL;

/**
 * Manages the {@link com.rackspace.papi.service.httpclient.HttpClientService} instance and
 * subscribes to the http-connection-pool.cfg.xml configuration file.
 */

public class HttpConnectionPoolServiceContext implements ServiceContext<HttpClientService> {
    private static final Logger LOG = LoggerFactory.getLogger(HttpConnectionPoolServiceContext.class);

    public static final String SERVICE_NAME = "powerapi:/services/httpConnectionPool";
    public static final String DEFAULT_CONFIG_NAME = "http-connection-pool.cfg.xml";
    private static final String httpConnectionPoolServiceReport = "HttpConnectionPoolServiceReport";

    private final HttpClientService connectionPoolService;
    private final ServiceRegistry registry;
    private final ConfigurationService configurationService;
    private final ConfigurationListener configurationListener;
    private final HealthCheckService healthCheckService;
    private String healthCheckUID;

    @Autowired
    public HttpConnectionPoolServiceContext(ServiceRegistry registry,
                                            ConfigurationService configurationService,
                                            HttpClientService connectionPoolService,
                                            HealthCheckService healthCheckService) {

        this.registry = registry;
        this.configurationService = configurationService;
        this.connectionPoolService = connectionPoolService;
        this.healthCheckService = healthCheckService;
        configurationListener = new ConfigurationListener();
        healthCheckUID = healthCheckService.register(HttpConnectionPoolServiceContext.class);

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
    public HttpClientService getService() {
        return connectionPoolService;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.debug("Initializing context for HTTPConnectionPool");
        reportIssue();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/http-connection-pool.xsd");
        configurationService.subscribeTo(DEFAULT_CONFIG_NAME, xsdURL, configurationListener, HttpConnectionPoolConfig.class);

        // The Http Connection Pool config is optional so in the case where the configuration listener doesn't mark it iniitalized
        // and the file doesn't exist, this means that the Http Connection Pool service will load its own default configuration
        // and the initial health check error should be cleared.
        try {
            if (!configurationListener.isInitialized() && !configurationService.getResourceResolver().resolve(DEFAULT_CONFIG_NAME).exists()) {
                solveIssue();
            }
        } catch (IOException io) {
            LOG.error("Error attempting to search for {}", DEFAULT_CONFIG_NAME, io);
        }
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.debug("Destroying context for HTTPConnectionPool");
        connectionPoolService.shutdown();
        configurationService.unsubscribeFrom(DEFAULT_CONFIG_NAME, configurationListener);
    }

    private class ConfigurationListener implements UpdateListener<HttpConnectionPoolConfig> {

        private boolean initialized = false;

        @Override
        public void configurationUpdated(HttpConnectionPoolConfig poolConfig) {
            connectionPoolService.configure(poolConfig);
            initialized = true;
            solveIssue();
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }

    private void reportIssue() {

        LOG.debug("Reporting issue to Health Checker Service: " + httpConnectionPoolServiceReport);
        try {
            healthCheckService.reportIssue(healthCheckUID, httpConnectionPoolServiceReport,
                    new HealthCheckReport("Metrics Service Configuration Error", Severity.BROKEN));
        } catch (InputNullException e) {
            LOG.error("Unable to report Issues to Health Check Service", e);
        } catch (NotRegisteredException e) {
            LOG.error("Unable to report Issues to Health Check Service", e);
        }

    }

    private void solveIssue() {

        try {
            LOG.debug("Resolving issue: " + httpConnectionPoolServiceReport);
            healthCheckService.solveIssue(healthCheckUID, httpConnectionPoolServiceReport);
        } catch (InputNullException e) {
            LOG.error("Unable to solve issue {} from {}", httpConnectionPoolServiceReport, healthCheckUID, e);
        } catch (NotRegisteredException e) {
            LOG.error("Unable to solve issue {} from {}", httpConnectionPoolServiceReport, healthCheckUID, e);
        }

    }

}

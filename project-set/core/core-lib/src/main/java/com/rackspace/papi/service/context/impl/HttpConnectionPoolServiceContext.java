package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.httpclient.HttpClientService;
import com.rackspace.papi.service.httpclient.config.HttpConnectionPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContextEvent;
import java.net.URL;

/**
 * Manages the {@link com.rackspace.papi.service.httpclient.HttpClientService} instance and
 * subscribes to the http-connection-pool.cfg.xml configuration file.
 */

public class HttpConnectionPoolServiceContext implements ServiceContext<HttpClientService> {
    private static final Logger LOG = LoggerFactory.getLogger(HttpConnectionPoolServiceContext.class);

    public static final String SERVICE_NAME = "powerapi:/services/httpConnectionPool";
    public static final String DEFAULT_CONFIG_NAME = "http-connection-pool.cfg.xml";

    private final HttpClientService connectionPoolService;
    private final ServiceRegistry registry;
    private final ConfigurationService configurationService;
    private final ConfigurationListener configurationListener;

    @Autowired
    public HttpConnectionPoolServiceContext( ServiceRegistry registry,
                                 ConfigurationService configurationService,
                                 HttpClientService connectionPoolService) {

        this.registry = registry;
        this.configurationService = configurationService;
        this.connectionPoolService = connectionPoolService;
        configurationListener = new ConfigurationListener();
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
        URL xsdURL = getClass().getResource("/META-INF/schema/config/http-connection-pool.xsd");
        configurationService.subscribeTo(DEFAULT_CONFIG_NAME, xsdURL, configurationListener, HttpConnectionPoolConfig.class);

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
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }
}
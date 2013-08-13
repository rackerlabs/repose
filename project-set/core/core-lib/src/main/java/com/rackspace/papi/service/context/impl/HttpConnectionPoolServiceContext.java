package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;

import com.rackspace.papi.service.httpclient.HttpClientService;
import com.rackspace.papi.service.httpconnectionpool.config.HttpConnectionPoolConfig;
import com.rackspace.papi.service.httpconnectionpool.HttpConnectionPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.servlet.ServletContextEvent;
import java.net.URL;

/**
 * Manages the {@link httpconnectionpool.HttpClientService} instance and
 * subscribes to the http-connection-pool.cfg.xml configuration file.
 */
//@Component("httpConnectionPoolServiceContext")
public class HttpConnectionPoolServiceContext implements ServiceContext<HttpClientService> {

    public static final String SERVICE_NAME = "HttpConnectionPoolService";
    public static final String DEFAULT_CONFIG_NAME = "http-connection-pool.cfg.xml";

    private final HttpClientService connectionPoolService;
    private final ServiceRegistry registry;
    private final ConfigurationService configurationService;
    private final ConfigurationListener configurationListener;

    @Autowired
    public HttpConnectionPoolServiceContext(@Qualifier("serviceRegistry") ServiceRegistry registry,
                                 @Qualifier("configurationManager") ConfigurationService configurationService,
                                 @Qualifier("httpConnectionPoolService") HttpClientService connectionPoolService) {

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

        URL xsdURL = getClass().getResource("/META-INF/schema/config/http-connection-pool.xsd");
        configurationService.subscribeTo(DEFAULT_CONFIG_NAME, xsdURL, configurationListener, HttpConnectionPoolConfig.class);

        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
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
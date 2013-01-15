package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.proxy.RequestProxyService;
import javax.servlet.ServletContextEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("requestProxyServiceContext")
@Lazy(true)
public class RequestProxyServiceContext implements ServiceContext<RequestProxyService> {

    public static final String SERVICE_NAME = "powerapi:/services/proxy";
    private final ConfigurationService configurationManager;
    private final RequestProxyService proxyService;
    private final ServiceRegistry registry;
    private final ContainerConfigListener configListener;

    @Autowired
    public RequestProxyServiceContext(
            @Qualifier("requestProxyService") RequestProxyService proxyService,
            @Qualifier("serviceRegistry") ServiceRegistry registry,
            @Qualifier("configurationManager") ConfigurationService configurationManager) {
        this.proxyService = proxyService;
        this.configurationManager = configurationManager;
        this.registry = registry;
        this.configListener = new ContainerConfigListener();
    }

    public void register() {
        if (registry != null) {
            registry.addService(this);
        }
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public RequestProxyService getService() {
        return proxyService;
    }

    private class ContainerConfigListener implements UpdateListener<ContainerConfiguration> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContainerConfiguration config) {
            Integer connectionTimeout = config.getDeploymentConfig().getConnectionTimeout();
            Integer readTimeout = config.getDeploymentConfig().getReadTimeout();
            Integer proxyThreadPool = config.getDeploymentConfig().getProxyThreadPool();
            boolean requestLogging = config.getDeploymentConfig().isClientRequestLogging();
            proxyService.updateConfiguration(connectionTimeout, readTimeout, proxyThreadPool, requestLogging);
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        configurationManager.subscribeTo("container.cfg.xml", configListener, ContainerConfiguration.class);
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (configurationManager != null) {
            configurationManager.unsubscribeFrom("container.cfg.xml", configListener);
        }
    }
}

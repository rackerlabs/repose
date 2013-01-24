package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.container.config.DeploymentConfiguration;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.container.ContainerConfigurationService;
import java.net.URL;
import javax.servlet.ServletContextEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("containerServiceContext")
public class ContainerServiceContext implements ServiceContext<ContainerConfigurationService> {

    public static final String SERVICE_NAME = "powerapi:/services/container";
    private final ContainerConfigurationListener configurationListener;
    private ContainerConfigurationService containerConfigurationService;
    private ConfigurationService configurationManager;
    private final ServiceRegistry registry;

    @Autowired
    public ContainerServiceContext(
            @Qualifier("containerConfigurationService") ContainerConfigurationService containerConfigurationService,
            @Qualifier("serviceRegistry") ServiceRegistry registry,
            @Qualifier("configurationManager") ConfigurationService configurationManager) {
        this.containerConfigurationService = containerConfigurationService;
        this.configurationListener = new ContainerConfigurationListener();
        this.configurationManager = configurationManager;
        this.registry = registry;
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
    public ContainerConfigurationService getService() {
        return containerConfigurationService;
    }

    /**
     * Listens for updates to the container.cfg.xml file which holds the
     * location of the log properties file.
     */
    private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContainerConfiguration configurationObject) {
            DeploymentConfiguration deployConfig = configurationObject.getDeploymentConfig();
            String via = deployConfig.getVia();

            Long maxResponseContentSize = deployConfig.getContentBodyReadLimit();
            containerConfigurationService.setVia(via);
            containerConfigurationService.setContentBodyReadLimit(maxResponseContentSize);
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        URL xsdURL = getClass().getResource("/META-INF/schema/container/container-configuration.xsd");
        configurationManager.subscribeTo("container.cfg.xml", xsdURL, configurationListener, ContainerConfiguration.class);
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        if (configurationManager != null) {
            configurationManager.unsubscribeFrom("container.cfg.xml", configurationListener);
        }
    }
}

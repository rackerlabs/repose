package org.openrepose.core.services.context.impl;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.container.config.DeploymentConfiguration;
import org.openrepose.core.services.ServiceRegistry;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.context.ServiceContext;
import org.openrepose.core.services.context.container.ContainerConfigurationService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;
import java.net.URL;

@Component("containerServiceContext")
public class ContainerServiceContext implements ServiceContext<ContainerConfigurationService> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ContainerServiceContext.class);
    public static final String SERVICE_NAME = "powerapi:/services/container";
    private static final int THIRTY_SECONDS_MILLIS = 30000;
    private static final int THREAD_POOL_SIZE = 20;
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

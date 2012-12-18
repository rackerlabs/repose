package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.management.ManagementService;
import com.rackspace.papi.servlet.InitParameter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;
import javax.xml.transform.stream.StreamSource;

/**
 * Created by IntelliJ IDEA.
 * User: fran
 * Date: Oct 24, 2012
 * Time: 3:31:26 PM
 */
@Component("managementServiceContext")
public class ManagementServiceContext implements ServiceContext<ManagementService> {

    public static final String SERVICE_NAME = "powerapi:/services/management";
    private static final String DEFAULT_MANAGEMENT_CONTEXT = "/repose";

    private final ServiceRegistry registry;
    private final ConfigurationService configurationService;
    private final ManagementService managementService;
    private final ContainerConfigurationListener configurationListener;
    private int managementPort;
    private String managementContext;

    @Autowired    
    public ManagementServiceContext(@Qualifier("serviceRegistry") ServiceRegistry registry ,
           @Qualifier("configurationManager") ConfigurationService configurationService,
           @Qualifier("managementService") ManagementService managementService) {
        this.registry = registry;
        this.configurationService = configurationService;
        this.managementService = managementService;
        this.configurationListener = new ContainerConfigurationListener();
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
    public ManagementService getService() {
        return managementService;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        getManagementPort();
        getManagementContext();
        configurationService.subscribeTo("container.cfg.xml",configurationListener, ContainerConfiguration.class);
        register();
    }

    private void getManagementPort() {
        managementPort = Integer.valueOf(System.getProperty(InitParameter.MANAGEMENT_PORT.getParameterName()));
    }

    private void getManagementContext() {
        managementContext = System.getProperty(InitParameter.MANAGEMENT_CONTEXT.getParameterName(), DEFAULT_MANAGEMENT_CONTEXT);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        configurationService.unsubscribeFrom("container.cfg.xml", configurationListener);
        managementService.stop();
    }

    private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

        @Override
        public void configurationUpdated(ContainerConfiguration configurationObject) {

            if (configurationObject.getDeploymentConfig() != null) {

                final String artifactDirectory = configurationObject.getDeploymentConfig().getArtifactDirectory().getValue();

                managementService.start(managementPort, artifactDirectory, managementContext);
            }
        }
    }
}

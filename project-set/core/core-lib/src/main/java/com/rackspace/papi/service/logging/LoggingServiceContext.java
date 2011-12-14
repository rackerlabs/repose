package com.rackspace.papi.service.logging;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.container.config.ContainerConfiguration;

import com.rackspace.papi.container.config.LoggingConfiguration;
import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.io.InputStream;

/**
 * @author fran
 */
public class LoggingServiceContext implements ServiceContext<LoggingService> {
    public static final String SERVICE_NAME = "powerapi:/services/logging";
    private final LoggingService loggingService;
    private ConfigurationService configurationManager;
    private final ContainerConfigurationListener configurationListener;
    private final LoggingConfigurationListener loggingConfigurationListener;
    private String loggingConfigurationConfig = "";

    public LoggingServiceContext() {
        this.loggingService = new LoggingServiceImpl();
        this.configurationListener = new ContainerConfigurationListener();
        this.loggingConfigurationListener = new LoggingConfigurationListener();
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public LoggingService getService() {
        return loggingService;
    }

    private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

        @Override
        public void configurationUpdated(ContainerConfiguration configurationObject) {

            if (configurationObject.getDeploymentConfig() != null) {
                final LoggingConfiguration loggingConfig = configurationObject.getDeploymentConfig().getLoggingConfiguration();

                if (loggingConfig != null && !StringUtilities.isBlank(loggingConfig.getValue())) {
                    final String newLoggingConfig = loggingConfig.getValue();

                    if (!loggingConfigurationConfig.equalsIgnoreCase(newLoggingConfig)) {
                        updateLogConfigFileSubscription(loggingConfigurationConfig, newLoggingConfig);
                        loggingConfigurationConfig = newLoggingConfig;
                    }
                }
            }
        }
    }

    private void updateLogConfigFileSubscription(String currentLoggingConfig, String loggingConfig) {
        configurationManager.unsubscribeFrom(currentLoggingConfig, loggingConfigurationListener);
        configurationManager.subscribeTo(loggingConfig, loggingConfigurationListener, InputStream.class);
    }

    private class LoggingConfigurationListener implements UpdateListener<InputStream> {

        @Override
        public void configurationUpdated(InputStream configurationObject) {
            loggingService.updateLoggingConfiguration(configurationObject);
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        configurationManager = ServletContextHelper.getPowerApiContext(servletContext).configurationService();

        configurationManager.subscribeTo("container.cfg.xml", configurationListener, ContainerConfiguration.class);
        
        /* 
         * TODO: Re-implement when custom parser is created
         */
        //configurationManager.subscribeTo(loggingFileLocation, loggingConfigurationListener, InputStream.class); 
        configurationManager.subscribeTo(loggingConfigurationConfig, loggingConfigurationListener, InputStream.class);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        configurationManager.unsubscribeFrom("container.cfg.xml", configurationListener);
        configurationManager.unsubscribeFrom(loggingConfigurationConfig, loggingConfigurationListener);
    }
}

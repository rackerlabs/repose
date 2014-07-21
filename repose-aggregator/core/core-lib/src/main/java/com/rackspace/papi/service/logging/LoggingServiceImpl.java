package com.rackspace.papi.service.logging;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.properties.PropertiesFileConfigurationParser;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.container.config.LoggingConfiguration;
import com.rackspace.papi.service.config.ConfigurationService;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.inject.Named;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.URL;
import java.util.Properties;

@Named
public class LoggingServiceImpl implements LoggingService {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingServiceImpl.class);

    private final ContainerConfigurationListener configurationListener;
    private final LoggingConfigurationListener loggingConfigurationListener;

    private ConfigurationService configurationManager;
    private String loggingConfigurationConfig = "";

    @Inject
    public LoggingServiceImpl(ConfigurationService configurationManager) {
        this.configurationManager = configurationManager;

        this.configurationListener = new ContainerConfigurationListener();
        this.loggingConfigurationListener = new LoggingConfigurationListener();
    }

    @PostConstruct
    public void afterPropertiesSet() {
        URL containerXsdURL = getClass().getResource("/META-INF/schema/container/container-configuration.xsd");

        configurationManager.subscribeTo("container.cfg.xml", containerXsdURL, configurationListener, ContainerConfiguration.class);
    }

    @PreDestroy
    public void destroy() {
        configurationManager.unsubscribeFrom("container.cfg.xml", configurationListener);
        configurationManager.unsubscribeFrom(loggingConfigurationConfig, loggingConfigurationListener);
    }

    @Override
    public void updateLoggingConfiguration(Properties loggingConfigFile) {
        PropertyConfigurator.configure(loggingConfigFile);
    }

    private void updateLogConfigFileSubscription(String currentLoggingConfig, String loggingConfig) {
        configurationManager.unsubscribeFrom(currentLoggingConfig, loggingConfigurationListener);
        configurationManager.subscribeTo("", loggingConfig, loggingConfigurationListener, new PropertiesFileConfigurationParser());
    }

    /**
     * Listens for updates to the container.cfg.xml file which holds the location of the log properties file.
     */
    private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {
        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContainerConfiguration configurationObject) {
            if (configurationObject.getDeploymentConfig() != null) {
                final LoggingConfiguration loggingConfig = configurationObject.getDeploymentConfig().getLoggingConfiguration();

                if (loggingConfig != null && !StringUtilities.isBlank(loggingConfig.getHref())) {
                    final String newLoggingConfig = loggingConfig.getHref();
                    loggingConfigurationConfig = newLoggingConfig;
                    updateLogConfigFileSubscription(loggingConfigurationConfig, newLoggingConfig);
                }
            }

            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    /**
     * Listens for updates to the log properties file.
     */
    private class LoggingConfigurationListener implements UpdateListener<Properties> {
        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(Properties configurationObject) {
            updateLoggingConfiguration(configurationObject);

            LOG.error("ERROR LEVEL LOG STATEMENT");
            LOG.warn("WARN LEVEL LOG STATEMENT");
            LOG.info("INFO LEVEL LOG STATEMENT");
            LOG.debug("DEBUG LEVEL LOG STATEMENT");
            LOG.trace("TRACE LEVEL LOG STATEMENT");

            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}

package org.openrepose.core.services.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.services.config.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

@Named
public class LoggingServiceImpl implements LoggingService {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingServiceImpl.class);

    private final ConfigurationService configurationService;
    private final ContainerConfigurationListener configurationListener;

    private File currentConfigDir = new File(System.getProperty("user.dir"));
    private String currentConfigFileName = null;

    @Inject
    public LoggingServiceImpl(ConfigurationService configurationService) {
        this.configurationService = configurationService;

        this.configurationListener = new ContainerConfigurationListener();
    }

    @PostConstruct
    public void init() {
        URL containerXsdURL = getClass().getResource("/META-INF/schema/container/container-configuration.xsd");

        configurationService.subscribeTo("container.cfg.xml", containerXsdURL, configurationListener, ContainerConfiguration.class);
    }

    @PreDestroy
    public void destroy() {
        configurationService.unsubscribeFrom("container.cfg.xml", configurationListener);
    }

    @Override
    public void updateLoggingConfiguration(String configFileName) {
        if (configFileName == null) {
            LOG.debug("Requested to reload a NULL configuration.");
        } else if (configFileName.equals(currentConfigFileName)) {
            LOG.debug("Requested to reload the same configuration: {}", configFileName);
        } else {
            URL url = null;
            try {
                url = new URL(configFileName);
            } catch (MalformedURLException e) {
                LOG.trace("Failed to convert Logging Configuration File Name to URL.", e);
            }
            File file;
            if (url != null) {
                file = new File(url.getFile());
            } else {
                file = new File(configFileName);
            }
            if (!file.exists()) {
                file = new File(currentConfigDir, configFileName);
            }
            if (file.canRead()) {
                currentConfigFileName = configFileName;
                System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, file.getAbsolutePath());
                LoggerContext context = (LoggerContext) LogManager.getContext(false);
                context.reconfigure();
                LOG.error("ERROR LEVEL LOG STATEMENT");
                LOG.warn("WARN  LEVEL LOG STATEMENT");
                LOG.info("INFO  LEVEL LOG STATEMENT");
                LOG.debug("DEBUG LEVEL LOG STATEMENT");
                LOG.trace("TRACE LEVEL LOG STATEMENT");
            } else if (currentConfigFileName == null) {
                String fallBackResource = this.getClass().getResource("/org/openrepose/core/services/logging/log4j2-DEFAULT.xml").toString();
                System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, fallBackResource);
                LoggerContext context = (LoggerContext) LogManager.getContext(false);
                context.reconfigure();
                LOG.error("================================================================================");
                LOG.error("|| ERROR FALLING BACK TO THE DEFAULT LOGGING CONFIGURATION!!!                 ||");
                LOG.error("|| NO LOGS ARE BEING CREATED. ALL OUTPUT IS ONLY ON STANDARD OUT              ||");
                LOG.error("================================================================================");
            } else {
                LOG.warn("An attempt was made to switch to an invalid Logging Configuration file: {}", configFileName);
            }
        }
    }

    /**
     * Listens for updates to the container.cfg.xml file which holds the location of the log properties file.
     */
    private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContainerConfiguration configurationObject) {
            if (configurationObject.getDeploymentConfig() != null) {
                updateLoggingConfiguration(configurationObject.getDeploymentConfig().getLoggingConfiguration().getHref());
            }
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}

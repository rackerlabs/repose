package org.openrepose.core.services.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * @author fran
 */
public class LoggingServiceImpl implements LoggingService {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(LoggingServiceImpl.class);

    public LoggingServiceImpl() {
    }

    @Override
    public void updateLoggingConfiguration(Resource configLocation) {
        try {
            ConfigurationSource configurationSource = new ConfigurationSource(configLocation.getInputStream());
            XmlConfiguration xmlConfiguration = new XmlConfiguration(configurationSource);
            ((Logger) LogManager.getRootLogger()).getContext().start(xmlConfiguration);
        } catch (IOException e) {
            LOG.info("Failed to load the new Logging configuration from " + configLocation, e);
        }
    }
}

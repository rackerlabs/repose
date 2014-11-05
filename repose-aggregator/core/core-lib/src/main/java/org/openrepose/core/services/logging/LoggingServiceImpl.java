package org.openrepose.core.services.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

/**
 * @author fran
 */
public class LoggingServiceImpl implements LoggingService {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(LoggingServiceImpl.class);

    public LoggingServiceImpl() {
    }

    @Override
    public void updateLoggingConfiguration(URI configLocation) {
        try {
            URL url = configLocation.toURL();
            ConfigurationSource configurationSource = new ConfigurationSource(url.openStream(), url);
            XmlConfiguration xmlConfiguration = new XmlConfiguration(configurationSource);
            ((Logger) LogManager.getRootLogger()).getContext().start(xmlConfiguration);
        } catch (IOException e) {
            LOG.info("Failed to load the new Logging configuration from " + configLocation, e);
        }
    }
}

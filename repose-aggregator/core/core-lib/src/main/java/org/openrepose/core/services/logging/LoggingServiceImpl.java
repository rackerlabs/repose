package org.openrepose.core.services.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;

import java.io.File;

/**
 * @author fran
 */
public class LoggingServiceImpl implements LoggingService {
    public LoggingServiceImpl() {
    }

    @Override
    public void updateLoggingConfiguration(File configLocation) {
            System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, configLocation.getAbsolutePath());
            ((LoggerContext) LogManager.getContext(false)).reconfigure();
    }
}

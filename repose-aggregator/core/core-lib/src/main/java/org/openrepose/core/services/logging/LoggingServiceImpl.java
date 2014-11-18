package org.openrepose.core.services.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class LoggingServiceImpl implements LoggingService {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingServiceImpl.class.getName());
    private File currentConfigDir = new File(System.getProperty("user.dir"));
    private String currentConfigFileName = null;

    public LoggingServiceImpl() {
    }

    @Override
    public void updateLoggingConfiguration(String configFileName) {
        if (configFileName != null && !configFileName.equals(currentConfigFileName)) {
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
                System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, file.getAbsolutePath());
                LoggerContext context = (LoggerContext) LogManager.getContext(false);
                context.reconfigure();
                LOG.error("ERROR LEVEL LOG STATEMENT");
                LOG.warn("WARN  LEVEL LOG STATEMENT");
                LOG.info("INFO  LEVEL LOG STATEMENT");
                LOG.debug("DEBUG LEVEL LOG STATEMENT");
                LOG.trace("TRACE LEVEL LOG STATEMENT");
            } else if (currentConfigFileName == null) {
                // BE LOUD - Bad Location on Initial Load
            } else {
                // BE LOUD - Bad Location after running
            }
        } else {
            // Location was NULL or the same as the one already loaded.
        }
    }
}

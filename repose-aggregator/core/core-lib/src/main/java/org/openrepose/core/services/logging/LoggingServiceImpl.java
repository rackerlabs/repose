package org.openrepose.core.services.logging;

import java.util.Properties;

/**
 * @author fran
 */
public class LoggingServiceImpl implements LoggingService {
    public LoggingServiceImpl(){}

    @Override
    public void updateLoggingConfiguration(Properties loggingConfigFile) {
        org.apache.log4j.PropertyConfigurator.configure(loggingConfigFile);
    }
}

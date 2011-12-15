package com.rackspace.papi.service.logging;

import com.rackspace.papi.service.logging.facade.LoggingConfigurationFacade;
import com.rackspace.papi.service.logging.facade.LoggingConfigurationFacadeImpl;

import java.io.InputStream;

/**
 * @author fran
 */
public class LoggingServiceImpl implements LoggingService {
    private final LoggingConfigurationFacade loggingConfigurationFacade;

    public LoggingServiceImpl(LogFrameworks logFramework) {
        loggingConfigurationFacade = new LoggingConfigurationFacadeImpl(logFramework);
    }

    @Override
    public void updateLoggingConfiguration(InputStream loggingConfigFile) {
        loggingConfigurationFacade.configure(loggingConfigFile);
    }
}

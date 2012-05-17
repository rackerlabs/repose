package com.rackspace.papi.service.logging;

import com.rackspace.papi.service.logging.common.LogFrameworks;
import com.rackspace.papi.service.logging.facade.LoggingConfigurationFacade;
import com.rackspace.papi.service.logging.facade.LoggingConfigurationFacadeImpl;

import java.util.Properties;

/**
 * @author fran
 */
public class LoggingServiceImpl implements LoggingService {
    private final LoggingConfigurationFacade loggingConfigurationFacade;
    
    public LoggingServiceImpl(String framework) {
       this(LogFrameworks.valueOf(framework));
    }

    public LoggingServiceImpl(LogFrameworks logFramework) {
        loggingConfigurationFacade = new LoggingConfigurationFacadeImpl(logFramework);
    }

    @Override
    public void updateLoggingConfiguration(Properties loggingConfigFile) {
        loggingConfigurationFacade.configure(loggingConfigFile);
    }
}

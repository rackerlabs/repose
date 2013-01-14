package com.rackspace.papi.service.logging.facade;

import com.rackspace.papi.service.logging.common.LogFrameworks;
import org.apache.log4j.PropertyConfigurator;

import java.util.Properties;


/**
 * @author fran
 */
public class LoggingConfigurationFacadeImpl implements LoggingConfigurationFacade {

    private final LogFrameworks logFramework;

    public LoggingConfigurationFacadeImpl(LogFrameworks logFramework) {
        this.logFramework = logFramework;
    }

    @Override
    public void configure(Properties logProperties) {
        switch (logFramework) {
            case LOG4J :
                PropertyConfigurator.configure(logProperties);
                break;
                
            default:
                break;
        }
    }
}

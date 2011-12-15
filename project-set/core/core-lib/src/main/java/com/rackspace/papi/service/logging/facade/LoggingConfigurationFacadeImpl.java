package com.rackspace.papi.service.logging.facade;

import com.rackspace.papi.service.logging.LogFrameworks;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.apache.log4j.PropertyConfigurator;

/**
 * @author fran
 */
public class LoggingConfigurationFacadeImpl implements LoggingConfigurationFacade {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingConfigurationFacadeImpl.class);

    private final LogFrameworks logFramework;

    public LoggingConfigurationFacadeImpl(LogFrameworks logFramework) {
        this.logFramework = logFramework;
    }

    @Override
    public void configure(InputStream configurationProperties) {
        switch (logFramework) {
            case LOG4J :
                configureLog4JFramework(configurationProperties);
        }
    }

    private void configureLog4JFramework(InputStream configurationProperties) {
        Properties properties = new Properties();

        try {
            properties.load(configurationProperties);
        } catch (IOException e) {
            LOG.error("An IOException occurred when attempting to read the Repose system logging configuration file.");
        }

//        PropertyConfigurator.configure(properties);
    }
}

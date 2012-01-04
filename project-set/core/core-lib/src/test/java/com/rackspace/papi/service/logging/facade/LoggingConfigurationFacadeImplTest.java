package com.rackspace.papi.service.logging.facade;

import com.rackspace.papi.service.logging.common.LogFrameworks;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class LoggingConfigurationFacadeImplTest {

    public static class WhenConfiguringLog4J {
        private static final Logger LOG = LoggerFactory.getLogger(WhenConfiguringLog4J.class);

        @Test
        @Ignore
        public void shouldConfigureToLogLevelAll() throws IOException {

            LoggingConfigurationFacade loggingConfigurationFacade = new LoggingConfigurationFacadeImpl(LogFrameworks.LOG4J);

            System.out.println(new File(".").getAbsolutePath());

            loggingConfigurationFacade.configure(LoggingConfigurationFacadeImplTest.class.getResourceAsStream("log4j.properties"));

            LOG.error("error");
            LOG.warn("warn");
            LOG.info("info");
            LOG.debug("debug");
            LOG.trace("trace");

            // TODO: This task got moved in priority
        }
    }
}

package com.rackspace.papi.service.logging;

import java.io.InputStream;

/**
 * @author fran
 */
public interface LoggingService {
    public void updateLoggingConfiguration(InputStream loggingConfigFile);
}

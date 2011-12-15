package com.rackspace.papi.service.logging.facade;

import java.io.InputStream;

/**
 * @author fran
 */
public interface LoggingConfigurationFacade {
    public void configure(InputStream configurationProperties);
}

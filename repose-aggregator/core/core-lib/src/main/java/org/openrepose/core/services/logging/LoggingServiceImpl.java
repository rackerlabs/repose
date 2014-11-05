package org.openrepose.core.services.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

import java.net.URI;

/**
 * @author fran
 */
public class LoggingServiceImpl implements LoggingService {
    public LoggingServiceImpl(){}

    @Override
    public void updateLoggingConfiguration(URI configLocation) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configurator.initialize(ctx.getName(), null, configLocation);
    }
}

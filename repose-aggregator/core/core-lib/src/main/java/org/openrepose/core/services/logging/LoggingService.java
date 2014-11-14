package org.openrepose.core.services.logging;

import org.springframework.core.io.Resource;

/**
 * @author fran
 */
public interface LoggingService {
    void updateLoggingConfiguration(Resource configLocation);
}

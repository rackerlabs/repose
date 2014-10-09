package org.openrepose.services.healthcheck;

public interface HealthCheckService {
    /**
     * Register to Health Check Service. This will return a unique ID which the caller will associate with issues
     *
     * @return A {@link HealthCheckServiceProxy} with which to report issues
     */
    HealthCheckServiceProxy register();

    /**
     * Reports whether there is an issue with the current Repose Deployment
     *
     * @return boolean indicating whether or not there is an issue with the current Repose deployment
     */
    boolean isHealthy();
}

package com.rackspace.papi.service.healthcheck;

public interface HealthCheckService {
    /**
     * Register to Health Check Service. This will return a unique ID which the caller will associate with issues
     *
     * @return A {@link com.rackspace.papi.service.healthcheck.HealthCheckServiceProxy} with which to report issues
     */
    HealthCheckServiceProxy register();
}

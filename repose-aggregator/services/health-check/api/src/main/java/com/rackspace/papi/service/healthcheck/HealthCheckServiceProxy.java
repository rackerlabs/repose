package com.rackspace.papi.service.healthcheck;

import java.util.Map;
import java.util.Set;

/**
 * An API proxy returned when registering with the HealthCheckService.
 * <p/>
 * This proxy stores a UID to uniquely identify the caller of the register method and provides a simplified API
 * by eliminating the need to provide a UID on each request.
 * <p/>
 * See method documentation on the {@link HealthCheckService} interface.
 */
interface HealthCheckServiceProxy {
    boolean isHealthy();

    HealthCheckReport getDiagnosis(String id);

    void reportIssue(String rid, String message, Severity severity);

    Set<String> getReportIds();

    void resolveIssue(String id);

    Map<String, HealthCheckReport> getReports();
}

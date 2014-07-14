package com.rackspace.papi.service.healthcheck;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * An API proxy returned when registering with the HealthCheckService.
 * <p/>
 * This proxy stores a UID to uniquely identify the caller of the register method and provides a simplified API
 * by eliminating the need to provide a UID on each request.
 */
public interface HealthCheckServiceProxy {
    /**
     * @return The UUID identifying this proxy to the HealthCheckService
     */
    UUID getUid();

    /**
     * Reports whether there is an issue with the current Repose Deployment
     *
     * @return boolean indicating whether or not there is an issue with the current Repose deployment
     */
    boolean isHealthy();

    /**
     * Retrieves message associated with the latest issue
     *
     * @param id  The id of issue. If null, behavior undefined.
     * @return HealthCheckReport containing details of issue reported
     */
    HealthCheckReport getDiagnosis(String id);

    /**
     * Reports an issue to the health service
     *
     * @param rid    The id of issue. If null, behavior undefined.
     * @param message A detailed message to include in the HealthCheckReport
     * @param severity The severity of the issues. If broken, Repose will return response code 503 until the issue
     *                 is resolved.
     */
    void reportIssue(String rid, String message, Severity severity);

    /**
     * Retrieves the ids of all reported problems for a given UID
     *
     * @return IDs of all reports corresponding to the UID provided
     */
    Set<String> getReportIds();

    /**
     * Tells the Health Check Service that the issue has been resolved.
     *
     * @param id  The id of issue. If null, behavior undefined.
     */
    void resolveIssue(String id);

    /**
     * Retrieves map of health reports associated with the given UID
     *
     * @return Map of all reports corresponding to the UID provided
     */
    Map<String, HealthCheckReport> getReports();

    /**
     * Deregister with the Health Check Service.
     */
    void deregister();
}

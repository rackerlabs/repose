package com.rackspace.papi.service.healthcheck;

import java.util.Map;
import java.util.Set;

public interface HealthCheckService {

    /**
     * Reports whether there is an issue with the current Repose Deployment
     *
     * @return boolean indicating whether or not there is an issue with the current Repose deployment
     */
    boolean isHealthy();

    /**
     * Retrieves message associated with the latest issue
     *
     * @param uid Id of class/user which reported the issue. If null, behavior undefined.
     * @param id  The id of issue. If null, behavior undefined.
     * @return HealthCheckReport containing details of issue reported
     */
    HealthCheckReport getDiagnosis(String uid, String id);

    /**
     * Reports an issue to the health service
     *
     * @param uid    Id of class/user which reported the issue. If null, behavior undefined.
     * @param rid    The id of issue. If null, behavior undefined.
     * @param report The HealthCheckReport which contains the details of the report
     */
    void reportIssue(String uid, String rid, HealthCheckReport report);

    /**
     * Retrieves the ids of all reported problems for a given UID
     *
     * @param uid Id of class/user which reported the issue. If null, behavior undefined.
     * @return IDs of all reports corresponding to the UID provided
     */
    Set<String> getReportIds(String uid);

    /**
     * Tells the Health Check Service that the issue has been resolved.
     *
     * @param uid Id of class/user which reported the issue. If null, behavior undefined.
     * @param id  The id of issue. If null, behavior undefined.
     */
    void resolveIssue(String uid, String id);

    /**
     * Register to Health Check Service. This will return a unique ID which the caller will associate with issues
     *
     * @param T Class registering with the Health Check Service
     * @return UID with which to report issues
     */
    HealthCheckServiceProxy register(Class T);

    /**
     * Retrieves map of health reports associated with the given UID
     *
     * @param uid Id of class/user which reported the issue. If null, behavior undefined.
     * @return Map of all reports corresponding to the UID provided
     */
    Map<String, HealthCheckReport> getReports(String uid);
}

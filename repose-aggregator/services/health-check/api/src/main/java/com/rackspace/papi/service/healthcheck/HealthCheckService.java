package com.rackspace.papi.service.healthcheck;

import java.util.Map;
import java.util.Set;

public interface HealthCheckService {

    /**
     * Reports whether there is an issue with the current Repose Deployment
     * @return
     */
    boolean isHealthy();

    /**
     * Retrieves message associated with the latest issue.
     * @return
     */
    HealthCheckReport getDiagnosis(String UID, String id) throws NotRegisteredException;

    /**
     * Reports an issue to the health service.
     * @return
     */
    void reportIssue(String UID, String RID, HealthCheckReport report) throws NotRegisteredException;

    /**
     * Retrieves the ids of all reported problems for a given UID
     *
     */
     Set<String> getReportIds(String UID) throws NotRegisteredException;

    /**
     * Tells the Health Check Service that the issue has been resolved.
     *
     */
    void solveIssue(String UID, String id) throws NotRegisteredException;

    /**
     * Register to Health Check Service. This will return a unique ID which the caller will associate with issues
     */
    String register(Class T);

    /**
     * Retrieves map of health reports associated with the given UID
     * @param UID
     * @return
     */
    Map<String, HealthCheckReport> getReports(String UID) throws NotRegisteredException;
}

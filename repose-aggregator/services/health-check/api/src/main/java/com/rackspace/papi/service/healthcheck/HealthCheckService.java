package com.rackspace.papi.service.healthcheck;

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
    HealthCheckReport getDiagnosis(String id);

    /**
     * Reports an issue to the health service and returns the id associated with the issue.
     * @return
     */
    String reportIssue(HealthCheckReport report);

    /**
     * Retrieves the ids of all reported problems
     *
     */
     Set<String> getRepordIds();

    /**
     * Tells the Health Check Service that the issue has been resolved.
     *
     */
    void solveIssue(String id);
}

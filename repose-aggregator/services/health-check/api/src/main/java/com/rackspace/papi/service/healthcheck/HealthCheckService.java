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
     * Retrieves message associated with the latest issue
     * @param UID Id of class/user which reported the issue
     * @param id The id of issue
     * @return HealthReport containing details of issue reported
     * @throws NotRegisteredException If the UID is not registered
     * @throws InputNullException If passed null for UID or id
     */
    HealthCheckReport getDiagnosis(String UID, String id) throws NotRegisteredException, InputNullException;

    /**
     * Reports an issue to the health service
     * @param UID Id of class/user which reported the issue
     * @param RID The id of issue
     * @param report The HealthCheckReport which contains the details of the report
     * @throws NotRegisteredException If the UID is not registered
     * @throws InputNullException If passed null for UID or id
     */
    void reportIssue(String UID, String RID, HealthCheckReport report) throws NotRegisteredException, InputNullException;

    /**
     * Retrieves the ids of all reported problems for a given UID
     * @param UID Id of class/user which reported the issue
     * @return
     * @throws NotRegisteredException If the UID is not registered
     * @throws InputNullException If passed null for UID or id
     */
     Set<String> getReportIds(String UID) throws NotRegisteredException, InputNullException;

    /**
     * Tells the Health Check Service that the issue has been resolved.
     * @param UID Id of class/user which reported the issue
     * @param id The id of issue
     * @throws NotRegisteredException If the UID is not registered
     * @throws InputNullException If passed null for UID or id
     */
    void solveIssue(String UID, String id) throws NotRegisteredException, InputNullException;

    /**
     * Register to Health Check Service. This will return a unique ID which the caller will associate with issues
     * @param T Class registering with the Health Check Service
     * @return UID with which to report issues
     */
    String register(Class T);

    /**
     * Retrieves map of health reports associated with the given UID
     * @param UID Id of class/user which reported the issue
     * @return
     * @throws NotRegisteredException If the UID is not registered
     * @throws InputNullException If passed null for UID or id
     */
    Map<String, HealthCheckReport> getReports(String UID) throws NotRegisteredException, InputNullException;
}

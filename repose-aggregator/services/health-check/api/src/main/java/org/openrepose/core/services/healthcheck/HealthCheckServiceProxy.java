/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.healthcheck;

import java.util.Map;
import java.util.Set;

/**
 * An API proxy returned when registering with the HealthCheckService.
 * <p/>
 * This proxy stores a UID to uniquely identify the caller of the register method and provides a simplified API
 * by eliminating the need to provide a UID on each request.
 */
public interface HealthCheckServiceProxy {
    /**
     * Retrieves message associated with the latest issue
     *
     * @param issueName The id of issue. If null, behavior undefined.
     * @return HealthCheckReport containing details of issue reported
     */
    HealthCheckReport getDiagnosis(String issueName);

    /**
     * Reports an issue to the health service
     *
     * @param issueName The id of issue. If null, behavior undefined.
     * @param message   A detailed message to include in the HealthCheckReport
     * @param severity  The severity of the issues. If broken, Repose will return response code 503 until the issue
     *                  is resolved.
     */
    void reportIssue(String issueName, String message, Severity severity);

    /**
     * Retrieves the ids of all reported problems for a given UID
     *
     * @return IDs of all reports corresponding to the UID provided
     */
    Set<String> getReportIds();

    /**
     * Tells the Health Check Service that the issue has been resolved.
     *
     * @param issueName The id of issue. If null, behavior undefined.
     */
    void resolveIssue(String issueName);

    /**
     * Retrieves map of health reports associated with the given UID
     *
     * @return Map of all reports corresponding to the UID provided
     */
    Map<String, HealthCheckReport> getReports();

    /**
     * Deregister with the Health Check Service. After deregistering, this object should be disposed of as behavior
     * will be undefined.
     */
    void deregister();
}

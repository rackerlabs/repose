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
package org.openrepose.core.services.healthcheck.impl

import org.junit.Before
import org.junit.Test
import org.openrepose.core.services.healthcheck.HealthCheckReport
import org.openrepose.core.services.healthcheck.HealthCheckService
import org.openrepose.core.services.healthcheck.HealthCheckServiceProxy
import org.openrepose.core.services.healthcheck.Severity

import static org.hamcrest.Matchers.*
import static org.junit.Assert.*

class HealthCheckServiceImplTest {

    HealthCheckService healthCheckService;
    String h1Message = "Health report 1"
    String h2Message = "Health report 2"
    String h3Message = "Health report 3"
    String h4Message = "Health report 4"
    String h5Message = "Health report 5"
    Severity h1Severity = Severity.WARNING
    Severity h2Severity = Severity.BROKEN
    Severity h3Severity = Severity.BROKEN
    Severity h4Severity = Severity.WARNING
    Severity h5Severity = Severity.WARNING
    HealthCheckServiceProxy healthCheckServiceProxy;

    @Before
    void setUp() {
        healthCheckService = new HealthCheckServiceImpl();
        healthCheckServiceProxy = healthCheckService.register();
    }

    @Test
    void shouldReportUnHealthyWithBrokenReport() {
        healthCheckServiceProxy.reportIssue("id1", h1Message, h1Severity);
        healthCheckServiceProxy.reportIssue("id2", h2Message, h2Severity);

        assertFalse(healthCheckService.isHealthy())
    }

    @Test
    void shouldReportHealthyWithOnlyReportedWarnings() {
        healthCheckServiceProxy.reportIssue("id1", h1Message, h1Severity);
        healthCheckServiceProxy.reportIssue("id1", h4Message, h4Severity);
        healthCheckServiceProxy.reportIssue("id1", h5Message, h5Severity);

        assertTrue(healthCheckService.isHealthy())
    }

    @Test
    void shouldReturnReportByRid() {
        HealthCheckReport expectedReport = new HealthCheckReport(h1Message, h1Severity)

        healthCheckServiceProxy.reportIssue("id", h1Message, h1Severity)

        assertThat(healthCheckServiceProxy.getDiagnosis("id").level, equalTo(expectedReport.level))
        assertThat(healthCheckServiceProxy.getDiagnosis("id").message, equalTo(expectedReport.message))
    }

    @Test
    void shouldRemoveIssue() {
        HealthCheckReport expectedReport = new HealthCheckReport(h1Message, h1Severity)
        healthCheckServiceProxy.reportIssue("id", h1Message, h1Severity)

        assertThat(healthCheckServiceProxy.getDiagnosis("id").level, equalTo(expectedReport.level))
        assertThat(healthCheckServiceProxy.getDiagnosis("id").message, equalTo(expectedReport.message))

        healthCheckServiceProxy.resolveIssue("id")

        assertNull(healthCheckServiceProxy.getDiagnosis("id"))
    }

    @Test
    void shouldProvideIdsToRetrieveReportedIssues() {
        healthCheckServiceProxy.reportIssue("id1", h1Message, h1Severity);
        healthCheckServiceProxy.reportIssue("id2", h2Message, h2Severity);
        healthCheckServiceProxy.reportIssue("id3", h3Message, h3Severity);
        healthCheckServiceProxy.reportIssue("id4", h4Message, h4Severity);
        healthCheckServiceProxy.reportIssue("id5", h5Message, h5Severity);

        assertThat(healthCheckServiceProxy.getReportIds(), hasItem("id1"))
        assertThat(healthCheckServiceProxy.getReportIds(), hasItem("id2"))
        assertThat(healthCheckServiceProxy.getReportIds(), hasItem("id3"))
        assertThat(healthCheckServiceProxy.getReportIds(), hasItem("id4"))
        assertThat(healthCheckServiceProxy.getReportIds(), hasItem("id5"))
        assertThat(healthCheckServiceProxy.getReportIds(), not(hasItem("notAnId")))
    }

    @Test
    void shouldReturnReportsAssociatedWithUid() {
        healthCheckServiceProxy.reportIssue("id1", h1Message, h1Severity);
        healthCheckServiceProxy.reportIssue("id2", h2Message, h2Severity);
        healthCheckServiceProxy.reportIssue("id3", h3Message, h3Severity);

        HealthCheckServiceProxy healthCheckServiceProxy2 = healthCheckService.register()

        healthCheckServiceProxy2.reportIssue("id4", h4Message, h4Severity);
        healthCheckServiceProxy2.reportIssue("id5", h5Message, h5Severity);

        assertThat(healthCheckServiceProxy.getReports().keySet(), hasItem("id1"))
        assertThat(healthCheckServiceProxy.getReports().keySet(), hasItem("id2"))
        assertThat(healthCheckServiceProxy.getReports().keySet(), not(hasItem("id4")))
        assertThat(healthCheckServiceProxy.getReports().keySet(), not(hasItem("id5")))
    }

    @Test
    void shouldProvideHealthyResponseWhenNoIssuesReported() {
        assertTrue(healthCheckService.isHealthy())
    }

    @Test
    void shouldNotThrowErrorIfNoIssuesExist() {
        healthCheckServiceProxy.resolveIssue("notAnIssue")
    }
}

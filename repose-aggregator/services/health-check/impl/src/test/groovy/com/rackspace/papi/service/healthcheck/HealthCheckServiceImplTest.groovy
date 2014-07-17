package com.rackspace.papi.service.healthcheck

import org.junit.Before
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.CoreMatchers.not
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

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

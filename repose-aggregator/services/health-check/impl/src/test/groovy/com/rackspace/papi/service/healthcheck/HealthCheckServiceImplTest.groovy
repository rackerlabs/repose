package com.rackspace.papi.service.healthcheck

import org.junit.Before
import org.junit.Test

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
    HealthCheckService.HealthCheckServiceProxy healthCheckServiceProxy;

    @Before
    void setUp() {
        healthCheckService = new HealthCheckServiceImpl();
        healthCheckServiceProxy = healthCheckService.register(HealthCheckServiceImplTest.class);
    }

    @Test
    void shouldReportUnHealthyWithBrokenReport() {
        healthCheckServiceProxy.reportIssue("id1", h1Message, h1Severity);
        healthCheckServiceProxy.reportIssue("id2", h2Message, h2Severity);

        assert !healthCheckServiceProxy.isHealthy()
    }

    @Test
    void shouldReportHealthyWithOnlyReportedWarnings() {
        healthCheckServiceProxy.reportIssue("id1", h1Message, h1Severity);
        healthCheckServiceProxy.reportIssue("id1", h4Message, h4Severity);
        healthCheckServiceProxy.reportIssue("id1", h5Message, h5Severity);

        assert healthCheckService.isHealthy()
    }

    @Test
    void shouldReturnReportByRid() {
        HealthCheckReport expectedReport = new HealthCheckReport(h1Message, h1Severity)

        healthCheckServiceProxy.reportIssue("id", h1Message, h1Severity)

        assert expectedReport.level == healthCheckServiceProxy.getDiagnosis("id").level
        assert expectedReport.message.equals(healthCheckServiceProxy.getDiagnosis("id").message)
    }

    @Test
    void shouldRemoveIssue() {
        HealthCheckReport expectedReport = new HealthCheckReport(h1Message, h1Severity)
        healthCheckServiceProxy.reportIssue("id", h1Message, h1Severity)

        assert expectedReport.level == healthCheckServiceProxy.getDiagnosis("id").level
        assert expectedReport.message.equals(healthCheckServiceProxy.getDiagnosis("id").message)

        healthCheckServiceProxy.resolveIssue("id")

        assert healthCheckServiceProxy.getDiagnosis("id") == null
    }

    @Test
    void shouldProvideIdsToRetrieveReportedIssues() {
        healthCheckServiceProxy.reportIssue("id1", h1Message, h1Severity);
        healthCheckServiceProxy.reportIssue("id2", h2Message, h2Severity);
        healthCheckServiceProxy.reportIssue("id3", h3Message, h3Severity);
        healthCheckServiceProxy.reportIssue("id4", h4Message, h4Severity);
        healthCheckServiceProxy.reportIssue("id5", h5Message, h5Severity);

        assert healthCheckServiceProxy.getReportIds().contains("id1")
        assert healthCheckServiceProxy.getReportIds().contains("id2")
        assert healthCheckServiceProxy.getReportIds().contains("id3")
        assert healthCheckServiceProxy.getReportIds().contains("id4")
        assert healthCheckServiceProxy.getReportIds().contains("id5")
        assert !healthCheckServiceProxy.getReportIds().contains("notAnId")
    }

    @Test
    void shouldReturnReportsAssociatedWithUid() {
        healthCheckServiceProxy.reportIssue("id1", h1Message, h1Severity);
        healthCheckServiceProxy.reportIssue("id2", h2Message, h2Severity);
        healthCheckServiceProxy.reportIssue("id3", h3Message, h3Severity);

        HealthCheckService.HealthCheckServiceProxy healthCheckServiceProxy2 = healthCheckService.register(HealthCheckServiceImplTest.class)

        healthCheckServiceProxy2.reportIssue("id4", h4Message, h4Severity);
        healthCheckServiceProxy2.reportIssue("id5", h5Message, h5Severity);

        assert healthCheckServiceProxy.getReports().containsKey("id1")
        assert healthCheckServiceProxy.getReports().containsKey("id2")
        assert !healthCheckServiceProxy.getReports().containsKey("id4")
        assert !healthCheckServiceProxy.getReports().containsKey("id5")
    }

    @Test
    void shouldProvideHealthyResponseWhenNoIssuesReported() {
        assert healthCheckServiceProxy.isHealthy()
    }

    @Test
    void shouldNotThrowErrorIfNoIssuesExist() {
        healthCheckServiceProxy.resolveIssue("notAnIssue")
    }

    @Test
    void shouldGenerateUniqueUids() {
        def list = []
        for (int i = 0; i < 1000; i++) {
            list.push(healthCheckService.register(HealthCheckServiceImplTest.class).uid)
        }

        def set = list as Set

        assert set.size() == 1000
    }

    @Test(expected = IllegalArgumentException)
    void shouldReturnIllegalArgumentExceptionForRegisteringNullClass() {
        healthCheckService.register(null)
    }
}

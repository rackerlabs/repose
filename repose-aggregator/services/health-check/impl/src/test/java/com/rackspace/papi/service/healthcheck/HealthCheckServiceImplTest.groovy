package com.rackspace.papi.service.healthcheck

import org.junit.Before
import org.junit.Test

class HealthCheckServiceImplTest {

    HealthCheckService healthCheckService;
    HealthCheckReport h1, h2, h3, h4, h5;

    @Before
    void setUp() {

        healthCheckService = new HealthCheckServiceImpl();
        h1 = new HealthCheckReport("Health report 1", Severity.WARNING)
        h2 = new HealthCheckReport("Health report 2", Severity.BROKEN)
        h3 = new HealthCheckReport("Health report 3", Severity.BROKEN)
        h4 = new HealthCheckReport("Health report 4", Severity.WARNING)
        h5 = new HealthCheckReport("Health report 5", Severity.WARNING)

    }

    @Test
    void shouldReportUnHealthWithBrokenReport() {

        healthCheckService.reportIssue(h1);
        healthCheckService.reportIssue(h2);


        assert !healthCheckService.isHealthy()
    }

    @Test
    void shouldReportHealthyWithOnlyReportedWarnings() {

        healthCheckService.reportIssue(h1);
        healthCheckService.reportIssue(h4);
        healthCheckService.reportIssue(h5);

        assert healthCheckService.isHealthy()
    }

    @Test
    void shouldReturnValidIdToRetrieveReport() {

        String id = healthCheckService.reportIssue(h1);

        assert h1.level == healthCheckService.getDiagnosis(id).level
        assert h1.message.equals(healthCheckService.getDiagnosis(id).message)
    }

    @Test
    void shouldRemoveIssue() {

        String id = healthCheckService.reportIssue(h1);

        assert h1.level == healthCheckService.getDiagnosis(id).level
        assert h1.message.equals(healthCheckService.getDiagnosis(id).message)

        healthCheckService.solveIssue(id)

        assert healthCheckService.getDiagnosis(id) == null
    }

    @Test
    void shouldProvideIdsToRetrieveReportedIssues() {

        String id1 = healthCheckService.reportIssue(h1);
        String id2 = healthCheckService.reportIssue(h2);
        String id3 = healthCheckService.reportIssue(h3);
        String id4 = healthCheckService.reportIssue(h4);
        String id5 = healthCheckService.reportIssue(h5);

        assert healthCheckService.repordIds.contains(id1)
        assert healthCheckService.repordIds.contains(id2)
        assert healthCheckService.repordIds.contains(id3)
        assert healthCheckService.repordIds.contains(id4)
        assert healthCheckService.repordIds.contains(id5)
        assert !healthCheckService.repordIds.contains("NotAnId")
    }
}

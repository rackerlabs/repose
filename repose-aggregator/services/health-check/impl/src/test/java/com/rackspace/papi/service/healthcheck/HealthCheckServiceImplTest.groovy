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
    void shouldReportUnHealthyWithBrokenReport() {

        String uid = healthCheckService.register(this.class);

        healthCheckService.reportIssue(uid, "id1", h1);
        healthCheckService.reportIssue(uid, "id2", h2);


        assert !healthCheckService.isHealthy()
    }

    @Test
    void shouldReportHealthyWithOnlyReportedWarnings() {

        String uid = healthCheckService.register(this.class)
        healthCheckService.reportIssue(uid, "id1", h1);
        healthCheckService.reportIssue(uid, "id1", h4);
        healthCheckService.reportIssue(uid, "id1", h5);

        assert healthCheckService.isHealthy()
    }

    @Test
    void shouldReturnReportByUidAndRid() {

        String uid = healthCheckService.register(this.class)
        healthCheckService.reportIssue(uid, "id", h1)

        assert h1.level == healthCheckService.getDiagnosis(uid, "id").level
        assert h1.message.equals(healthCheckService.getDiagnosis(uid,"id").message)
    }

    @Test
    void shouldRemoveIssue() {

        String uid = healthCheckService.register(this.class)
        healthCheckService.reportIssue(uid, "id", h1)

        assert h1.level == healthCheckService.getDiagnosis(uid, "id").level
        assert h1.message.equals(healthCheckService.getDiagnosis(uid, "id").message)

        healthCheckService.solveIssue(uid, "id")

        assert healthCheckService.getDiagnosis(uid, "id") == null
    }

    @Test
    void shouldProvideIdsToRetrieveReportedIssues() {

        String uid = healthCheckService.register(this.class);
        healthCheckService.reportIssue(uid, "id1", h1);
        healthCheckService.reportIssue(uid, "id2", h2);
        healthCheckService.reportIssue(uid, "id3", h3);
        healthCheckService.reportIssue(uid, "id4", h4);
        healthCheckService.reportIssue(uid, "id5", h5);

        assert healthCheckService.getReportIds(uid).contains("id1")
        assert healthCheckService.getReportIds(uid).contains("id2")
        assert healthCheckService.getReportIds(uid).contains("id3")
        assert healthCheckService.getReportIds(uid).contains("id4")
        assert healthCheckService.getReportIds(uid).contains("id5")
        assert !healthCheckService.getReportIds(uid).contains("notAnId")

    }

    @Test
    void shouldReturnReportsAssociatedWithUid(){

        String uid = healthCheckService.register(this.class);
        healthCheckService.reportIssue(uid, "id1", h1);
        healthCheckService.reportIssue(uid, "id2", h2);
        healthCheckService.reportIssue(uid, "id3", h3);

        String uid2 = healthCheckService.register(String.class)

        healthCheckService.reportIssue(uid2, "id4", h4);
        healthCheckService.reportIssue(uid2, "id5", h5);

        assert healthCheckService.getReports(uid).containsKey("id1")
        assert healthCheckService.getReports(uid).containsKey("id2")
        assert !healthCheckService.getReports(uid).containsKey("id4")
        assert !healthCheckService.getReports(uid).containsKey("id5")

    }

    @Test
    void shouldProvideHealthyResponseWhenNoIssuesReported() {

        assert healthCheckService.isHealthy()
    }

    @Test(expected = NotRegisteredException.class)
    void shouldThrowNotRegisteredExceptionWhenRetrievingReportsListForUnregisteredUID(){

        healthCheckService.getReportIds("doesNotExist")
    }

    @Test(expected = NotRegisteredException.class)
    void shouldThrowNotRegisteredExceptionWhenRetrievingValidRidWithInvalidUID(){

        String uid = healthCheckService.register(this.class)
        healthCheckService.reportIssue(uid, "id", h1)

        healthCheckService.getDiagnosis(uid+"blah", "id")
    }

    @Test(expected = NotRegisteredException.class)
    void shouldThrowNotRegisteredExceptionWhenReportingHealthIssueToInvalidUID(){

        String uid = healthCheckService.register(this.class)

        healthCheckService.reportIssue(uid+"blah", "id", h1)
    }

    @Test(expected = NotRegisteredException.class)
    void shouldThrowNotRegisteredExceptionWhenRetrievingReportsOfUnregisteredUID(){

        String uid = healthCheckService.register(this.class)

        healthCheckService.getReports(uid+"blah")
    }

    @Test(expected = NotRegisteredException.class)
    void shouldThrowNotRegisteredExceptionWhenSolvingIssuesOfUnregisteredUID(){

        String uid = healthCheckService.register(this.class)

        healthCheckService.solveIssue(uid+"blah", "notThere")
    }

    @Test
    void shouldNotThrowErrorIfNoIssuesExist(){

        String uid = healthCheckService.register(this.class)
        healthCheckService.solveIssue(uid, "notAnIssue")
    }

    @Test
    void shouldGenerateUniqueUids(){

        String x = "HealthChecker"
        def list = []
        for(int i=0; i<1000; i++){

            list.push(healthCheckService.register(x.class))
        }

        def set = list as Set

        assert set.size() == 1000
    }

    @Test(expected = InputNullException)
    void shouldNotAllowNullOrBlankForReportIds(){

        String uid = healthCheckService.register(this.class)

        healthCheckService.reportIssue(uid, null, h1);
    }

    @Test(expected = InputNullException)
    void shouldReturnIdNullWhenGivenNullUid(){

        healthCheckService.reportIssue(null, "rid", h1);
    }

    @Test(expected = InputNullException)
    void shouldReturnInputNullExceptionForRegisteringNullClass(){

        healthCheckService.register(null)
    }

}

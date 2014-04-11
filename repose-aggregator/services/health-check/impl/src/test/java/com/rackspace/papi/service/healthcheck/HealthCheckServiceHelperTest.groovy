package com.rackspace.papi.service.healthcheck
import org.junit.Before
import org.junit.Test
import org.slf4j.Logger

import static org.mockito.Matchers.eq
import static org.mockito.Matchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify

class HealthCheckServiceHelperTest {
    private HealthCheckServiceHelper healthCheckServiceHelper

    private HealthCheckService hcs
    private Logger lgr

    @Before
    void setUp() {
        hcs = mock(HealthCheckService.class)
        lgr = mock(Logger.class)
        String uid = "test_uid"

        healthCheckServiceHelper = new HealthCheckServiceHelper(hcs, lgr, uid)
    }

    @Test(expected = IllegalArgumentException.class)
    void "when passed null arguments on construction, should throw IllegalArgumentException"() {
        new HealthCheckServiceHelper(null, null, null)
    }

    @Test
    void "when a valid issue is reported, should report the issue to the health check service"() {
        healthCheckServiceHelper.reportIssue("test_rid", "some_message", Severity.BROKEN)

        verify(hcs).reportIssue(eq("test_uid"), eq("test_rid"), any(HealthCheckReport.class))
        verify(lgr, never()).error(any(String.class))
    }

    @Test
    void "when a valid issue is resolved, should resolve the issue with the health check service"() {
        healthCheckServiceHelper.resolveIssue("test_rid")

        verify(hcs).solveIssue("test_uid", "test_rid")
        verify(lgr, never()).error(any(String.class))
    }

    //todo: add non-happy path testing
}

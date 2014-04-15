package com.rackspace.papi.service.healthcheck

import org.slf4j.Logger
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.*

class HealthCheckServiceHelperTest extends Specification {
    private static final TEST_UID = "test_uid"
    private static final TEST_RID = "test_rid"

    @Shared
    private HealthCheckServiceHelper healthCheckServiceHelper

    @Shared
    private HealthCheckService hcs

    @Shared
    private Logger lgr

    void setupSpec() {
        hcs = mock(HealthCheckService.class)
        lgr = mock(Logger.class)

        healthCheckServiceHelper = new HealthCheckServiceHelper(hcs, lgr, TEST_UID)
    }

    void "when passed non-null arguments on construction, should not throw IllegalArgumentException"() {
        when:
        new HealthCheckServiceHelper(hcs, lgr, TEST_UID)

        then:
        notThrown(IllegalArgumentException.class)
    }

    @Unroll("when HealthCheckServiceHelper(#constructorString) is constructed, should throw exception")
    void "when passed null arguments on construction, should throw IllegalArgumentException"() {
        when:
        new HealthCheckServiceHelper(healthCheckService, logger, uid)

        then:
        thrown(IllegalArgumentException.class)

        where:
        healthCheckService | logger | uid      | constructorString
        hcs                | lgr    | null     | "hcs, lgr, null"
        hcs                | null   | TEST_UID | "hcs, null, uid"
        null               | lgr    | TEST_UID | "null, lgr, uid"
        null               | null   | null     | "null, null, null"
    }

    void "when a valid issue is reported, should report the issue to the health check service"() {
        when:
        healthCheckServiceHelper.reportIssue(TEST_RID, "some_message", Severity.BROKEN)

        then:
        verify(hcs).reportIssue(eq(TEST_UID), eq(TEST_RID), any(HealthCheckReport.class))
        verify(lgr, never()).error(any(String.class))
    }

    void "when a valid issue is resolved, should resolve the issue with the health check service"() {
        when:
        healthCheckServiceHelper.resolveIssue(TEST_RID)

        then:
        verify(hcs).solveIssue(TEST_UID, TEST_RID)
        verify(lgr, never()).error(any(String.class))
    }

    //todo: add non-happy path testing
}

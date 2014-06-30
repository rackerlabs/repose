package com.rackspace.papi.service.healthcheck;

import org.slf4j.Logger;

public class HealthCheckServiceHelper {
    private final HealthCheckService healthCheckService;
    private final Logger logger;
    private final String uid;

    public HealthCheckServiceHelper(HealthCheckService hcs, Logger lgr, String uid) {
        if (hcs == null || lgr == null || uid == null) {
            throw new IllegalArgumentException("The HealthCheckService, Logger, and UID arguments may not be null");
        }
        this.healthCheckService = hcs;
        this.logger = lgr;
        this.uid = uid;
    }

    public void reportIssue(String rid, String message, Severity severity) {
        logger.debug("Reporting issue to Health Checker Service: " + rid);
        try {
            healthCheckService.reportIssue(uid, rid, new HealthCheckReport(message, severity));
        } catch (InputNullException e) {
            logger.error("Unable to report Issues to Health Check Service", e);
        } catch (NotRegisteredException e) {
            logger.error("Unable to report Issues to Health Check Service", e);
        }
    }

    public void resolveIssue(String rid) {
        try {
            healthCheckService.solveIssue(uid, rid);
            logger.debug("Resolving issue: " + rid);
        } catch (InputNullException ine) {
            logger.error("Unable to solve null uid issue", ine);
        } catch (NotRegisteredException nre) {
            // issue is not registered which means we're OK, don't log this (to prevent log spamming -- the health
            // check service needs to be cleaned up so that we don't have to handle exceptions as often)
        }
    }
}

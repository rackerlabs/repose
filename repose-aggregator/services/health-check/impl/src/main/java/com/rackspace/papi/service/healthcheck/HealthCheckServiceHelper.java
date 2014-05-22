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
            if(logger.isTraceEnabled()){
                logger.trace(e.getMessage());
                e.printStackTrace();
            }

            logger.error("Unable to report Issues to Health Check Service");
        } catch (NotRegisteredException e) {
            if(logger.isTraceEnabled()){
                logger.trace(e.getMessage());
                e.printStackTrace();
            }
            logger.error("Unable to report Issues to Health Check Service");
        }
    }

    public void resolveIssue(String rid) {
        try {
            logger.debug("Resolving issue: " + rid);
            healthCheckService.solveIssue(uid, rid);
        } catch (InputNullException e) {
            if(logger.isTraceEnabled()){
                logger.trace(e.getMessage());
                e.printStackTrace();
            }
            logger.error("Unable to solve issue " + rid + "from " + uid);
        } catch (NotRegisteredException e) {
            if(logger.isTraceEnabled()){
                logger.trace(e.getMessage());
                e.printStackTrace();
            }
            logger.error("Unable to solve issue " + rid + "from " + uid);
        }
    }
}

package com.rackspace.papi.service.healthcheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HealthCheckServiceImpl implements HealthCheckService {
    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckServiceImpl.class);

    private Map<UUID, Map<String, HealthCheckReport>> reports = new ConcurrentHashMap<>();

    @Override
    public HealthCheckServiceProxy register() {
        Map<String, HealthCheckReport> reportMap = new HashMap<>();
        UUID rand = UUID.randomUUID();
        reports.put(rand, reportMap);
        return new HealthCheckServiceProxyImpl(rand);
    }

    @Override
    public boolean isHealthy() {
        for (Map.Entry<UUID, Map<String, HealthCheckReport>> stringMapEntry : reports.entrySet()) {
            for (Map.Entry<String, HealthCheckReport> entry : stringMapEntry.getValue().entrySet()) {

                if (entry.getValue().getLevel().equals(Severity.BROKEN)) {
                    return false;
                }
            }
        }

        return true;
    }

    public void deregister(UUID uid) {
        reports.remove(uid);
    }

    private HealthCheckReport getDiagnosis(UUID uid, String issueName) {
        return reports.get(uid).get(issueName);
    }

    private void reportIssue(UUID uid, String issueName, HealthCheckReport report) {
        LOG.info("HealthCheckService.reportIssue: " + issueName + " reported by " + uid);

        reports.get(uid).put(issueName, report);
    }

    private Set<String> getReportIds(UUID uid) {
        return reports.get(uid).keySet();
    }

    private void resolveIssue(UUID uid, String issueName) {
        Iterator<String> itr = reports.get(uid).keySet().iterator();

        while (itr.hasNext()) {
            String cur = itr.next();
            if (issueName.equals(cur)) {
                LOG.info("HealthCheckService.resolveIssue: " + issueName + " resolved by " + uid);

                itr.remove();
            }
        }
    }

    private Map<String, HealthCheckReport> getReports(UUID uid) {
        return reports.get(uid);
    }

    private class HealthCheckServiceProxyImpl implements HealthCheckServiceProxy {
        private UUID uid;

        private HealthCheckServiceProxyImpl(UUID uid) {
            this.uid = uid;
        }

        @Override
        public HealthCheckReport getDiagnosis(String issueName) {
            return HealthCheckServiceImpl.this.getDiagnosis(uid, issueName);
        }

        @Override
        public void reportIssue(String issueName, String message, Severity severity) {
            HealthCheckServiceImpl.this.reportIssue(uid, issueName, new HealthCheckReport(message, severity));
        }

        @Override
        public Set<String> getReportIds() {
            return HealthCheckServiceImpl.this.getReportIds(uid);
        }

        @Override
        public void resolveIssue(String issueName) {
            HealthCheckServiceImpl.this.resolveIssue(uid, issueName);
        }

        @Override
        public Map<String, HealthCheckReport> getReports() {
            return HealthCheckServiceImpl.this.getReports(uid);
        }

        @Override
        public void deregister() {
            HealthCheckServiceImpl.this.deregister(uid);
        }
    }
}

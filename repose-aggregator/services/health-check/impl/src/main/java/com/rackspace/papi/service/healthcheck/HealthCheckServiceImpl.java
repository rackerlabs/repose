package com.rackspace.papi.service.healthcheck;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HealthCheckServiceImpl implements HealthCheckService {

    private Map<UUID, Map<String, HealthCheckReport>> reports = new ConcurrentHashMap<>();

    @Override
    public HealthCheckServiceProxy register() {
        Map<String, HealthCheckReport> reportMap = new HashMap<>();
        UUID rand = UUID.randomUUID();
        reports.put(rand, reportMap);
        return new HealthCheckServiceProxyImpl(rand);
    }

    public void deregister(UUID uid) {
        reports.remove(uid);
    }

    private boolean isHealthy() {
        for (Map.Entry<UUID, Map<String, HealthCheckReport>> stringMapEntry : reports.entrySet()) {
            for (Map.Entry<String, HealthCheckReport> entry : stringMapEntry.getValue().entrySet()) {

                if (entry.getValue().getLevel().equals(Severity.BROKEN)) {
                    return false;
                }
            }
        }

        return true;
    }

    private HealthCheckReport getDiagnosis(UUID uid, String id) {
        return reports.get(uid).get(id);
    }

    private void reportIssue(UUID uid, String rid, HealthCheckReport report) {
        reports.get(uid).put(rid, report);
    }

    private Set<String> getReportIds(UUID uid) {
        return reports.get(uid).keySet();
    }

    private void resolveIssue(UUID uid, String id) {
        resolveIssue(id, reports.get(uid));
    }

    private void resolveIssue(String id, Map<String, HealthCheckReport> reportMap) {
        Iterator<String> itr = reportMap.keySet().iterator();

        while (itr.hasNext()) {
            String cur = itr.next();
            if (id.equals(cur)) {
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
        public UUID getUid() {
            return uid;
        }

        @Override
        public boolean isHealthy() {
            return HealthCheckServiceImpl.this.isHealthy();
        }

        @Override
        public HealthCheckReport getDiagnosis(String id) {
            return HealthCheckServiceImpl.this.getDiagnosis(uid, id);
        }

        @Override
        public void reportIssue(String rid, String message, Severity severity) {
            HealthCheckServiceImpl.this.reportIssue(uid, rid, new HealthCheckReport(message, severity));
        }

        @Override
        public Set<String> getReportIds() {
            return HealthCheckServiceImpl.this.getReportIds(uid);
        }

        @Override
        public void resolveIssue(String id) {
            HealthCheckServiceImpl.this.resolveIssue(uid, id);
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

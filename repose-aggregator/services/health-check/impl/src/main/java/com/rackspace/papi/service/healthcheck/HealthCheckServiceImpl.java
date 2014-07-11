package com.rackspace.papi.service.healthcheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HealthCheckServiceImpl implements HealthCheckService {
    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckServiceImpl.class);

    private Map<String, Map<String, HealthCheckReport>> reports;

    @Autowired
    public HealthCheckServiceImpl() {
        reports = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isHealthy() {
        for (Map.Entry<String, Map<String, HealthCheckReport>> stringMapEntry : reports.entrySet()) {
            for (Map.Entry<String, HealthCheckReport> entry : stringMapEntry.getValue().entrySet()) {
                if (entry.getValue().getLevel().equals(Severity.BROKEN)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public HealthCheckReport getDiagnosis(String uid, String id) {
        return reports.get(uid).get(id);
    }

    @Override
    public void reportIssue(String uid, String rid, HealthCheckReport report) {
        reports.get(uid).put(rid, report);
    }

    @Override
    public Set<String> getReportIds(String uid) {
        return reports.get(uid).keySet();
    }

    @Override
    public void resolveIssue(String uid, String id) {
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

    @Override
    public HealthCheckServiceProxy register(Class T) {
        if (T == null) {
            throw new IllegalArgumentException("Registering Class");
        }
        Map<String, HealthCheckReport> reportMap = new HashMap<>();
        Long rand = UUID.randomUUID().getMostSignificantBits();
        String uid = T.getName() + ":" + rand.toString(); // TODO: Decide whether or not to tie each UID to a specific class
        reports.put(uid, reportMap);
        return new HealthCheckServiceProxyImpl(uid);
    }

    @Override
    public Map<String, HealthCheckReport> getReports(String uid) {
        return reports.get(uid);
    }

    private class HealthCheckServiceProxyImpl implements HealthCheckServiceProxy {
        private String uid;

        private HealthCheckServiceProxyImpl(String uid) {
            this.uid = uid;
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
    }
}

package com.rackspace.papi.service.healthcheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HealthCheckServiceImpl implements HealthCheckService {
    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckServiceImpl.class);

    private Map<HealthCheckServiceProxy, Map<String, HealthCheckReport>> reports = new ConcurrentHashMap<>();

    @Override
    public HealthCheckServiceProxy register() {
        HealthCheckServiceProxy proxy = new HealthCheckServiceProxyImpl();
        Map<String, HealthCheckReport> reportMap = new HashMap<>();
        reports.put(proxy, reportMap);
        return proxy;
    }

    @Override
    public boolean isHealthy() {
        for (Map.Entry<HealthCheckServiceProxy, Map<String, HealthCheckReport>> stringMapEntry : reports.entrySet()) {
            for (Map.Entry<String, HealthCheckReport> entry : stringMapEntry.getValue().entrySet()) {
                if (entry.getValue().getLevel().equals(Severity.BROKEN)) {
                    return false;
                }
            }
        }

        return true;
    }

    public void deregister(HealthCheckServiceProxy proxy) {
        reports.remove(proxy);
    }

    private HealthCheckReport getDiagnosis(HealthCheckServiceProxy proxy, String issueName) {
        return reports.get(proxy).get(issueName);
    }

    private void reportIssue(HealthCheckServiceProxy proxy, String issueName, HealthCheckReport report) {
        LOG.info("HealthCheckService.reportIssue: " + issueName + " reported by " + System.identityHashCode(proxy));

        reports.get(proxy).put(issueName, report);
    }

    private Set<String> getReportIds(HealthCheckServiceProxy proxy) {
        return reports.get(proxy).keySet();
    }

    private void resolveIssue(HealthCheckServiceProxy proxy, String issueName) {
        Iterator<String> itr = reports.get(proxy).keySet().iterator();

        while (itr.hasNext()) {
            String cur = itr.next();
            if (cur.equals(issueName)) {
                LOG.info("HealthCheckService.resolveIssue: " + issueName + " resolved by " + System.identityHashCode(proxy));

                itr.remove();
            }
        }
    }

    private Map<String, HealthCheckReport> getReports(HealthCheckServiceProxy proxy) {
        return reports.get(proxy);
    }

    private class HealthCheckServiceProxyImpl implements HealthCheckServiceProxy {
        @Override
        public HealthCheckReport getDiagnosis(String issueName) {
            return HealthCheckServiceImpl.this.getDiagnosis(this, issueName);
        }

        @Override
        public void reportIssue(String issueName, String message, Severity severity) {
            HealthCheckServiceImpl.this.reportIssue(this, issueName, new HealthCheckReport(message, severity));
        }

        @Override
        public Set<String> getReportIds() {
            return HealthCheckServiceImpl.this.getReportIds(this);
        }

        @Override
        public void resolveIssue(String issueName) {
            HealthCheckServiceImpl.this.resolveIssue(this, issueName);
        }

        @Override
        public Map<String, HealthCheckReport> getReports() {
            return HealthCheckServiceImpl.this.getReports(this);
        }

        @Override
        public void deregister() {
            HealthCheckServiceImpl.this.deregister(this);
        }
    }
}

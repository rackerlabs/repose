package com.rackspace.papi.service.healthcheck;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HealthCheckServiceImpl implements HealthCheckService {

    Map<String, Map<String, HealthCheckReport>> reports;

    public HealthCheckServiceImpl() {

        reports = new ConcurrentHashMap<String, Map<String, HealthCheckReport>>();
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
    public HealthCheckReport getDiagnosis(String UID, String id) throws NotRegisteredException {
        if (reports.keySet().contains(UID)) {
            return reports.get(UID).get(id);
        } else {
            throw new NotRegisteredException(UID);
        }
    }

    @Override
    public void reportIssue(String UID, String RID, HealthCheckReport report) throws NotRegisteredException {

        if (reports.containsKey(UID)) {
            reports.get(UID).put(RID, report);
        } else {
            throw new NotRegisteredException(UID);
        }
    }

    @Override
    public Set<String> getReportIds(String UID) throws NotRegisteredException {

        if (reports.containsKey(UID)) {
            return reports.get(UID).keySet();
        } else {
            throw new NotRegisteredException(UID);
        }
    }

    @Override
    public void solveIssue(String UID, String id) throws NotRegisteredException {

        if (reports.containsKey(UID)) {
            solveIssue(id, reports.get(UID));
        } else {
            throw new NotRegisteredException(UID);
        }
    }

    private void solveIssue(String id, Map<String, HealthCheckReport> reportMap) {

        Iterator<String> itr = reportMap.keySet().iterator();

        while (itr.hasNext()) {
            String cur = itr.next();
            if (id.equals(cur)) {
                itr.remove();
            }
        }
    }

    @Override
    public String register(Class T) {

        Map<String, HealthCheckReport> reportMap = new HashMap<String, HealthCheckReport>();
        Long rand = UUID.randomUUID().getMostSignificantBits();
        String UID = T.getName() + ":" + rand.toString();
        reports.put(UID, reportMap);
        return UID;
    }

    @Override
    public Map<String, HealthCheckReport> getReports(String UID) throws NotRegisteredException {

        if (reports.containsKey(UID)) {
            return reports.get(UID);
        } else {
            throw new NotRegisteredException(UID);
        }
    }
}

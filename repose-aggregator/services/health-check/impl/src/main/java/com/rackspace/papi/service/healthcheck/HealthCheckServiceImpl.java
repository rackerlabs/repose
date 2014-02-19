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
    public HealthCheckReport getDiagnosis(String UID, String id) throws NotRegisteredException, InputNullException {
        checkUid(UID);
        return reports.get(UID).get(id);
    }

    @Override
    public void reportIssue(String UID, String RID, HealthCheckReport report) throws NotRegisteredException, InputNullException {

        checkUid(UID);
        checkIdNull(RID);
        reports.get(UID).put(RID, report);
    }

    @Override
    public Set<String> getReportIds(String UID) throws NotRegisteredException, InputNullException {

        checkUid(UID);
        return reports.get(UID).keySet();
    }

    @Override
    public void solveIssue(String UID, String id) throws NotRegisteredException, InputNullException {

        checkUid(UID);
        checkIdNull(id);
        solveIssue(id, reports.get(UID));
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
    public String register(Class T) throws InputNullException {

        if(T == null){
            throw new InputNullException("Registering Class");
        }
        Map<String, HealthCheckReport> reportMap = new HashMap<String, HealthCheckReport>();
        Long rand = UUID.randomUUID().getMostSignificantBits();
        String UID = T.getName() + ":" + rand.toString();
        reports.put(UID, reportMap);
        return UID;
    }

    @Override
    public Map<String, HealthCheckReport> getReports(String UID) throws NotRegisteredException, InputNullException {

        checkUid(UID);
        return reports.get(UID);

    }

    private void checkUid(String uid) throws NotRegisteredException, InputNullException {

        checkIdNull(uid);
        if (!reports.containsKey(uid)) {
            throw new NotRegisteredException(uid);
        }
    }

    private void checkIdNull(String id) throws InputNullException {

        if (id == null) {
            throw new InputNullException();
        }
    }
}

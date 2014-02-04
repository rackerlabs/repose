package com.rackspace.papi.service.healthcheck;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HealthCheckServiceImpl implements HealthCheckService{

    Map<String, HealthCheckReport> reports;

    public HealthCheckServiceImpl(){

        reports = new ConcurrentHashMap<String, HealthCheckReport>();
    }

    @Override
    public boolean isHealthy() {

        for(Map.Entry<String, HealthCheckReport> entry : reports.entrySet()){

            if (entry.getValue().getLevel().equals(Severity.BROKEN)){
                return false;
            }
        }

        return true;
    }

    @Override
    public HealthCheckReport getDiagnosis(String id) {
        return reports.get(id);
    }

    @Override
    public String reportIssue(HealthCheckReport report) {

        Long reportId = UUID.randomUUID().getMostSignificantBits();
        reports.put(reportId.toString(), report);
        return reportId.toString();
    }

    @Override
    public Set<String> getRepordIds() {
        return reports.keySet();
    }

    @Override
    public void solveIssue(String id) {

        Iterator<String> itr = reports.keySet().iterator();

        while(itr.hasNext()){
            String cur = itr.next();
            if(id.equals(cur)){
                itr.remove();
            }
        }
    }
}

/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.healthcheck.impl;

import org.openrepose.core.services.healthcheck.HealthCheckReport;
import org.openrepose.core.services.healthcheck.HealthCheckService;
import org.openrepose.core.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.core.services.healthcheck.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Named
public class HealthCheckServiceImpl implements HealthCheckService {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckServiceImpl.class);

    private Map<HealthCheckServiceProxy, Map<String, HealthCheckReport>> reports = new ConcurrentHashMap<>();

    @Override
    public HealthCheckServiceProxy register() {
        HealthCheckServiceProxy proxy = new HealthCheckServiceProxyImpl();
        reports.put(proxy, new HashMap<>());
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

    private class HealthCheckServiceProxyImpl implements HealthCheckServiceProxy {
        @Override
        public HealthCheckReport getDiagnosis(String issueName) {
            return HealthCheckServiceImpl.this.reports.get(this).get(issueName);
        }

        @Override
        public void reportIssue(String issueName, String message, Severity severity) {
            LOG.info("HealthCheckService.reportIssue: " + issueName + " reported by " + System.identityHashCode(this));
            HealthCheckServiceImpl.this.reports.get(this).put(issueName, new HealthCheckReport(message, severity));
        }

        @Override
        public Set<String> getReportIds() {
            return HealthCheckServiceImpl.this.reports.get(this).keySet();
        }

        @Override
        public void resolveIssue(String issueName) {
            Iterator<String> itr = reports.get(this).keySet().iterator();

            while (itr.hasNext()) {
                String cur = itr.next();
                if (cur.equals(issueName)) {
                    LOG.info("HealthCheckService.resolveIssue: " + issueName + " resolved by " + System.identityHashCode(this));

                    itr.remove();
                }
            }
        }

        @Override
        public Map<String, HealthCheckReport> getReports() {
            return HealthCheckServiceImpl.this.reports.get(this);
        }

        @Override
        public void deregister() {
            HealthCheckServiceImpl.this.reports.remove(this);
        }
    }
}

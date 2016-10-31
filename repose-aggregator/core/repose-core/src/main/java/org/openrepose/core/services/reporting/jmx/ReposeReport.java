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
package org.openrepose.core.services.reporting.jmx;

import org.openrepose.core.services.reporting.ReportingService;
import org.openrepose.core.services.reporting.destinations.DestinationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Named("reposeReport")
@ManagedResource(objectName = "org.openrepose.core.services.reporting:type=ReposeReport", description = "Repose report MBean.")
public class ReposeReport implements ReposeReportMBean {

    private static final Logger LOG = LoggerFactory.getLogger(ReposeReport.class);
    private static final int STATUS_CODE_400 = 400;
    private static final int STATUS_CODE_500 = 500;
    private final ReportingService reportingService;

    @Inject
    public ReposeReport(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @Override
    @ManagedOperation
    public Date getLastReset() {
        return reportingService.getLastReset();
    }

    @Override
    @ManagedOperation
    public String getTotal400sReposeToClient() {
        LOG.debug("JMX: Retrieving total number of 400s from Repose to Client.");
        return Long.toString(reportingService.getReposeInfo().getTotalStatusCode(STATUS_CODE_400));
    }

    @Override
    @ManagedOperation
    public String getTotal500sReposeToClient() {
        LOG.debug("JMX: Retrieving total number of 500s from Repose to Client.");
        return Long.toString(reportingService.getReposeInfo().getTotalStatusCode(STATUS_CODE_500));
    }

    @Override
    @ManagedOperation
    public List<CompositeData> getDestinationInfo() throws OpenDataException {
        List<CompositeData> compositeDataList = new ArrayList<>();

        LOG.debug("JMX: Retrieving destination information.");
        for (DestinationInfo destination : reportingService.getDestinations()) {
            final DestinationCompositeDataBuilder dataBuilder = new DestinationCompositeDataBuilder(destination);
            compositeDataList.add(dataBuilder.toCompositeData());
        }

        return compositeDataList;
    }
}

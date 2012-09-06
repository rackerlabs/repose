package com.rackspace.papi.service.reporting.jmx;

import com.rackspace.papi.service.reporting.ReportingService;
import com.rackspace.papi.service.reporting.destinations.DestinationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import javax.management.openmbean.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component("reposeReport")
@ManagedResource(objectName = "com.rackspace.papi.service.reporting:type=ReposeReport", description="Repose report MBean.")
public class ReposeReport implements ReposeReportMBean {

    private static final Logger LOG = LoggerFactory.getLogger(ReposeReport.class);
    private static final int STATUS_CODE_400 = 400;
    private static final int STATUS_CODE_500 = 500;
    private final ReportingService reportingService;

    @Autowired
    public ReposeReport(@Qualifier("reportingService") ReportingService reportingService) {
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
        List<CompositeData> compositeDataList = new ArrayList<CompositeData>();

        LOG.debug("JMX: Retrieving destination information.");
        for (DestinationInfo destination : reportingService.getDestinations()) {
            final DestinationCompositeDataBuilder dataBuilder = new DestinationCompositeDataBuilder(destination);
            compositeDataList.add(dataBuilder.toCompositeData());
        }

        return compositeDataList;
    }
}

package com.rackspace.repose.management.reporting;

import com.rackspace.papi.service.reporting.jmx.ReposeReportMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: fran
 * Date: Oct 24, 2012
 * Time: 2:11:33 PM
 */
public class ReposeReportMBeanAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ReposeReportMBeanAdapter.class);

    public Report getReportingData(ReposeReportMBean reportMBean) {
        Report report = new Report();

        report.setLastResetDate(getFormattedDate(reportMBean.getLastReset()));
        report.setTotal400sReposeToClient(reportMBean.getTotal400sReposeToClient());
        report.setTotal500sReposeToClient(reportMBean.getTotal500sReposeToClient());

        List<Destination> destinations = new ArrayList<Destination>();
        try {
            for (CompositeData compositeData : reportMBean.getDestinationInfo()) {
                destinations.add(getDestinationData(compositeData));
            }
        } catch (OpenDataException e) {
            LOG.error("Problem retrieving destination information via JMX.");
        }

        report.setDestinations(destinations);

        return report;
    }

    private String getFormattedDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat();

        return dateFormat.format(date);
    }

    private final String[] keys = new String[]{"destinationId", "totalRequests", "total400s", "total500s", "responseTimeInMillis", "throughputInSeconds"};

    private Destination getDestinationData(CompositeData compositeData) {
        Destination destination = new Destination();

        if (compositeData.containsKey(keys[0])) {
            destination.setDestinationId((String) compositeData.get(keys[0]));
        }

        if (compositeData.containsKey(keys[1])) {
            destination.setTotalRequests(compositeData.get(keys[1]).toString());
        }

        if (compositeData.containsKey(keys[2])) {
            destination.setTotal400s(compositeData.get(keys[2]).toString());
        }

        if (compositeData.containsKey(keys[3])) {
            destination.setTotal500s(compositeData.get(keys[3]).toString());
        }

        if (compositeData.containsKey(keys[4])) {
            destination.setResponseTimeInMillis(compositeData.get(keys[4]).toString());
        }

        if (compositeData.containsKey(keys[5])) {
            destination.setThroughputInSeconds(compositeData.get(keys[5]).toString());
        }

        return destination;
    }
}

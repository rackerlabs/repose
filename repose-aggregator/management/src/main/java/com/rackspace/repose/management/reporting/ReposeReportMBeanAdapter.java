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
    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int TWO = 2;
    private static final int THREE = 3;
    private static final int FOUR = 4;
    private static final int FIVE = 5;

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

        if (compositeData.containsKey(keys[ZERO])) {
            destination.setDestinationId((String) compositeData.get(keys[ZERO]));
        }

        if (compositeData.containsKey(keys[ONE])) {
            destination.setTotalRequests(compositeData.get(keys[ONE]).toString());
        }

        if (compositeData.containsKey(keys[TWO])) {
            destination.setTotal400s(compositeData.get(keys[TWO]).toString());
        }

        if (compositeData.containsKey(keys[THREE])) {
            destination.setTotal500s(compositeData.get(keys[THREE]).toString());
        }

        if (compositeData.containsKey(keys[FOUR])) {
            destination.setResponseTimeInMillis(compositeData.get(keys[FOUR]).toString());
        }

        if (compositeData.containsKey(keys[FIVE])) {
            destination.setThroughputInSeconds(compositeData.get(keys[FIVE]).toString());
        }

        return destination;
    }
}

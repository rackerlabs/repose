package com.rackspace.papi.service.reporting.jmx;

import com.rackspace.papi.service.reporting.destinations.DestinationInfo;

import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.util.ArrayList;
import java.util.List;

public class DestinationCompositeDataBuilder extends CompositeDataBuilder {

    private static final int STATUS_CODE_400 = 400;
    private static final int STATUS_CODE_500 = 500;
    private final DestinationInfo destinationInfo;

    public DestinationCompositeDataBuilder(DestinationInfo destinationInfo) {
        this.destinationInfo = destinationInfo;
    }

    @Override
    public String getItemName() {
        return destinationInfo.getDestinationId();
    }

    @Override
    public String getDescription() {
        return "Information about destination id " + destinationInfo.getDestinationId() + ".";
    }

    @Override
    public String[] getItemNames() {
        return new String[]{"destinationId", "totalRequests", "total400s", "total500s", "responseTimeInMillis", "throughputInSeconds"};
    }

    @Override
    public String[] getItemDescriptions() {
        return new String[]{"The repose system-model id of the destination.",
                "The total number of requests sent to this destination.",
                "The total number of 400 response codes received from this destination.",
                "The total number of 500 response codes received from this destination.",
                "Average response time in milliseconds from this destination.",
                "Throughput in requests/second to this destination."};
    }

    @Override
    public OpenType[] getItemTypes() {
        return new OpenType[]{SimpleType.STRING, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.DOUBLE, SimpleType.DOUBLE};
    }

    @Override
    public Object[] getItems() {
        final List<Object> itemsB = new ArrayList<Object>();

        itemsB.add(destinationInfo.getDestinationId());
        itemsB.add(destinationInfo.getTotalRequests());
        itemsB.add(destinationInfo.getTotalStatusCode(STATUS_CODE_400));
        itemsB.add(destinationInfo.getTotalStatusCode(STATUS_CODE_500));
        itemsB.add(destinationInfo.getAverageResponseTime());
        itemsB.add(destinationInfo.getThroughput());

        return itemsB.toArray();
    }
}

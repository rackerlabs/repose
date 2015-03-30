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

import org.openrepose.core.services.reporting.destinations.DestinationInfo;

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

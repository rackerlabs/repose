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
package org.openrepose.core.services.reporting.destinations.impl;

import com.google.common.base.Objects;
import org.openrepose.core.services.reporting.StatusCodeResponseStore;
import org.openrepose.core.services.reporting.destinations.DestinationInfo;

import java.util.Map;


public class DestinationInfoLogic implements DestinationInfo {

    private static final int INT_ONE = 1;
    private static final long LONG_ZERO = 0l;
    private static final long LONG_ONE = 1l;
    private static final double DOUBLE_ZERO = 0.0d;
    private static final double DOUBLE_THOUSAND = 1000d;
    private DestinationInfoStore dataStore;

    public DestinationInfoLogic(String destinationAuthority) {
        dataStore = new DestinationInfoStore(destinationAuthority);
    }

    private DestinationInfoLogic(DestinationInfoLogic destinationInfoLogic) {
        dataStore = new DestinationInfoStore(destinationInfoLogic.dataStore);
    }

    public double elapsedTimeInSeconds() {
        return (System.currentTimeMillis() - dataStore.getStartTime()) / DOUBLE_THOUSAND;
    }

    public Map<Integer, StatusCodeResponseStore> getStatusCodeCounts() {
        return dataStore.getStatusCodeCounts();
    }

    public long getAccumulatedResponseTime() {
        return dataStore.getAccumulatedResponseTime();
    }

    public long getTotalResponses() {
        return dataStore.getTotalResponses();
    }

    @Override
    public void incrementRequestCount() {
        dataStore.setTotalRequests(dataStore.getTotalRequests() + INT_ONE);
    }

    @Override
    public void incrementResponseCount() {
        dataStore.setTotalResponses(dataStore.getTotalResponses() + INT_ONE);
    }

    @Override
    public void incrementStatusCodeCount(int statusCode, long time) {
        StatusCodeResponseStore value = dataStore.getStatusCodeCounts().get(statusCode);

        if (value != null) {
            dataStore.getStatusCodeCounts().put(statusCode, value.update(1, time));
        } else {
            dataStore.getStatusCodeCounts().put(statusCode, new StatusCodeResponseStore(LONG_ONE, time));
        }
    }

    @Override
    public void accumulateResponseTime(long responseTime) {
        dataStore.setAccumulatedResponseTime(dataStore.getAccumulatedResponseTime() + responseTime);
    }

    @Override
    public String getDestinationId() {
        return dataStore.getDestinationId();
    }

    @Override
    public long getTotalRequests() {
        return dataStore.getTotalRequests();
    }

    @Override
    public long getTotalStatusCode(int statusCode) {
        StatusCodeResponseStore count = dataStore.getStatusCodeCounts().get(statusCode);

        if (count != null) {
            return count.getTotalCount();
        } else {
            return LONG_ZERO;
        }
    }

    @Override
    public long getTotalResponseTime(int statusCode) {
        StatusCodeResponseStore count = dataStore.getStatusCodeCounts().get(statusCode);

        if (count != null) {
            return count.getAccumulatedResponseTime();
        } else {
            return LONG_ZERO;
        }
    }

    @Override
    public double getAverageResponseTime() {
        double averageResponseTime = (double) dataStore.getTotalResponses() / dataStore.getAccumulatedResponseTime();

        if (Double.isNaN(averageResponseTime)) {
            return DOUBLE_ZERO;
        } else {
            return averageResponseTime;
        }
    }

    @Override
    public double getThroughput() {
        double throughput = (double) dataStore.getTotalResponses() / elapsedTimeInSeconds();

        if (Double.isNaN(throughput)) {
            return DOUBLE_ZERO;
        } else {
            return throughput;
        }
    }

    @Override
    public DestinationInfo copy() {
        return new DestinationInfoLogic(this);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o instanceof DestinationInfoLogic) {
            DestinationInfoLogic other = (DestinationInfoLogic) o;

            return Objects.equal(this.dataStore, other.dataStore);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dataStore);
    }
}

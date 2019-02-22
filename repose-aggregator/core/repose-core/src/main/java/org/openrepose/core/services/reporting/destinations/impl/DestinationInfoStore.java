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

import java.util.HashMap;
import java.util.Map;

public class DestinationInfoStore {

    private static final long LONG_ZERO = 0L;
    private final String destinationId;
    private final long startTime;
    private long totalRequests = LONG_ZERO;
    private long totalResponses = LONG_ZERO;
    private Map<Integer, StatusCodeResponseStore> statusCodeCounts = new HashMap<>();
    private long accumulatedResponseTime = LONG_ZERO;

    public DestinationInfoStore(String destinationId) {
        this.destinationId = destinationId;
        this.startTime = System.currentTimeMillis();
    }

    public DestinationInfoStore(DestinationInfoStore destinationInfoStore) {
        this(destinationInfoStore.destinationId, destinationInfoStore.startTime,
                destinationInfoStore.totalRequests, destinationInfoStore.totalResponses,
                destinationInfoStore.statusCodeCounts, destinationInfoStore.accumulatedResponseTime);
    }

    private DestinationInfoStore(String destinationId, long startTime, long totalRequests, long totalResponses,
                                 Map<Integer, StatusCodeResponseStore> statusCodeCounts, long accumulatedResponseTime) {
        this.destinationId = destinationId;
        this.startTime = startTime;
        this.totalRequests = totalRequests;
        this.totalResponses = totalResponses;
        this.statusCodeCounts = deepCopyStatusCodeCounts(statusCodeCounts);
        this.accumulatedResponseTime = accumulatedResponseTime;
    }

    private static Map<Integer, StatusCodeResponseStore> deepCopyStatusCodeCounts(Map<Integer, StatusCodeResponseStore> statusCodeCounts) {
        Map<Integer, StatusCodeResponseStore> copy = new HashMap<>();
        for (Map.Entry<Integer, StatusCodeResponseStore> entry : statusCodeCounts.entrySet()) {
            copy.put(entry.getKey(), new StatusCodeResponseStore(entry.getValue()));
        }

        return copy;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    protected void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getTotalResponses() {
        return totalResponses;
    }

    protected void setTotalResponses(long totalResponses) {
        this.totalResponses = totalResponses;
    }

    public long getAccumulatedResponseTime() {
        return accumulatedResponseTime;
    }

    protected void setAccumulatedResponseTime(long accumulatedResponseTime) {
        this.accumulatedResponseTime = accumulatedResponseTime;
    }

    protected Map<Integer, StatusCodeResponseStore> getStatusCodeCounts() {
        return statusCodeCounts;
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) {
            return true;
        }

        if (o instanceof DestinationInfoStore) {
            DestinationInfoStore other = (DestinationInfoStore) o;

            return Objects.equal(this.destinationId, other.destinationId) &&
                    Objects.equal(this.accumulatedResponseTime, other.accumulatedResponseTime) &&
                    Objects.equal(this.startTime, other.startTime) &&
                    Objects.equal(this.totalRequests, other.totalRequests) &&
                    Objects.equal(this.totalResponses, other.totalResponses) &&
                    Objects.equal(this.statusCodeCounts, other.statusCodeCounts);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(destinationId, accumulatedResponseTime, startTime, totalRequests, totalResponses, statusCodeCounts);
    }
}

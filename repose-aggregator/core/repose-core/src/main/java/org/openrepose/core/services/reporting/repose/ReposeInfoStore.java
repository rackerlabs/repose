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
package org.openrepose.core.services.reporting.repose;

import org.openrepose.core.services.reporting.StatusCodeResponseStore;

import java.util.HashMap;
import java.util.Map;

public class ReposeInfoStore {

    private static final long LONG_ZERO = 0L;
    private Map<Integer, StatusCodeResponseStore> statusCodeCounts = new HashMap<>();
    private long totalRequests = LONG_ZERO;
    private long totalResponses = LONG_ZERO;
    private long accumulatedRequestSize = LONG_ZERO;
    private long accumulatedResponseSize = LONG_ZERO;
    private long minRequestSize = LONG_ZERO;
    private long maxRequestSize = LONG_ZERO;
    private long minResponseSize = LONG_ZERO;
    private long maxResponseSize = LONG_ZERO;

    public ReposeInfoStore() {
    }

    protected ReposeInfoStore(ReposeInfoStore reposeInfoStore) {
        this.statusCodeCounts = deepCopyStatusCodeCounts(reposeInfoStore.statusCodeCounts);
    }

    private Map<Integer, StatusCodeResponseStore> deepCopyStatusCodeCounts(Map<Integer, StatusCodeResponseStore> statusCodeCounts) {
        Map<Integer, StatusCodeResponseStore> copy = new HashMap<>();
        for (Map.Entry<Integer, StatusCodeResponseStore> entry : statusCodeCounts.entrySet()) {
            copy.put(entry.getKey(), new StatusCodeResponseStore(entry.getValue()));
        }

        return copy;
    }

    public Map<Integer, StatusCodeResponseStore> getStatusCodeCounts() {
        return statusCodeCounts;
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

    public long getAccumulatedRequestSize() {
        return accumulatedRequestSize;
    }

    public long getAccumulatedResponseSize() {
        return accumulatedResponseSize;
    }

    public long getMinRequestSize() {
        return minRequestSize;
    }

    public long getMaxRequestSize() {
        return maxRequestSize;
    }

    public long getMinResponseSize() {
        return minResponseSize;
    }

    public long getMaxResponseSize() {
        return maxResponseSize;
    }

    protected void processRequestSize(long requestSize) {
        this.accumulatedRequestSize += requestSize;
        if (requestSize < minRequestSize || minRequestSize == 0) {
            minRequestSize = requestSize;
        }
        if (requestSize > maxRequestSize) {
            maxRequestSize = requestSize;
        }
    }

    protected void processResponseSize(long responseSize) {
        this.accumulatedResponseSize += responseSize;
        if (responseSize < minResponseSize || minResponseSize == 0) {
            minResponseSize = responseSize;
        }
        if (responseSize > maxResponseSize) {
            maxResponseSize = responseSize;
        }
    }

}

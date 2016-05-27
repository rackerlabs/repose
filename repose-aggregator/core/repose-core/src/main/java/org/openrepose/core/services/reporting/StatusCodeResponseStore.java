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
package org.openrepose.core.services.reporting;

public class StatusCodeResponseStore {

    private static final long LONG_ZERO = 0l;
    private static final int HASH = 67;
    private static final int SHIFT = 32;
    private long totalCount = LONG_ZERO;
    private long accumulatedResponseTime = LONG_ZERO;

    public StatusCodeResponseStore(StatusCodeResponseStore store) {
        this.totalCount = store.totalCount;
        this.accumulatedResponseTime = store.accumulatedResponseTime;
    }

    public StatusCodeResponseStore(long totalCount, long accumulatedResponseTime) {
        this.totalCount = totalCount;
        this.accumulatedResponseTime = accumulatedResponseTime;
    }

    public StatusCodeResponseStore update(long count, long time) {
        this.totalCount += count;
        this.accumulatedResponseTime += time;
        return this;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public Long getAccumulatedResponseTime() {
        return accumulatedResponseTime;
    }

    @Override
    public int hashCode() {
        int hash = HASH;
        hash = HASH * hash + (int) (this.totalCount ^ (this.totalCount >>> SHIFT));
        hash = HASH * hash + (int) (this.accumulatedResponseTime ^ (this.accumulatedResponseTime >>> SHIFT));
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StatusCodeResponseStore)) {
            return false;
        }

        StatusCodeResponseStore other = (StatusCodeResponseStore) o;

        return other.accumulatedResponseTime == accumulatedResponseTime && other.totalCount == totalCount;
    }
}

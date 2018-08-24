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
package org.openrepose.powerfilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;

public class RequestTracer {

    private final Logger FILTER_TIMING_LOG = LoggerFactory.getLogger("filter-timing");

    private final boolean trace;
    private final boolean addHeader;
    private long startTime;

    public RequestTracer(boolean trace, boolean addHeader) {
        this.trace = trace;
        this.addHeader = addHeader;
    }

    public long traceEnter() {
        if (!trace) {
            return 0;
        }

        startTime = System.currentTimeMillis();
        return startTime;
    }

    public long traceExit(HttpServletResponse response, String filterName) {
        if (!trace) {
            return 0;
        }

        long totalRequestTime = System.currentTimeMillis() - startTime;

        if (addHeader) {
            FILTER_TIMING_LOG.trace("Filter {} spent {}ms processing", filterName, totalRequestTime);
            response.addHeader("X-" + filterName + "-Time", totalRequestTime + "ms");
        }

        return totalRequestTime;
    }
}

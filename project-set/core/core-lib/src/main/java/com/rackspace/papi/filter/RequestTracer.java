package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;

import java.util.Date;

public class RequestTracer {

    private final boolean trace;
    private final boolean addHeader;
    private long accumulatedTime;
    private long requestStart;

    public RequestTracer(boolean trace, boolean addHeader) {
        this.trace = trace;
        this.addHeader = addHeader;
        requestStart = new Date().getTime();
    }

    public long traceEnter() {
        if (!trace) {
            return 0;
        }

        return new Date().getTime() - requestStart;
    }

    public long traceExit(MutableHttpServletResponse response, String filterName, long myStart) {
        if (!trace) {
            return 0;
        }

        long totalRequestTime = new Date().getTime() - requestStart;
        long myTime = totalRequestTime - myStart - accumulatedTime;
        accumulatedTime += myTime;

        if (addHeader)
            response.addHeader("X-" + filterName + "-Time", myTime + "ms");

        return myTime;
    }
}

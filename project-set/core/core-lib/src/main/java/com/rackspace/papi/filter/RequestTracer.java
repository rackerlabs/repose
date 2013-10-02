package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import java.util.Date;

public class RequestTracer {

    private final boolean trace;
    private long accumulatedTime;
    private long requestStart;

    public RequestTracer(boolean trace) {
        this.trace = trace;
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
            return Long.MIN_VALUE;
        }
        long totalRequestTime = new Date().getTime() - requestStart;
        long myTime = totalRequestTime - myStart - accumulatedTime;
        accumulatedTime += myTime;
        response.addHeader("X-" + filterName + "-Time", myTime + "ms");

        return myTime;
    }
}

package org.openrepose.powerfilter;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;

public class RequestTracer {

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

    public long traceExit(MutableHttpServletResponse response, String filterName) {
        if (!trace) {
            return 0;
        }

        long totalRequestTime = System.currentTimeMillis() - startTime;

        if (addHeader) {
            response.addHeader("X-" + filterName + "-Time", totalRequestTime + "ms");
        }

        return totalRequestTime;
    }
}

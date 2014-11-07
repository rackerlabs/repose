package org.openrepose.powerfilter.intrafilterLogging;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.powerfilter.filtercontext.FilterContext;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;

public class RequestLog {

    String preamble;
    String timestamp;
    String currentFilter;
    String httpMethod;
    String requestURI;
    String requestBody;
    HashMap<String, String> headers;

    public RequestLog(MutableHttpServletRequest mutableHttpServletRequest,
                      FilterContext filterContext) throws IOException {

        preamble = "Intrafilter Request Log";
        timestamp = new DateTime().toString();
        currentFilter = filterContext.getFilterConfig().getName();
        httpMethod = mutableHttpServletRequest.getMethod();
        requestURI = mutableHttpServletRequest.getRequestURI();
        headers = convertRequestHeadersToMap(mutableHttpServletRequest);

        mutableHttpServletRequest.getInputStream().mark(Integer.MAX_VALUE);
        requestBody = IOUtils.toString(mutableHttpServletRequest.getInputStream()); //http://stackoverflow.com/a/309448
        mutableHttpServletRequest.getInputStream().reset();
    }

    private HashMap<String, String> convertRequestHeadersToMap(
            MutableHttpServletRequest mutableHttpServletRequest) {

        HashMap<String, String> headerMap = new LinkedHashMap<String, String>();
        List<String> headerNames = Collections.list(mutableHttpServletRequest.getHeaderNames());

        for (String headername : headerNames) {
            headerMap.put(headername, mutableHttpServletRequest.getHeader(headername));
        }

        return headerMap;
    }
}

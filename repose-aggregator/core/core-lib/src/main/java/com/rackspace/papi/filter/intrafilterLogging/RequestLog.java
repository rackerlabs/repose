package com.rackspace.papi.filter.intrafilterLogging;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.filter.FilterContext;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class RequestLog {

    String preamble;
    String timestamp;
    String currentFilter;
    String httpMethod;
    String requestURI;
    String requestBody;
    LinkedHashMap<String, String> headers;

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

    private LinkedHashMap<String, String> convertRequestHeadersToMap(
            MutableHttpServletRequest mutableHttpServletRequest) {

        LinkedHashMap<String, String> headerMap = new LinkedHashMap<String, String>();
        List<String> headerNames = Collections.list(mutableHttpServletRequest.getHeaderNames());

        for (String headername : headerNames) {
            headerMap.put(headername, mutableHttpServletRequest.getHeader(headername));
        }

        return headerMap;
    }
}

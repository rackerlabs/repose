package com.rackspace.papi.filter.intrafilterLogging;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.filter.FilterContext;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

public class ResponseLog {

    String preamble;
    String timestamp;
    String currentFilter;
    String httpResponseCode;
    String responseBody;
    LinkedHashMap<String, String> headers;

    public ResponseLog(MutableHttpServletResponse mutableHttpServletResponse,
                       FilterContext filterContext) throws IOException {

        preamble = "intrafilter Response Log";
        timestamp = new DateTime().toString();
        currentFilter = filterContext.getFilterConfig().getName();
        httpResponseCode = Integer.toString(mutableHttpServletResponse.getStatus());
        headers = convertResponseHeadersToMap(mutableHttpServletResponse);

        mutableHttpServletResponse.getBufferedOutputAsInputStream().mark(Integer.MAX_VALUE);
        responseBody = IOUtils.toString(mutableHttpServletResponse.getBufferedOutputAsInputStream());
        mutableHttpServletResponse.getBufferedOutputAsInputStream().reset();
    }

    private LinkedHashMap<String, String> convertResponseHeadersToMap(
            MutableHttpServletResponse mutableHttpServletResponse) {

        LinkedHashMap<String, String> headerMap = new LinkedHashMap<String, String>();
        List<String> headerNames = (List<String>) mutableHttpServletResponse.getHeaderNames();

        for (String headername : headerNames) {
            headerMap.put(headername, mutableHttpServletResponse.getHeader(headername));
        }

        return headerMap;
    }
}

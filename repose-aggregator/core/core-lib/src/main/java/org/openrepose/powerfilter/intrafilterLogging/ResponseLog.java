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
package org.openrepose.powerfilter.intrafilterLogging;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.core.systemmodel.Filter;
import org.openrepose.powerfilter.filtercontext.FilterContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * POJO that is used to log details about a response when the log level is set to TRACE.
 * See {@link org.openrepose.powerfilter.PowerFilterChain#intrafilterResponseLog} for more details.
 */
public class ResponseLog {

    String preamble;
    String timestamp;
    String currentFilter;
    String httpResponseCode;
    String responseBody;
    HashMap<String, String> headers;

    /**
     * Constructor populates all of the fields necessary for logging.
     * @param mutableHttpServletResponse {@link MutableHttpServletResponse}
     * @param filterContext {@link FilterContext}
     * @throws IOException if there's an issue converting the response body to a string
     */
    public ResponseLog(MutableHttpServletResponse mutableHttpServletResponse,
                       FilterContext filterContext) throws IOException {

        preamble = "Intrafilter Response Log";
        timestamp = new DateTime().toString();
        currentFilter = getFilterDescription(filterContext.getFilterConfig());
        httpResponseCode = Integer.toString(mutableHttpServletResponse.getStatus());
        headers = convertResponseHeadersToMap(mutableHttpServletResponse);

        responseBody = IOUtils.toString(mutableHttpServletResponse.getBufferedOutputAsInputStream()); //http://stackoverflow.com/a/309448
        mutableHttpServletResponse.setInputStream(new ByteArrayInputStream(responseBody.getBytes()));
    }

    /**
     * Convert the headers in the response into a HashMap.
     * @param mutableHttpServletResponse {@link MutableHttpServletResponse}
     * @return {@link HashMap}<{@link String}, {@link String}>
     */
    private HashMap<String, String> convertResponseHeadersToMap(
            MutableHttpServletResponse mutableHttpServletResponse) {

        HashMap<String, String> headerMap = new LinkedHashMap<>();
        List<String> headerNames = (List<String>) mutableHttpServletResponse.getHeaderNames();

        for (String headerName : headerNames) {
            headerMap.put(headerName, mutableHttpServletResponse.getHeader(headerName));
        }

        return headerMap;
    }

    /**
     * Creates a filter description using the filter name and (if specified) the filter ID.
     * The filter ID provides context in the event there is more than one filter with the same name.
     * @param filter {@link Filter}
     * @return {@link String} the filter description
     */
    private String getFilterDescription(final Filter filter) {
        if (StringUtils.isEmpty(filter.getId())) {
            return filter.getName();
        } else {
            return filter.getId() + "-" + filter.getName();
        }
    }
}

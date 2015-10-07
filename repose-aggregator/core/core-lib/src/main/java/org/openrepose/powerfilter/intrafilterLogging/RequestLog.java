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
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.core.systemmodel.Filter;
import org.openrepose.powerfilter.filtercontext.FilterContext;

import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.util.*;

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

        Filter filter = filterContext.getFilterConfig();

        preamble = "Intrafilter Request Log";
        timestamp = new DateTime().toString();
        currentFilter = StringUtils.isEmpty(filter.getId()) ? filter.getName() : filter.getId() + "-" + filter.getName();
        httpMethod = mutableHttpServletRequest.getMethod();
        requestURI = mutableHttpServletRequest.getRequestURI();
        headers = convertRequestHeadersToMap(mutableHttpServletRequest);

        //Have to wrap the input stream in something that can be buffered, as well as reset.
        ServletInputStream bin = mutableHttpServletRequest.getInputStream();
        bin.mark(Integer.MAX_VALUE); //Something doesn't support mark reset
        requestBody = IOUtils.toString(bin); //http://stackoverflow.com/a/309448
        bin.reset();
    }

    private HashMap<String, String> convertRequestHeadersToMap(
            MutableHttpServletRequest mutableHttpServletRequest) {

        HashMap<String, String> headerMap = new LinkedHashMap<>();
        List<String> headerNames = Collections.list(mutableHttpServletRequest.getHeaderNames());

        for (String headerName : headerNames) {
            StringBuilder allHeaderValues = new StringBuilder();
            for (String value : Collections.list(mutableHttpServletRequest.getHeaders(headerName))) {
                allHeaderValues.append(value).append(",");
            }
            //Clobber the last character
            allHeaderValues.deleteCharAt(allHeaderValues.length() - 1);
            headerMap.put(headerName, allHeaderValues.toString());
        }

        return headerMap;
    }
}

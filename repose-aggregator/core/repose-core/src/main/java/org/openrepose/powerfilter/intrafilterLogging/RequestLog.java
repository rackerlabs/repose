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
import org.openrepose.core.systemmodel.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

public class RequestLog {
    private static final Logger LOG = LoggerFactory.getLogger(RequestLog.class);

    String preamble;
    String timestamp;
    String currentFilter;
    String httpMethod;
    String requestURI;
    String requestBody;
    Map<String, String> headers;

    public RequestLog(HttpServletRequest httpServletRequest, Filter filter) throws IOException {
        preamble = "Intrafilter Request Log";
        timestamp = new DateTime().toString();
        currentFilter = StringUtils.isEmpty(filter.getId()) ? filter.getName() : filter.getId() + "-" + filter.getName();
        httpMethod = httpServletRequest.getMethod();
        requestURI = httpServletRequest.getRequestURI();
        headers = convertRequestHeadersToMap(httpServletRequest);

        try {
            ServletInputStream inputStream = httpServletRequest.getInputStream();
            if (inputStream.markSupported()) {
                inputStream.mark(Integer.MAX_VALUE);
                requestBody = IOUtils.toString(inputStream); //http://stackoverflow.com/a/309448
                inputStream.reset();
            } else {
                LOG.warn("Unable to populate request body - {} does not support mark/reset.", inputStream);
            }
        } catch (IOException e) {
            LOG.warn("Unable to populate request body.", e);
        }
    }

    private Map<String, String> convertRequestHeadersToMap(HttpServletRequest httpServletRequest) {
        Map<String, String> headerMap = new LinkedHashMap<>();
        List<String> headerNames = Collections.list(httpServletRequest.getHeaderNames());

        for (String headerName : headerNames) {
            StringJoiner stringJoiner = new StringJoiner(",");
            Collections.list(httpServletRequest.getHeaders(headerName)).forEach(stringJoiner::add);
            headerMap.put(headerName, stringJoiner.toString());
        }

        return headerMap;
    }
}

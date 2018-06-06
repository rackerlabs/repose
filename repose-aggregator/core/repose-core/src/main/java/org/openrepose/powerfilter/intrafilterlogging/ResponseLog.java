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
package org.openrepose.powerfilter.intrafilterlogging;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.util.*;

public class ResponseLog {

    String preamble;
    String timestamp;
    String currentFilter;
    String httpResponseCode;
    String responseBody;
    Map<String, String> headers;

    public ResponseLog(HttpServletResponseWrapper wrappedServletResponse, String currentFilter) throws IOException {
        preamble = "Intrafilter Response Log";
        timestamp = new DateTime().toString();
        this.currentFilter = currentFilter;
        httpResponseCode = Integer.toString(wrappedServletResponse.getStatus());
        headers = convertResponseHeadersToMap(wrappedServletResponse);

        responseBody = IOUtils.toString(wrappedServletResponse.getOutputStreamAsInputStream()); //http://stackoverflow.com/a/309448
    }

    private Map<String, String> convertResponseHeadersToMap(HttpServletResponseWrapper wrappedServletResponse) {
        HashMap<String, String> headerMap = new LinkedHashMap<>();
        Collection<String> headerNames = wrappedServletResponse.getHeaderNames();

        for (String headerName : headerNames) {
            StringJoiner stringJoiner = new StringJoiner(",");
            wrappedServletResponse.getHeaders(headerName).forEach(stringJoiner::add);
            headerMap.put(headerName, stringJoiner.toString());
        }

        return headerMap;
    }
}

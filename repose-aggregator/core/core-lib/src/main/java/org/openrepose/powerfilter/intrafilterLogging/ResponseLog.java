/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
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
 * #L%
 */
package org.openrepose.powerfilter.intrafilterLogging;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.powerfilter.filtercontext.FilterContext;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;

public class ResponseLog {

    String preamble;
    String timestamp;
    String currentFilter;
    String httpResponseCode;
    String responseBody;
    HashMap<String, String> headers;

    public ResponseLog(MutableHttpServletResponse mutableHttpServletResponse,
                       FilterContext filterContext) throws IOException {

        preamble = "Intrafilter Response Log";
        timestamp = new DateTime().toString();
        currentFilter = filterContext.getFilterConfig().getId() + "-" + filterContext.getFilterConfig().getName();
        httpResponseCode = Integer.toString(mutableHttpServletResponse.getStatus());
        headers = convertResponseHeadersToMap(mutableHttpServletResponse);

        responseBody = IOUtils.toString(mutableHttpServletResponse.getBufferedOutputAsInputStream()); //http://stackoverflow.com/a/309448
        mutableHttpServletResponse.setInputStream(new ByteArrayInputStream(responseBody.getBytes()));
    }

    private HashMap<String, String> convertResponseHeadersToMap(
            MutableHttpServletResponse mutableHttpServletResponse) {

        HashMap<String, String> headerMap = new LinkedHashMap<String, String>();
        List<String> headerNames = (List<String>) mutableHttpServletResponse.getHeaderNames();

        for (String headername : headerNames) {
            headerMap.put(headername, mutableHttpServletResponse.getHeader(headername));
        }

        return headerMap;
    }
}

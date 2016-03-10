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
package org.openrepose.filters.headeridentity.extractor;

import org.openrepose.commons.utils.regex.ExtractorResult;
import org.openrepose.filters.headeridentity.config.HttpHeader;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

public class HeaderValueExtractor {

    private HttpServletRequest request;

    public HeaderValueExtractor(HttpServletRequest request) {
        this.request = request;
    }

    protected String extractHeader(String name) {
        // Header value may be a comma separated list of values.
        // The left most value in the list will be extracted.
        String header = request.getHeader(name);
        return header != null ? header.split(",")[0].trim() : "";
    }

    public List<ExtractorResult<String>> extractUserGroup(List<HttpHeader> headerNames) {
        List<ExtractorResult<String>> results = new ArrayList<ExtractorResult<String>>();
        String user = "";
        String group = "";

        for (HttpHeader header : headerNames) {
            user = extractHeader(header.getId());
            if (!user.isEmpty()) {
                String quality = determineQuality(header);
                user += quality;
                group = header.getId() + quality;
                results.add(new ExtractorResult<String>(user, group));
            }
        }

        return results;
    }

    private String determineQuality(HttpHeader header) {
        return ";q=" + header.getQuality();
    }
}

/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.filters.cnorm.normalizer;

import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.filters.cnorm.config.HeaderFilterList;
import org.openrepose.filters.cnorm.config.HttpHeader;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.List;

/**
 *
 * @author jhopper
 */
public class HeaderNormalizer {

    private final List<HttpHeader> headers;
    private final boolean isBlacklist;

    public HeaderNormalizer(HeaderFilterList headerFilterList, boolean isBlacklist) {
        this.headers = isBlacklist ? headerFilterList.getBlacklist().getHeader() : headerFilterList.getWhitelist().getHeader();
        this.isBlacklist = isBlacklist;
    }

    public void normalizeHeaders(HttpServletRequest request, FilterDirector currentDirector) {
        final Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            final String requestHeaderName = headerNames.nextElement();

            boolean found = false;

            for (HttpHeader configuredHeader : headers) {
                if (configuredHeader.getId().equalsIgnoreCase(requestHeaderName)) {
                    found = true;
                    break;
                }
            }

            if (found && isBlacklist || !found && !isBlacklist) {
                currentDirector.requestHeaderManager().removeHeader(requestHeaderName);
            }
        }
    }
}

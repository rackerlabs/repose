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
package org.openrepose.filters.ratelimiting;

import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.core.services.ratelimit.RateLimitingService;
import org.openrepose.core.services.ratelimit.config.RateLimitList;
import org.openrepose.core.services.ratelimit.exception.OverLimitException;
import org.openrepose.filters.ratelimiting.write.ActiveLimitsWriter;
import org.openrepose.filters.ratelimiting.write.CombinedLimitsWriter;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

public class RateLimitingServiceHelper {

    private final RateLimitingService service;
    private final ActiveLimitsWriter activeLimitsWriter;
    private final CombinedLimitsWriter combinedLimitsWriter;

    public RateLimitingServiceHelper(RateLimitingService service, ActiveLimitsWriter activeLimitsWriter, CombinedLimitsWriter combinedLimitsWriter) {
        this.service = service;
        this.activeLimitsWriter = activeLimitsWriter;
        this.combinedLimitsWriter = combinedLimitsWriter;
    }

    public MediaType queryActiveLimits(HttpServletRequest request, MediaType preferredMediaType, OutputStream outputStream) {
        RateLimitList rateLimits = service.queryLimits(getPreferredUser(request), getPreferredGroups(request));

        return activeLimitsWriter.write(rateLimits, preferredMediaType, outputStream);
    }

    public MediaType queryCombinedLimits(HttpServletRequest request, MediaType preferredMediaType, InputStream absoluteLimits, OutputStream outputStream) {
        RateLimitList rateLimits = service.queryLimits(getPreferredUser(request), getPreferredGroups(request));

        return combinedLimitsWriter.write(rateLimits, preferredMediaType, absoluteLimits, outputStream);
    }

    public void trackLimits(HttpServletRequest request, int datastoreWarnLimit) throws OverLimitException {
        service.trackLimits(getPreferredUser(request), getPreferredGroups(request), decodeURI(request.getRequestURI()), HttpServletRequestWrapper.parseQueryString(request.getQueryString()), request.getMethod(), datastoreWarnLimit);
    }

    public String getPreferredUser(HttpServletRequest request) {
        final HttpServletRequestWrapper mutableRequest = new HttpServletRequestWrapper(request);
        final List<String> preferredUsers = mutableRequest.getPreferredSplittableHeaders(PowerApiHeader.USER);

        String preferredUser = null;
        if (!preferredUsers.isEmpty()) {
            preferredUser = preferredUsers.get(0);
        }

        return preferredUser;
    }

    public List<String> getPreferredGroups(HttpServletRequest request) {
        final HttpServletRequestWrapper mutableRequest = new HttpServletRequestWrapper(request);

        return mutableRequest.getPreferredSplittableHeaders(PowerApiHeader.GROUPS);
    }

    private String decodeURI(String uri) {
        return URI.create(uri).getPath();
    }
}

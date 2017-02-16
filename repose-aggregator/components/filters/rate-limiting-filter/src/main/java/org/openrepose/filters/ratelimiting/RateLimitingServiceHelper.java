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

import com.sun.jersey.server.impl.provider.RuntimeDelegateImpl;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.core.services.ratelimit.RateLimitingService;
import org.openrepose.core.services.ratelimit.config.RateLimitList;
import org.openrepose.core.services.ratelimit.exception.OverLimitException;
import org.openrepose.filters.ratelimiting.write.ActiveLimitsWriter;
import org.openrepose.filters.ratelimiting.write.CombinedLimitsWriter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ext.RuntimeDelegate;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

public class RateLimitingServiceHelper {

    private static final RuntimeDelegateImpl runtimeDelegateImpl = new RuntimeDelegateImpl();
    private final RateLimitingService service;
    private final ActiveLimitsWriter activeLimitsWriter;
    private final CombinedLimitsWriter combinedLimitsWriter;

    public RateLimitingServiceHelper(RateLimitingService service, ActiveLimitsWriter activeLimitsWriter, CombinedLimitsWriter combinedLimitsWriter) {
        this.service = service;
        this.activeLimitsWriter = activeLimitsWriter;
        this.combinedLimitsWriter = combinedLimitsWriter;
        // This fixes the ClassNotFoundException: org.glassfish.jersey.internal.RuntimeDelegateImpl
        // and requires the Maven dependency: com.sun.jersey:jersey-server:jar:1.16:compile
        // http://www.programcreek.com/java-api-examples/index.php?api=javax.ws.rs.ext.RuntimeDelegate
        RuntimeDelegate.setInstance(runtimeDelegateImpl);
    }

    public MimeType queryActiveLimits(HttpServletRequest request, MimeType preferredMediaType, OutputStream outputStream) {
        RateLimitList rateLimits = service.queryLimits(getPreferredUser(request), getPreferredGroups(request));
        javax.ws.rs.core.MediaType mediaType = activeLimitsWriter.write(rateLimits, getJavaMediaType(preferredMediaType), outputStream);

        return getReposeMimeType(mediaType);
    }

    public MimeType queryCombinedLimits(HttpServletRequest request, MimeType preferredMediaType, InputStream absoluteLimits, OutputStream outputStream) {
        RateLimitList rateLimits = service.queryLimits(getPreferredUser(request), getPreferredGroups(request));
        javax.ws.rs.core.MediaType mediaType = combinedLimitsWriter.write(rateLimits, getJavaMediaType(preferredMediaType), absoluteLimits, outputStream);

        return getReposeMimeType(mediaType);
    }

    public void trackLimits(HttpServletRequest request, int datastoreWarnLimit) throws OverLimitException {
        service.trackLimits(getPreferredUser(request), getPreferredGroups(request), decodeURI(request.getRequestURI()), request.getParameterMap(), request.getMethod(), datastoreWarnLimit);
    }

    public MimeType getReposeMimeType(javax.ws.rs.core.MediaType mediaType) {
        return MimeType.guessMediaTypeFromString(mediaType.toString());
    }

    public javax.ws.rs.core.MediaType getJavaMediaType(MimeType reposeMimeType) {
        return new javax.ws.rs.core.MediaType(reposeMimeType.getTopLevelTypeName(), reposeMimeType.getSubTypeName());
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

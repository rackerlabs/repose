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

import com.google.common.base.Optional;

import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.http.HttpDate;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.media.MediaRangeProcessor;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.commons.utils.servlet.filter.FilterAction;
import org.openrepose.commons.utils.servlet.http.*;
import org.openrepose.core.services.datastore.DatastoreOperationException;
import org.openrepose.core.services.event.common.EventService;
import org.openrepose.core.services.ratelimit.OverLimitData;
import org.openrepose.core.services.ratelimit.RateLimitFilterEvent;
import org.openrepose.core.services.ratelimit.RateLimitingServiceImpl;
import org.openrepose.core.services.ratelimit.exception.CacheException;
import org.openrepose.core.services.ratelimit.exception.OverLimitException;
import org.openrepose.filters.ratelimiting.log.LimitLogger;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;


/* Responsible for handling requests and responses to rate limiting, also tracks and provides limits */
public class RateLimitingHandler {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingHandler.class);
    private static final MimeType DEFAULT_MIME_TYPE = MimeType.APPLICATION_JSON;
    private static final int SC_TOO_MANY_REQUESTS = 429;

    private final boolean includeAbsoluteLimits;
    private final Optional<Pattern> describeLimitsUriPattern;
    private final RateLimitingServiceHelper rateLimitingServiceHelper;
    private MimeType originalPreferredAccept;
    private boolean overLimit429ResponseCode;
    private int datastoreWarnLimit;
    private final EventService eventService;

    public RateLimitingHandler(RateLimitingServiceHelper rateLimitingServiceHelper, EventService eventService, boolean includeAbsoluteLimits, Optional<Pattern> describeLimitsUriPattern, boolean overLimit429ResponseCode, int datastoreWarnLimit) {
        this.includeAbsoluteLimits = includeAbsoluteLimits;
        this.describeLimitsUriPattern = describeLimitsUriPattern;
        this.rateLimitingServiceHelper = rateLimitingServiceHelper;
        this.overLimit429ResponseCode = overLimit429ResponseCode;
        this.datastoreWarnLimit = datastoreWarnLimit;
        this.eventService = eventService;
    }

    public FilterAction handleRequest(HttpServletRequestWrapper request, HttpServletResponse response) {
        FilterAction filterAction;

        List<String> headerValues = request.getPreferredSplittableHeaders(CommonHttpHeader.ACCEPT.toString());
        List<MimeType> mimeTypes = MediaRangeProcessor.getMimeTypesFromHeaderValues(headerValues);
        if (mimeTypes.isEmpty()) {
            mimeTypes.add(DEFAULT_MIME_TYPE);
        }

        if (requestHasExpectedHeaders(request)) {
            originalPreferredAccept = getPreferredMimeType(mimeTypes);

            // record limits
            if (!recordLimitedRequest(request, response)) {
                // failure - either over the limit or some other exception related to the data store occurred
                filterAction = FilterAction.RETURN;
            } else if (describeLimitsUriPattern.isPresent() && describeLimitsUriPattern.get().matcher(request.getRequestURI()).matches()) {
                // request matches the configured getCurrentLimits API call endpoint
                filterAction = describeLimitsForRequest(request, response);
            } else {
                filterAction = FilterAction.PASS;
            }
        } else {
            LOG.warn("Expected header: {} was not supplied in the request. Rate limiting requires this header to operate.", PowerApiHeader.USER.toString());

            // Auto return a 401 if the request does not meet expectations
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            filterAction = FilterAction.RETURN;
        }

        return filterAction;
    }

    private boolean requestHasExpectedHeaders(HttpServletRequest request) {
        return request.getHeader(PowerApiHeader.USER.toString()) != null;
    }

    private FilterAction describeLimitsForRequest(HttpServletRequestWrapper request, HttpServletResponse response) {
        if (originalPreferredAccept == MimeType.UNKNOWN) {
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            return FilterAction.RETURN;
        } else {
            // If include absolute limits let request pass thru but prepare the combined
            // (absolute and active) limits when processing the response
            // TODO: A way to query global rate limits
            if (includeAbsoluteLimits) {
                request.replaceHeader(CommonHttpHeader.ACCEPT.toString(), MimeType.APPLICATION_XML.toString());
                return FilterAction.PROCESS_RESPONSE;
            } else {
                return noUpstreamResponse(request, response);
            }
        }
    }

    private FilterAction noUpstreamResponse(HttpServletRequestWrapper request, HttpServletResponse response) {
        try {
            HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper(response, ResponseMode.MUTABLE, ResponseMode.MUTABLE);
            final MimeType mimeType = rateLimitingServiceHelper.queryActiveLimits(request, originalPreferredAccept, wrappedResponse.getOutputStream());

            wrappedResponse.replaceHeader(CommonHttpHeader.CONTENT_TYPE.toString(), mimeType.toString());
            wrappedResponse.setStatus(HttpServletResponse.SC_OK);
            wrappedResponse.commitToResponse();
        } catch (Exception e) {
            LOG.error("Failure when querying limits. Reason: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return FilterAction.RETURN;
    }

    private boolean recordLimitedRequest(HttpServletRequest request, HttpServletResponse response) {
        boolean success = false;

        try {
            rateLimitingServiceHelper.trackLimits(request, datastoreWarnLimit);
            success = true;
        } catch (OverLimitException e) {
            LOG.trace("Over Limit", e);
            new LimitLogger(e.getUser(), request).log(e.getConfiguredLimit(), Integer.toString(e.getCurrentLimitAmount()));
            final HttpDate nextAvailableTime = new HttpDate(e.getNextAvailableTime());

            // We use a 413 "Request Entity Too Large" to communicate that the user
            // in question has hit their rate limit for this requested URI
            if (e.getUser().equals(RateLimitingServiceImpl.GLOBAL_LIMIT_USER)) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            } else if (overLimit429ResponseCode) {
                response.setStatus(SC_TOO_MANY_REQUESTS);
            } else {
                response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            }

            response.addHeader(CommonHttpHeader.RETRY_AFTER.toString(), nextAvailableTime.toRFC1123());
            eventService.newEvent(RateLimitFilterEvent.OVER_LIMIT, new OverLimitData(e, datastoreWarnLimit, request, response.getStatus()));
        } catch (CacheException e) {
            LOG.error("Failure when tracking limits.", e);
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        } catch (DatastoreOperationException doe) {
            LOG.error("Unable to communicate with dist-datastore.", doe);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }

        return success;
    }

    public void handleResponse(HttpServletRequestWrapper request, HttpServletResponse response) {
        try {
            if (response.getContentType() != null) {
                //If we have a content type to process, then we should do something about it,
                // else we should ensure that just the repose limits make it through...

                HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper(response, ResponseMode.MUTABLE, ResponseMode.MUTABLE);

                // I have to use mutable state, and that makes me sad, because If's aren't expressions
                InputStream absoluteInputStream;
                if (wrappedResponse.getContentType().equalsIgnoreCase(MimeType.APPLICATION_JSON.toString())) {
                    //New set up! Grab the upstream json, make it look like XML
                    String newXml = UpstreamJsonToXml.convert(wrappedResponse.getOutputStreamAsInputStream());

                    //Now we use the new XML we converted from the JSON as the input to the processing stream
                    absoluteInputStream = new ByteArrayInputStream(newXml.getBytes(StandardCharsets.UTF_8));
                } else if (wrappedResponse.getContentType().equalsIgnoreCase(MimeType.APPLICATION_XML.toString())) {
                    //If we got XML from upstream, just read the stream directly
                    absoluteInputStream = wrappedResponse.getOutputStreamAsInputStream();
                } else {
                    LOG.error("Upstream limits responded with a content type we cannot understand: {}", response.getContentType());
                    //Upstream responded with something we cannot talk, we failed to combine upstream limits, return a 502!
                    throw new UpstreamException("Upstream limits responded with a content type we cannot understand: " + response.getContentType());
                }

                //We'll get here if we were able to properly parse JSON, or if we had XML from upstream!
                final MimeType mimeType = rateLimitingServiceHelper.queryCombinedLimits(request, originalPreferredAccept, absoluteInputStream, wrappedResponse.getOutputStream());
                wrappedResponse.replaceHeader(CommonHttpHeader.CONTENT_TYPE.toString(), mimeType.toString());
            } else {
                LOG.warn("NO DATA RECEIVED FROM UPSTREAM limits, only sending regular rate limits!");
                //No data from upstream, so we send the regular stuff no matter what
                noUpstreamResponse(request, response);
            }
        } catch (UpstreamException ue) {
            //I want a 502 returned when upstream didn't respond appropriately
            LOG.error("Failure when querying limits. Reason: " + ue.getMessage(), ue);
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        } catch (Exception e) {
            LOG.error("Failure when querying limits. Reason: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public MimeType getPreferredMimeType(List<MimeType> mimeTypes) {
        for (MimeType mimeType : mimeTypes) {
            if (mimeType == MimeType.APPLICATION_XML || mimeType == MimeType.APPLICATION_JSON) {
                return mimeType;
            }
        }

        return mimeTypes.get(0);
    }

    /**
     * So I can have a different catch since I don't have nice pattern matching
     */
    private class UpstreamException extends Exception {
        public UpstreamException(String message) {
            super(message);
        }
    }
}

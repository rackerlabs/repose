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

import org.apache.http.HttpHeaders;
import org.openrepose.commons.utils.http.HttpDate;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.normal.ExtendedStatusCodes;
import org.openrepose.commons.utils.servlet.filter.FilterAction;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.openrepose.core.services.datastore.DatastoreOperationException;
import org.openrepose.core.services.event.EventService;
import org.openrepose.core.services.ratelimit.OverLimitData;
import org.openrepose.core.services.ratelimit.RateLimitFilterEvent;
import org.openrepose.core.services.ratelimit.RateLimitingServiceImpl;
import org.openrepose.core.services.ratelimit.exception.CacheException;
import org.openrepose.core.services.ratelimit.exception.OverLimitException;
import org.openrepose.filters.ratelimiting.log.LimitLogger;
import org.slf4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.util.comparator.CompoundComparator;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.servlet.http.HttpServletResponse.*;
import static org.openrepose.filters.ratelimiting.write.LimitsResponseMimeTypeWriter.SUPPORTED_MEDIA_TYPES;

/* Responsible for handling requests and responses to rate limiting, also tracks and provides limits */
public class RateLimitingHandler {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingHandler.class);
    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.APPLICATION_JSON;

    private final boolean includeAbsoluteLimits;
    private final Optional<Pattern> describeLimitsUriPattern;
    private final RateLimitingServiceHelper rateLimitingServiceHelper;
    private MediaType originalPreferredAccept;
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

    public FilterAction handleRequest(HttpServletRequestWrapper request, HttpServletResponseWrapper response) {
        FilterAction filterAction;

        if (requestHasExpectedHeaders(request)) {
            // record limits
            if (!recordLimitedRequest(request, response)) {
                // failure - either over the limit or some other exception related to the data store occurred
                filterAction = FilterAction.RETURN;
            } else if (describeLimitsUriPattern.isPresent() && describeLimitsUriPattern.get().matcher(request.getRequestURI()).matches()) {
                // request matches the configured getCurrentLimits API call endpoint
                Optional<MediaType> preferredMediaType = getPreferredMediaType(request.getSplittableHeaders(HttpHeaders.ACCEPT));
                if (!preferredMediaType.isPresent()) {
                    // If no supported media types are acceptable, return a 406.
                    // This is in according to HTTP/1.0 specification, even though HTTP/1.1 specifies that either a 406
                    // response or a response with a default media type may be returned.
                    response.setStatus(SC_NOT_ACCEPTABLE);
                    // TODO: Set the supported accept types in the body? Provide a link to a supported types page?
                    filterAction = FilterAction.RETURN;
                } else {
                    originalPreferredAccept = preferredMediaType.get();
                    filterAction = describeLimitsForRequest(request, response);
                }
            } else {
                filterAction = FilterAction.PASS;
            }
        } else {
            LOG.warn("Expected header: {} was not supplied in the request. Rate limiting requires this header to operate.", PowerApiHeader.USER);

            // Auto return a 401 if the request does not meet expectations
            response.setStatus(SC_UNAUTHORIZED);
            filterAction = FilterAction.RETURN;
        }

        return filterAction;
    }

    private boolean requestHasExpectedHeaders(HttpServletRequest request) {
        return request.getHeader(PowerApiHeader.USER) != null;
    }

    private static Optional<MediaType> parseMediaType(String type) {
        Optional<MediaType> parsedType = Optional.empty();
        try {
            parsedType = Optional.of(MediaType.parseMediaType(type));
        } catch (IllegalArgumentException iae) {
            LOG.warn("Media type could not be parsed: {}", type, iae);
        }
        return parsedType;
    }

    private Optional<MediaType> getPreferredMediaType(List<String> acceptValues) {
        Optional<MediaType> preferredMediaType = Optional.of(DEFAULT_MEDIA_TYPE);
        if (!acceptValues.isEmpty()) {
            /*
            Parses media types from the "Accept" header (dropping any that cannot be parsed),
            removes any media types that are not supported by this filter (since they will not be returned anyway),
            then sorts the remaining media types by specificity (primarily) and quality (secondarily).
              */
            List<MediaType> sortedRelevantAcceptTypes = acceptValues.stream()
                    .map(RateLimitingHandler::parseMediaType)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(mediaType -> SUPPORTED_MEDIA_TYPES.stream().anyMatch(mediaType::isCompatibleWith))
                    .sorted(new CompoundComparator<>(MediaType.SPECIFICITY_COMPARATOR, MediaType.QUALITY_VALUE_COMPARATOR))
                    .collect(Collectors.toList());

            /*
            Determines which supported media types are acceptable to the user.
            A media type is deemed acceptable if it was present in the "Accept" header and has a non-zero quality.
            Since we have a sorted stream of media types from the request, we only check the quality of the first
            compatible media type. The reasoning is that the first compatible media type will be the most specific
            media type that matches a supported media type. The most specific media type should be considered above
            any other media types.
             */
            Stream<MediaType> acceptableMediaTypes = SUPPORTED_MEDIA_TYPES.stream()
                    .filter(mediaType -> sortedRelevantAcceptTypes.stream()
                            .filter(mediaType::isCompatibleWith)
                            .findFirst()
                            .map(MediaType::getQualityValue)
                            .map(quality -> quality != 0.0)
                            .orElse(false));

            /*
            Sets the first most specific, highest quality acceptable media type as the preferred media type.
             */
            preferredMediaType = sortedRelevantAcceptTypes.stream()
                    .filter(mediaType -> acceptableMediaTypes.anyMatch(mediaType::isCompatibleWith))
                    .findFirst();
        }
        return preferredMediaType;
    }

    private FilterAction describeLimitsForRequest(HttpServletRequestWrapper request, HttpServletResponseWrapper response) {
        // If include absolute limits let request pass through but prepare the combined
        // (absolute and active) limits when processing the response
        // TODO: A way to query global rate limits
        if (includeAbsoluteLimits) {
            request.replaceHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE);
            return FilterAction.PROCESS_RESPONSE;
        } else {
            return noUpstreamResponse(request, response);
        }
    }

    private FilterAction noUpstreamResponse(HttpServletRequestWrapper request, HttpServletResponseWrapper response) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final MediaType mediaType = rateLimitingServiceHelper.queryActiveLimits(request, originalPreferredAccept, outputStream);
            response.setOutput(new ByteArrayInputStream(outputStream.toByteArray()));
            response.setContentType(mediaType.toString());
            response.setStatus(SC_OK);
        } catch (Exception e) {
            LOG.error("Failure when querying limits. Reason: " + e.getMessage(), e);
            response.setStatus(SC_INTERNAL_SERVER_ERROR);
        }

        return FilterAction.RETURN;
    }

    private boolean recordLimitedRequest(HttpServletRequest request, HttpServletResponseWrapper response) {
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
                response.setStatus(SC_SERVICE_UNAVAILABLE);
            } else if (overLimit429ResponseCode) {
                response.setStatus(ExtendedStatusCodes.SC_TOO_MANY_REQUESTS);
            } else {
                response.setStatus(SC_REQUEST_ENTITY_TOO_LARGE);
            }

            response.addHeader(HttpHeaders.RETRY_AFTER, nextAvailableTime.toRFC1123());
            eventService.newEvent(RateLimitFilterEvent.OVER_LIMIT, new OverLimitData(e, datastoreWarnLimit, request, response.getStatus()));
        } catch (CacheException e) {
            LOG.error("Failure when tracking limits.", e);
            response.setStatus(SC_BAD_GATEWAY);
        } catch (DatastoreOperationException doe) {
            LOG.error("Unable to communicate with dist-datastore.", doe);
            response.setStatus(SC_SERVICE_UNAVAILABLE);
        }

        return success;
    }

    public void handleResponse(HttpServletRequestWrapper request, HttpServletResponseWrapper response) {
        try {
            if (response.getContentType() != null) {
                //If we have a content type to process, then we should do something about it,
                // else we should ensure that just the repose limits make it through...

                // I have to use mutable state, and that makes me sad, because If's aren't expressions
                InputStream absoluteInputStream;
                if (response.getContentType().equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
                    //New set up! Grab the upstream json, make it look like XML
                    String newXml = UpstreamJsonToXml.convert(response.getOutputStreamAsInputStream());

                    //Now we use the new XML we converted from the JSON as the input to the processing stream
                    absoluteInputStream = new ByteArrayInputStream(newXml.getBytes(StandardCharsets.UTF_8));
                } else if (response.getContentType().equalsIgnoreCase(MediaType.APPLICATION_XML_VALUE)) {
                    //If we got XML from upstream, just read the stream directly
                    absoluteInputStream = response.getOutputStreamAsInputStream();
                } else {
                    LOG.error("Upstream limits responded with a content type we cannot understand: {}", response.getContentType());
                    //Upstream responded with something we cannot talk, we failed to combine upstream limits, return a 502!
                    throw new UpstreamException("Upstream limits responded with a content type we cannot understand: " + response.getContentType());
                }

                //We'll get here if we were able to properly parse JSON, or if we had XML from upstream!
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final MediaType mediaType = rateLimitingServiceHelper.queryCombinedLimits(request, originalPreferredAccept, absoluteInputStream, outputStream);
                response.setOutput(new ByteArrayInputStream(outputStream.toByteArray()));
                response.setContentType(mediaType.toString());
            } else {
                LOG.warn("NO DATA RECEIVED FROM UPSTREAM limits, only sending regular rate limits!");
                //No data from upstream, so we send the regular stuff no matter what
                noUpstreamResponse(request, response);
            }
        } catch (UpstreamException ue) {
            //I want a 502 returned when upstream didn't respond appropriately
            LOG.error("Failure when querying limits. Reason: " + ue.getMessage(), ue);
            response.setStatus(SC_BAD_GATEWAY);
        } catch (Exception e) {
            LOG.error("Failure when querying limits. Reason: " + e.getMessage(), e);
            response.setStatus(SC_INTERNAL_SERVER_ERROR);
        }
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

/*
 *
 */
package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.QualityFactorUtility;
import com.rackspace.papi.commons.util.http.media.MediaRangeParser;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.ratelimit.cache.RateLimitCache;
import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;

/**
 *
 * @author Dan Daley
 */
public class RateLimitingHandler extends AbstractFilterLogicHandler {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingHandler.class);
    private final Map<String, Map<String, Pattern>> regexCache;
    private final RateLimitCache rateLimitCache;
    private final Pattern describeLimitsUriRegex;
    private final RateLimitingConfiguration rateLimitingConfig;
    private Enumeration<String> originalAcceptHeaders;
    private List<MediaType> mediaRanges;
    private MediaType acceptType;

    public RateLimitingHandler(Map<String, Map<String, Pattern>> regexCache, RateLimitCache rateLimitCache, Pattern describeLimitsUriRegex, RateLimitingConfiguration rateLimitingConfig) {
        this.regexCache = regexCache;
        this.rateLimitCache = rateLimitCache;
        this.describeLimitsUriRegex = describeLimitsUriRegex;
        this.rateLimitingConfig = rateLimitingConfig;
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector director = new FilterDirectorImpl();
        originalAcceptHeaders = request.getHeaders("Accept");
        if (originalAcceptHeaders != null) {
            mediaRanges = new MediaRangeParser(originalAcceptHeaders).parse();
            acceptType = QualityFactorUtility.choosePreferredHeaderValue(mediaRanges);
        }else{
            acceptType = new MediaType(MimeType.APPLICATION_JSON);  //we will default to a json response
        }

        // Does the request contain expected headers?
        if (requestHasExpectedHeaders(request)) {
            final String requestUri = request.getRequestURI();

            // We set the default action to PASS in this case since further
            // logic may or may not change the action and this request can now
            // be considered valid.
            director.setFilterAction(FilterAction.PASS);

            // Does the request match the configured getCurrentLimits API call endpoint?
            if (describeLimitsUriRegex.matcher(requestUri).matches()) {
                describeLimitsForRequest(director, request);
            } else {
                recordLimitedRequest(new RateLimitingRequestInfo(request, acceptType), director);
            }
        } else {
            LOG.warn("Expected header: " + PowerApiHeader.USER.toString()
                    + " was not supplied in the request. Rate limiting requires this header to operate.");

            // Auto return a 401 if the request does not meet expectations
            director.setResponseStatus(HttpStatusCode.UNAUTHORIZED);
            director.setFilterAction(FilterAction.RETURN);
        }

        return director;
    }

    public void recordLimitedRequest(RateLimitingRequestInfo info, FilterDirector director) {
        new RateLimiter(rateLimitCache, regexCache, rateLimitingConfig).recordLimitedRequest(info, director);
    }

    public boolean requestHasExpectedHeaders(HttpServletRequest request) {
        return request.getHeader(PowerApiHeader.USER.toString()) != null;
    }

    private void describeLimitsForRequest(final FilterDirector director, HttpServletRequest request) {
        
        // Should we include the absolute limits from the service origin?
        if (rateLimitingConfig.getRequestEndpoint().isIncludeAbsoluteLimits()) {

            // Process the response on the way back up the filter chain
            director.setFilterAction(FilterAction.PROCESS_RESPONSE);

            if (acceptType == null || acceptType.getValue().equalsIgnoreCase(MimeType.APPLICATION_JSON.toString())) {
                director.requestHeaderManager().putHeader("Accept", MimeType.APPLICATION_XML.toString());
            }

        } else {
            if(acceptType.toString().equals(MimeType.WILDCARD.getMimeType())){
                acceptType = new MediaType(MimeType.APPLICATION_JSON);
            }
            new RateLimiterResponse(rateLimitCache, rateLimitingConfig).writeActiveLimits(new RateLimitingRequestInfo(request, acceptType), director);

            director.setFilterAction(FilterAction.RETURN);
            director.setResponseStatus(HttpStatusCode.OK);
        }
    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector director = new FilterDirectorImpl();
        MediaType preferredMediaRange = new MediaType(MimeType.APPLICATION_JSON); // defaulting this to json for now
        // TODO: figure out a better way of detecting this besides null; feels dirty =/
        if (originalAcceptHeaders != null) {
            preferredMediaRange = QualityFactorUtility.choosePreferredHeaderValue(mediaRanges);
        }

        new RateLimiterResponse(rateLimitCache, rateLimitingConfig).writeCombinedLimits(new RateLimitingRequestInfo(request, preferredMediaRange), response, director);
        director.responseHeaderManager().appendHeader("Content-Type", preferredMediaRange.getMimeType().getMimeType());
        return director;
    }
}

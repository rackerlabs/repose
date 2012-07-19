package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpDate;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.media.MediaRangeProcessor;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.ratelimit.log.LimitLogger;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.repose.service.ratelimit.exception.CacheException;
import com.rackspace.repose.service.ratelimit.exception.OverLimitException;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;


public class RateLimitingHandler extends AbstractFilterLogicHandler {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingHandler.class);
   private static final MediaType DEFAULT_TYPE = new MediaType(MimeType.APPLICATION_JSON);
   private MediaType originalPreferredAccept;
   private final boolean includeAbsoluteLimits;
   private final boolean responseDelegationEnabled;
   private final Pattern describeLimitsUriPattern;
   private final RateLimitingServiceWrapper rateLimitingServiceWrapper;

   public RateLimitingHandler(RateLimitingServiceWrapper rateLimitingServiceWrapper, boolean includeAbsoluteLimits, boolean responseDelegationEnabled, Pattern describeLimitsUriPattern) {
      this.includeAbsoluteLimits = includeAbsoluteLimits;
      this.responseDelegationEnabled = responseDelegationEnabled;
      this.describeLimitsUriPattern = describeLimitsUriPattern;
      this.rateLimitingServiceWrapper = rateLimitingServiceWrapper;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector director = new FilterDirectorImpl();
      MutableHttpServletRequest mutableRequest = MutableHttpServletRequest.wrap(request);
      MediaRangeProcessor processor = new MediaRangeProcessor(mutableRequest.getPreferredHeaderValues(CommonHttpHeader.ACCEPT.toString(), DEFAULT_TYPE));

      originalPreferredAccept = processor.process().get(0);
      MediaType preferredMediaType = originalPreferredAccept;

      if (requestHasExpectedHeaders(request)) {
         final String requestUri = request.getRequestURI();

         // request now considered valid with user.
         director.setFilterAction(FilterAction.PASS);

         // Does the request match the configured getCurrentLimits API call endpoint?
         if (describeLimitsUriPattern.matcher(requestUri).matches()) {
            describeLimitsForRequest(request, director, preferredMediaType);
         } else {
            recordLimitedRequest(request, director);
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

   @Override
   public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector director = new FilterDirectorImpl();

      try {
         final MimeType mimeType = rateLimitingServiceWrapper.queryCombinedLimits(request, originalPreferredAccept, response.getBufferedOutputAsInputStream(), director.getResponseOutputStream());

         director.responseHeaderManager().putHeader(CommonHttpHeader.CONTENT_TYPE.toString(), mimeType.toString());
      } catch (Exception e) {
         consumeException(e, director);
      }

      return director;
   }

   private boolean requestHasExpectedHeaders(HttpServletRequest request) {
      return request.getHeader(PowerApiHeader.USER.toString()) != null;
   }

   private void consumeException(Exception e, FilterDirector director) {
      LOG.error("Failure when querying limits. Reason: " + e.getMessage(), e);

      director.setFilterAction(FilterAction.RETURN);
      director.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
   }

   private void describeLimitsForRequest(HttpServletRequest request, FilterDirector director, MediaType preferredMediaType) {
      if (preferredMediaType.getMimeType() == MimeType.UNKNOWN) {
         director.setFilterAction(FilterAction.RETURN);
         director.setResponseStatus(HttpStatusCode.NOT_ACCEPTABLE);
      } else {
         // If include absolute limits let request pass thru but prepare the combined
         // (absolute and active) limits when processing the response
         if (includeAbsoluteLimits) {
            director.setFilterAction(FilterAction.PROCESS_RESPONSE);
            director.requestHeaderManager().putHeader(CommonHttpHeader.ACCEPT.toString(), MimeType.APPLICATION_XML.toString());
         } else {
            try {
               final MimeType mimeType = rateLimitingServiceWrapper.queryActiveLimits(request, preferredMediaType, director.getResponseOutputStream());

               director.responseHeaderManager().putHeader(CommonHttpHeader.CONTENT_TYPE.toString(), mimeType.toString());
               director.setFilterAction(FilterAction.RETURN);
               director.setResponseStatus(HttpStatusCode.OK);
            } catch (Exception e) {
               consumeException(e, director);
            }
         }
      }
   }

   private void recordLimitedRequest(HttpServletRequest request, FilterDirector director) {
      try {
         rateLimitingServiceWrapper.trackLimits(request);
      } catch (OverLimitException e) {
         new LimitLogger(e.getUser(), request).log(e.getConfiguredLimit(), Integer.toString(e.getCurrentLimitAmount()));
         final HttpDate nextAvailableTime = new HttpDate(e.getNextAvailableTime());

         if (!responseDelegationEnabled) {
            // Tell the filter we want to return right away
            director.setFilterAction(FilterAction.RETURN);

            // We use a 413 "Request Entity Too Large" to communicate that the user
            // in question has hit their rate limit for this requested URI
            director.setResponseStatus(HttpStatusCode.REQUEST_ENTITY_TOO_LARGE);
            director.responseHeaderManager().appendHeader(CommonHttpHeader.RETRY_AFTER.toString(), nextAvailableTime.toRFC1123());
         } else {
            // When response delegation is enabled, we add the X-PP-Rate-Limited
            // request header with its value set to the correctly formatted
            // retry-after date that would have been returned to the client
            director.setFilterAction(FilterAction.PASS);
            director.requestHeaderManager().putHeader(PowerApiHeader.RATE_LIMITED.toString(), nextAvailableTime.toRFC1123());
         }

      } catch (CacheException e) {
         LOG.error("Failure when tracking limits. Reason: " + e.getMessage(), e);

         director.setFilterAction(FilterAction.RETURN);
         director.setResponseStatus(HttpStatusCode.BAD_GATEWAY);
      }
   }
}

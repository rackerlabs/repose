/*
 *
 */
package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderChooser;
import com.rackspace.papi.commons.util.http.header.QualityFactorHeaderChooser;
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
   private static final HeaderChooser<MediaType> ACCEPT_TYPE_CHOOSER = new QualityFactorHeaderChooser<MediaType>(new MediaType(MimeType.APPLICATION_JSON));
   private final Map<String, Map<String, Pattern>> regexCache;
   private final RateLimitCache rateLimitCache;
   private final Pattern describeLimitsUriRegex;
   private final RateLimitingConfiguration rateLimitingConfig;
   private final RateLimiterBuilder rateLimiterBuilder;
   private Enumeration<String> originalAcceptHeaders;

   public RateLimitingHandler(Map<String, Map<String, Pattern>> regexCache, RateLimitCache rateLimitCache, Pattern describeLimitsUriRegex, RateLimitingConfiguration rateLimitingConfig, RateLimiterBuilder rateLimiterBuilder) {
      this.regexCache = regexCache;
      this.rateLimitCache = rateLimitCache;
      this.describeLimitsUriRegex = describeLimitsUriRegex;
      this.rateLimitingConfig = rateLimitingConfig;
      this.rateLimiterBuilder = rateLimiterBuilder;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector director = new FilterDirectorImpl();

      originalAcceptHeaders = request.getHeaders("Accept");
      final MediaType preferredMediaType = ACCEPT_TYPE_CHOOSER.choosePreferredHeaderValue(new MediaRangeParser(request.getHeaders("Accept")).parse());

      // Does the request contain expected headers?
      if (requestHasExpectedHeaders(request)) {
         final String requestUri = request.getRequestURI();

         // We set the default action to PASS in this case since further
         // logic may or may not change the action and this request can now
         // be considered valid.
         director.setFilterAction(FilterAction.PASS);

         // Does the request match the configured getCurrentLimits API call endpoint?
         if (describeLimitsUriRegex.matcher(requestUri).matches()) {
            describeLimitsForRequest(director, request, preferredMediaType);
         } else {
            recordLimitedRequest(new RateLimitingRequestInfo(request, preferredMediaType), director);
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
      rateLimiterBuilder.buildRateLimiter(rateLimitCache, regexCache, rateLimitingConfig).recordLimitedRequest(info, director);
   }

   public boolean requestHasExpectedHeaders(HttpServletRequest request) {
      return request.getHeader(PowerApiHeader.USER.toString()) != null;
   }

   private void describeLimitsForRequest(FilterDirector director, HttpServletRequest request, MediaType preferredMediaType) {
      if (preferredMediaType.getMimeType() == MimeType.UNKNOWN) {
         director.setFilterAction(FilterAction.RETURN);
         director.setResponseStatus(HttpStatusCode.NOT_ACCEPTABLE);
      } else {

         // Should we include the absolute limits from the service origin?
         if (rateLimitingConfig.getRequestEndpoint().isIncludeAbsoluteLimits()) {
            director.setFilterAction(FilterAction.PROCESS_RESPONSE);
            director.requestHeaderManager().putHeader("Accept", MimeType.APPLICATION_XML.toString());
         } else {
            new RateLimiterResponse(rateLimitCache, rateLimitingConfig).writeActiveLimits(new RateLimitingRequestInfo(request, preferredMediaType), director);

            director.setFilterAction(FilterAction.RETURN);
            director.setResponseStatus(HttpStatusCode.OK);
         }
      }
   }

   @Override
   public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector director = new FilterDirectorImpl();

      final MediaType preferredMediaType = ACCEPT_TYPE_CHOOSER.choosePreferredHeaderValue(new MediaRangeParser(originalAcceptHeaders).parse());
      director.responseHeaderManager().putHeader("content-Type", preferredMediaType.getMimeType().getMimeType());

      new RateLimiterResponse(rateLimitCache, rateLimitingConfig).writeCombinedLimits(new RateLimitingRequestInfo(request, preferredMediaType), response, director);

      return director;
   }
}

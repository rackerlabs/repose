package org.openrepose.filters.ratelimiting;

import com.google.common.base.Optional;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.http.HttpDate;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.media.MediaRangeProcessor;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.filters.ratelimiting.log.LimitLogger;
import org.openrepose.services.datastore.DatastoreOperationException;
import org.openrepose.services.ratelimit.RateLimitingServiceImpl;
import org.openrepose.services.ratelimit.exception.CacheException;
import org.openrepose.services.ratelimit.exception.OverLimitException;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.regex.Pattern;


/* Responsible for handling requests and responses to ratelimiting, also tracks and provides limits */
public class RateLimitingHandler extends AbstractFilterLogicHandler {

  private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingHandler.class);
  private static final MediaType DEFAULT_TYPE = new MediaType(MimeType.APPLICATION_JSON);
  private MediaType originalPreferredAccept;
  private final boolean includeAbsoluteLimits;
  private final Optional<Pattern> describeLimitsUriPattern;
  private final RateLimitingServiceHelper rateLimitingServiceHelper;
  private boolean overLimit429ResponseCode;
  private int datastoreWarnLimit;

  public RateLimitingHandler(RateLimitingServiceHelper rateLimitingServiceHelper, boolean includeAbsoluteLimits, Optional<Pattern> describeLimitsUriPattern, boolean overLimit429ResponseCode, int datastoreWarnLimit) {
    this.includeAbsoluteLimits = includeAbsoluteLimits;
    this.describeLimitsUriPattern = describeLimitsUriPattern;
    this.rateLimitingServiceHelper = rateLimitingServiceHelper;
    this.overLimit429ResponseCode = overLimit429ResponseCode;
    this.datastoreWarnLimit=datastoreWarnLimit;
  }

  @Override
  public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
    final FilterDirector director = new FilterDirectorImpl();
    MutableHttpServletRequest mutableRequest = MutableHttpServletRequest.wrap(request);
    MediaRangeProcessor processor = new MediaRangeProcessor(mutableRequest.getPreferredHeaders(CommonHttpHeader.ACCEPT.toString(), DEFAULT_TYPE));


    List<MediaType> mediaTypes = processor.process();


    if (requestHasExpectedHeaders(request)) {
      originalPreferredAccept = getPreferredMediaType(mediaTypes);
      MediaType preferredMediaType = originalPreferredAccept;

      final String requestUri = request.getRequestURI();

      // request now considered valid with user.
      director.setFilterAction(FilterAction.PASS);

        boolean pass = false;

        try {
            // Record limits
            pass = recordLimitedRequest(request, director);
        } catch (DatastoreOperationException doe) {
            LOG.error("Unable to communicate with dist-datastore.", doe);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }

      // Does the request match the configured getCurrentLimits API call endpoint?
      if (pass && describeLimitsUriPattern.isPresent() && describeLimitsUriPattern.get().matcher(requestUri).matches()) {
        describeLimitsForRequest(request, director, preferredMediaType);
      }
    } else {
      LOG.warn("Expected header: {} was not supplied in the request. Rate limiting requires this header to operate.", PowerApiHeader.USER.toString());

      // Auto return a 401 if the request does not meet expectations
      director.setResponseStatusCode(HttpServletResponse.SC_UNAUTHORIZED);
      director.setFilterAction(FilterAction.RETURN);
    }

    return director;
  }

  private boolean requestHasExpectedHeaders(HttpServletRequest request) {
    return request.getHeader(PowerApiHeader.USER.toString()) != null;
  }

  private void consumeException(Exception e, FilterDirector director) {
    LOG.error("Failure when querying limits. Reason: " + e.getMessage(), e);

    director.setFilterAction(FilterAction.RETURN);
    director.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  private void describeLimitsForRequest(HttpServletRequest request, FilterDirector director, MediaType preferredMediaType) {
    if (preferredMediaType.getMimeType() == MimeType.UNKNOWN) {
      director.setFilterAction(FilterAction.RETURN);
      director.setResponseStatusCode(HttpServletResponse.SC_NOT_ACCEPTABLE);
    } else {
      // If include absolute limits let request pass thru but prepare the combined
      // (absolute and active) limits when processing the response
      // TODO: A way to query global rate limits
      if (includeAbsoluteLimits) {
        director.setFilterAction(FilterAction.PROCESS_RESPONSE);
        director.requestHeaderManager().putHeader(CommonHttpHeader.ACCEPT.toString(), MimeType.APPLICATION_XML.toString());
      } else {
        try {
          final MimeType mimeType = rateLimitingServiceHelper.queryActiveLimits(request, preferredMediaType, director.getResponseOutputStream());

          director.responseHeaderManager().putHeader(CommonHttpHeader.CONTENT_TYPE.toString(), mimeType.toString());
          director.setFilterAction(FilterAction.RETURN);
          director.setResponseStatusCode(HttpServletResponse.SC_OK);
        } catch (Exception e) {
          consumeException(e, director);
        }
      }
    }
  }

  /**
   * @return false if over-limit and response delegation is not enabled
   */
  private boolean recordLimitedRequest(HttpServletRequest request, FilterDirector director) {
    boolean pass = true;

    try {
      rateLimitingServiceHelper.trackLimits(request,datastoreWarnLimit);
    } catch (OverLimitException e) {
      LOG.trace("Over Limit", e);
      new LimitLogger(e.getUser(), request).log(e.getConfiguredLimit(), Integer.toString(e.getCurrentLimitAmount()));
      final HttpDate nextAvailableTime = new HttpDate(e.getNextAvailableTime());

      // Tell the filter we want to return right away
      director.setFilterAction(FilterAction.RETURN);
      pass = false;

      // We use a 413 "Request Entity Too Large" to communicate that the user
      // in question has hit their rate limit for this requested URI
      if (e.getUser().equals(RateLimitingServiceImpl.GLOBAL_LIMIT_USER)) {
        director.setResponseStatusCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      } else if (overLimit429ResponseCode) {
        director.setResponseStatusCode(FilterDirector.SC_TOO_MANY_REQUESTS);
      } else {
        director.setResponseStatusCode(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
      }
      director.responseHeaderManager().appendHeader(CommonHttpHeader.RETRY_AFTER.toString(), nextAvailableTime.toRFC1123());

    } catch (CacheException e) {
      LOG.error("Failure when tracking limits.", e);

      director.setFilterAction(FilterAction.RETURN);
      director.setResponseStatusCode(HttpServletResponse.SC_BAD_GATEWAY);
    }

    return pass;
  }

  @Override
  public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
    final FilterDirector director = new FilterDirectorImpl();

    try {
      final MimeType mimeType = rateLimitingServiceHelper.queryCombinedLimits(request, originalPreferredAccept, response.getBufferedOutputAsInputStream(), director.getResponseOutputStream());

      director.responseHeaderManager().putHeader(CommonHttpHeader.CONTENT_TYPE.toString(), mimeType.toString());
    } catch (Exception e) {
      consumeException(e, director);
    }

    return director;
  }

  public MediaType getPreferredMediaType(List<MediaType> mediaTypes) {

    for (MediaType mediaType : mediaTypes) {

      if (mediaType.getMimeType() == MimeType.APPLICATION_XML) {
        return mediaType;
      } else if (mediaType.getMimeType() == MimeType.APPLICATION_JSON) {
        return mediaType;
      }
    }

    return mediaTypes.get(0);

  }
}

package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import com.rackspace.papi.commons.util.http.media.MediaType;

import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.repose.service.limits.schema.RateLimitList;
import com.rackspace.papi.components.ratelimit.write.ActiveLimitsWriter;
import com.rackspace.papi.components.ratelimit.write.CombinedLimitsWriter;
import com.rackspace.repose.service.ratelimit.RateLimitingService;
import com.rackspace.repose.service.ratelimit.exception.OverLimitException;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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

   public MimeType queryActiveLimits(HttpServletRequest request, MediaType preferredMediaType, OutputStream outputStream) {
      RateLimitList rateLimits = service.queryLimits(getPreferredUser(request), getPreferredGroups(request));
      javax.ws.rs.core.MediaType mediaType = activeLimitsWriter.write(rateLimits, getJavaMediaType(preferredMediaType.getMimeType()), outputStream);

      return getReposeMimeType(mediaType);
   }

   public MimeType queryCombinedLimits(HttpServletRequest request, MediaType preferredMediaType, InputStream absoluteLimits, OutputStream outputStream) {
      RateLimitList rateLimits = service.queryLimits(getPreferredUser(request), getPreferredGroups(request));
      javax.ws.rs.core.MediaType mediaType = combinedLimitsWriter.write(rateLimits, getJavaMediaType(preferredMediaType.getMimeType()), absoluteLimits, outputStream);

      return getReposeMimeType(mediaType);
   }

   public void trackLimits(HttpServletRequest request,int datastoreWarnLimit) throws OverLimitException {
      service.trackLimits(getPreferredUser(request), getPreferredGroups(request), request.getRequestURI(), request.getMethod(),datastoreWarnLimit);
   }

   public MimeType getReposeMimeType(javax.ws.rs.core.MediaType mediaType) {
      return MimeType.guessMediaTypeFromString(mediaType.toString());
   }

   public javax.ws.rs.core.MediaType getJavaMediaType(MimeType reposeMimeType) {
      return new javax.ws.rs.core.MediaType(reposeMimeType.getType(), reposeMimeType.getSubType());
   }

   public String getPreferredUser(HttpServletRequest request) {
      final MutableHttpServletRequest mutableRequest = MutableHttpServletRequest.wrap(request);
      final HeaderValue userNameHeaderValue = mutableRequest.getPreferredHeader(PowerApiHeader.USER.toString(), new HeaderValueImpl(""));

      return userNameHeaderValue.getValue();
   }

   public List<String> getPreferredGroups(HttpServletRequest request) {
      final MutableHttpServletRequest mutableRequest = MutableHttpServletRequest.wrap(request);
      final List<? extends HeaderValue> userGroup = mutableRequest.getPreferredHeaderValues(PowerApiHeader.GROUPS.toString(), null);
      final List<String> groups = new ArrayList<String>();

      for (HeaderValue group : userGroup) {
         groups.add(group.getValue());
      }

      return groups;
   }
}

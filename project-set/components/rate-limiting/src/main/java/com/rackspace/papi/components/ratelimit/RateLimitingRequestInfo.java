package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.components.limits.schema.HttpMethod;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class RateLimitingRequestInfo {
   
   private final HttpMethod requestMethod;
   private final List<? extends HeaderValue> userGroup;
   private final HeaderValue userName;
   private final HttpServletRequest request;
   private final MediaType acceptType;

   public RateLimitingRequestInfo(HttpServletRequest request, MediaType acceptType) {
      MutableHttpServletRequest mutableRequest = MutableHttpServletRequest.wrap(request);
      this.request = request;

      this.acceptType = acceptType;
      
      userName = mutableRequest.getPreferredHeader(PowerApiHeader.USER.toString(), new HeaderValueImpl(""));
      userGroup = mutableRequest.getPreferredHeaderValues(PowerApiHeader.GROUPS.toString(), new HeaderValueImpl(""));

      requestMethod = HttpMethod.fromValue(request.getMethod().toUpperCase());
   }

   public List<? extends HeaderValue> getUserGroups() {
      return userGroup;
   }

   public HeaderValue getUserName() {
      return userName;
   }

   public HttpMethod getRequestMethod() {
      return requestMethod;
   }

   public HttpServletRequest getRequest() {
      return request;
   }

   public MediaType getAcceptType() {
      return acceptType;
   }
}

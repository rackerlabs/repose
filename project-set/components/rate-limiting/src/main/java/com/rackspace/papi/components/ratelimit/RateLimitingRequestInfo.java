package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderFieldParser;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.QualityFactorUtility;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.components.limits.schema.HttpMethod;

import java.util.Collection;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class RateLimitingRequestInfo {

   private final Deque<String> allUserGroups;
   private final HttpMethod requestMethod;
   private final HeaderValue userName;
   private final HttpServletRequest request;
   private final MediaType acceptType;

   // TODO:Review - Consider builder pattern?
   public RateLimitingRequestInfo(HttpServletRequest request, MediaType acceptType) {
      this.request = request;

      this.acceptType = acceptType;

      final List<HeaderValue> values = new HeaderFieldParser(request.getHeaders(PowerApiHeader.USER.toString())).parse();
      userName = QualityFactorUtility.choosePreferedHeaderValue(values);

      allUserGroups = new LinkedList<String>();

      for (Enumeration<String> groupHeaders = request.getHeaders(PowerApiHeader.GROUPS.toString()); groupHeaders.hasMoreElements();) {
         allUserGroups.add(groupHeaders.nextElement());
      }

      requestMethod = HttpMethod.fromValue(request.getMethod().toUpperCase());
   }

   public String getFirstUserGroup() {
      return allUserGroups.isEmpty() ? null : allUserGroups.getFirst();
   }

   public Collection<String> getUserGroups() {
      return allUserGroups;
   }

   public String getUserName() {
      return userName.getValue();
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

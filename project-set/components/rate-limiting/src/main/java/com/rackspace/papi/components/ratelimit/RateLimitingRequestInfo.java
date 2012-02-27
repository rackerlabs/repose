package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderFieldParser;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.QualityFactorUtility;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.components.limits.schema.HttpMethod;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class RateLimitingRequestInfo {

   private final List<HeaderValue> userGroups;
   private final HttpMethod requestMethod;
   private final HeaderValue userName;
   private final HttpServletRequest request;
   private final MediaType acceptType;

   // TODO:Review - Consider builder pattern?
   public RateLimitingRequestInfo(HttpServletRequest request, MediaType acceptType) {
      this.request = request;

      this.acceptType = acceptType;

      final List<HeaderValue> values = new HeaderFieldParser(request.getHeaders(PowerApiHeader.USER.toString())).parse();
      userName = QualityFactorUtility.choosePreferredHeaderValue(values);

      final List<HeaderValue> groups = new HeaderFieldParser(request.getHeaders(PowerApiHeader.GROUPS.toString())).parse();
      userGroups = QualityFactorUtility.choosePreferredHeaderValues(groups);
              
      requestMethod = HttpMethod.fromValue(request.getMethod().toUpperCase());
   }

   public String getFirstUserGroup() {
      return userGroups.isEmpty() || userGroups.size() == 0 ? null : userGroups.get(0).getValue();
   }

   public List<HeaderValue> getUserGroups() {
      return userGroups;
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

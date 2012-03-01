package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderFieldParser;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import com.rackspace.papi.commons.util.http.header.QualityFactorHeaderChooser;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.components.limits.schema.HttpMethod;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class RateLimitingRequestInfo {

   private static final HeaderValue DEFAULT_EMPTTY = new HeaderValueImpl("", -1.0);
   
   private final HttpMethod requestMethod;
   private final List<HeaderValue> userGroup;
   private final HeaderValue userName;
   private final HttpServletRequest request;
   private final MediaType acceptType;

   public RateLimitingRequestInfo(HttpServletRequest request, MediaType acceptType) {
      this.request = request;

      this.acceptType = acceptType;

      final List<HeaderValue> values = new HeaderFieldParser(request.getHeaders(PowerApiHeader.USER.toString())).parse();
      userName = new QualityFactorHeaderChooser(DEFAULT_EMPTTY).choosePreferredHeaderValue(values);
      
      final List<HeaderValue> groups = new HeaderFieldParser(request.getHeaders(PowerApiHeader.GROUPS.toString())).parse();
      userGroup = new QualityFactorHeaderChooser(DEFAULT_EMPTTY).choosePreferredHeaderValues(groups);
              
      requestMethod = HttpMethod.fromValue(request.getMethod().toUpperCase());
   }

   public List<HeaderValue> getUserGroups() {
      return userGroup;
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

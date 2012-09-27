package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;

/**
 * If the username is an auth token return some other "log friendly"
 * form of user identification.
 */
public class UserIdentificationSanitizer {

   private final RateLimitingRequestInfo requestInfo;

   public UserIdentificationSanitizer(RateLimitingRequestInfo requestInfo) {
      this.requestInfo = requestInfo;
   }

   public String getUserIdentification() {
      String userIdentification = requestInfo.getUserName().getValue();

      final String xAuthToken = requestInfo.getRequest().getHeader(CommonHttpHeader.AUTH_TOKEN.toString());

      if (StringUtilities.nullSafeEqualsIgnoreCase(xAuthToken, userIdentification)) {
         final String xForwardedFor = requestInfo.getRequest().getHeader(CommonHttpHeader.X_FORWARDED_FOR.toString());

         userIdentification = xForwardedFor != null ? xForwardedFor : requestInfo.getRequest().getRemoteHost();
      }

      return userIdentification;
   }
}

package com.rackspace.papi.components.ratelimit.util;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.components.ratelimit.RateLimitingRequestInfo;

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
         final String XForwardedFor = requestInfo.getRequest().getHeader(CommonHttpHeader.X_FORWARDED_FOR.toString());

         userIdentification = XForwardedFor != null ? XForwardedFor : requestInfo.getRequest().getRemoteHost();
      }

      return userIdentification;
   }
}

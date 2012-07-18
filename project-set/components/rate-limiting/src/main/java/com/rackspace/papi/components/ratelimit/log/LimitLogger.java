package com.rackspace.papi.components.ratelimit.log;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;

public class LimitLogger {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(LimitLogger.class);
   private final String user;
   private final HttpServletRequest request;

   public LimitLogger(String user, HttpServletRequest request) {
      this.user = user;
      this.request = request;
   }

   public void log(String configured, String used) {

      LOG.info("Rate limiting user " + getSanitizedUserIdentification() + " at limit amount " + used + ".");
      LOG.info("User rate limited for request " + request.getMethod() + " " + request.getRequestURL() + ".");
      LOG.info("Configured rate limit is: " + configured);
   }

   public String getSanitizedUserIdentification() {
      String userIdentification = user;

      final String xAuthToken = request.getHeader(CommonHttpHeader.AUTH_TOKEN.toString());

      if (StringUtilities.nullSafeEqualsIgnoreCase(xAuthToken, userIdentification)) {
         final String xForwardedFor = request.getHeader(CommonHttpHeader.X_FORWARDED_FOR.toString());

         userIdentification = xForwardedFor != null ? xForwardedFor : request.getRemoteHost();
      }

      return userIdentification;
   }
}

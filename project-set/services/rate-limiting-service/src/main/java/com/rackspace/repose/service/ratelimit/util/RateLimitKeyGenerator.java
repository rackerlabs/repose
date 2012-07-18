package com.rackspace.repose.service.ratelimit.util;

import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;

/**
 * @author Freynard
 */
public final class RateLimitKeyGenerator {
   private static final String UNDERSCORE = "_";
   
   private RateLimitKeyGenerator(){
   }

   public static String createMapKey(ConfiguredRatelimit limit) {
      StringBuilder builder = new StringBuilder();

      // Key will look something like this:
      // /v[^/]+/(\d+)/?.*_POST_GET_MINUTE_10
      builder.append(limit.getUriRegex());

      for (HttpMethod httpMethod : limit.getHttpMethods()) {
         builder.append(UNDERSCORE).append(httpMethod.value());
      }

      builder.append(UNDERSCORE).append(limit.getUnit().value());
      builder.append(UNDERSCORE).append(Integer.valueOf(limit.getValue()).toString());

      return builder.toString();
   }
}

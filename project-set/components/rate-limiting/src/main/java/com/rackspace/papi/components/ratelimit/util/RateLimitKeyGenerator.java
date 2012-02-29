package com.rackspace.papi.components.ratelimit.util;

import com.rackspace.papi.components.limits.schema.HttpMethod;
import com.rackspace.papi.components.ratelimit.config.ConfiguredRatelimit;

/**
 * @author Freynard
 */
public class RateLimitKeyGenerator {
   private static final String UNDERSCORE = "_";

   public static String createMapKey(ConfiguredRatelimit limit) {
      StringBuilder builder = new StringBuilder();

      // Key will look something like this:
      // /v[^/]+/(\d+)/?.*_POST_GET_MINUTE_10
      builder.append(limit.getUriRegex());

      for (HttpMethod httpMethod : limit.getHttpMethods()) {
         builder.append(UNDERSCORE + httpMethod.value());
      }

      builder.append(UNDERSCORE + limit.getUnit().value());
      builder.append(UNDERSCORE + new Integer(limit.getValue()).toString());

      return builder.toString();
   }
}

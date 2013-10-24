package com.rackspace.repose.service.ratelimit;

import org.slf4j.Logger;

import java.util.regex.Matcher;

/*
 * This class is a utility class used to generate cache keys for the
 * rate-limiting filter
 */
public class LimitKey {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(LimitKey.class);

   public static String getLimitKey(Matcher uriMatcher, boolean useCaptureGroups) {
      // The group count represents the number of elements that will go into
      // generating the unique cache id for the requested URI
      final int groupCount = uriMatcher.groupCount();

      final StringBuilder cacheIdBuffer = new StringBuilder();

      // All cacheId's contain the full regex pattern
      cacheIdBuffer.append(uriMatcher.pattern().toString());

      if (useCaptureGroups) {
          // Capture groups are appended to the pattern for uniqueness
          for (int i = 1; i <= groupCount; ++i) {
            cacheIdBuffer.append(uriMatcher.group(i));
          }
      }

      return cacheIdBuffer.toString();
   }
}

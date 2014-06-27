package com.rackspace.repose.service.ratelimit;

import org.slf4j.Logger;

import java.util.List;
import java.util.regex.Matcher;

/*
 * This class is a utility class used to generate cache keys for the
 * rate-limiting filter
 */
public class LimitKey {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(LimitKey.class);

    private LimitKey() {}

    public static String getLimitKey(String limitGroup, String limitId, Matcher uriMatcher, List<Matcher> queryStringMatchers, boolean useCaptureGroups) {
        // The group count represents the number of elements that will go into
        // generating the unique cache id for the requested URI
        final StringBuilder cacheIdBuffer = new StringBuilder();

        // All cacheId's contain the unique limit group
        cacheIdBuffer.append(String.valueOf(limitGroup.hashCode()));

        // All cacheId's contain the unique limit Id
        cacheIdBuffer.append(":" + String.valueOf(limitId.hashCode()));

        // If using capture groups, captured text is hashed and appended
        if (useCaptureGroups) {
            for (int i = 1; i <= uriMatcher.groupCount(); ++i) {
                cacheIdBuffer.append(":" + String.valueOf(uriMatcher.group(i).hashCode()));
            }
            for (Matcher queryMatcher : queryStringMatchers) {
                for (int i = 1; i <= queryMatcher.groupCount(); ++i) {
                    cacheIdBuffer.append(":" + String.valueOf(queryMatcher.group(i).hashCode()));
                }
            }
        }

        return cacheIdBuffer.toString();
    }
}

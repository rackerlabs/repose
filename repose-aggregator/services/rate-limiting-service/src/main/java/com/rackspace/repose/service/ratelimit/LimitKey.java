package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.limits.schema.HttpMethod;
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

    public static String getConfigLimitKey(String uriRegex, List<HttpMethod> methods) {
        final StringBuilder buffer = new StringBuilder();

        buffer.append(String.valueOf(uriRegex.hashCode()));
        for (HttpMethod method : methods) {
            buffer.append(":" + String.valueOf(method.toString().hashCode()));
        }

        return buffer.toString();
    }

    public static String getLimitKey(Matcher uriMatcher, List<HttpMethod> methods, boolean useCaptureGroups) {
        // The group count represents the number of elements that will go into
        // generating the unique cache id for the requested URI
        final int groupCount = uriMatcher.groupCount();

        final StringBuilder cacheIdBuffer = new StringBuilder();

        // All cacheId's contain the full regex pattern and all associated methods
        cacheIdBuffer.append(String.valueOf(uriMatcher.pattern().toString().hashCode()));
        for (HttpMethod method : methods) {
            cacheIdBuffer.append(":" + String.valueOf(method.toString().hashCode()));
        }

        if (useCaptureGroups) {
            // Capture groups are appended to the pattern for uniqueness
            for (int i = 1; i <= groupCount; ++i) {
                cacheIdBuffer.append(":" + String.valueOf(uriMatcher.group(i).hashCode()));
            }
        }

        return cacheIdBuffer.toString();
    }
}

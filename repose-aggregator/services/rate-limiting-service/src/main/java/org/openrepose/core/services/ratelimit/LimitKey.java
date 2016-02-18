/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.ratelimit;

import org.slf4j.Logger;

import java.util.List;
import java.util.regex.Matcher;

/*
 * This class is a utility class used to generate cache keys for the
 * rate-limiting filter
 */
public class LimitKey {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(LimitKey.class);

    private LimitKey() {
    }

    public static String getLimitKey(String limitGroup, String limitId, Matcher uriMatcher, List<Matcher> queryParamMatchers, boolean useCaptureGroups) {
        // The group count represents the number of elements that will go into
        // generating the unique cache id for the requested URI
        final int groupCount = uriMatcher.groupCount();

        final StringBuilder cacheIdBuffer = new StringBuilder();

        // All cacheId's contain the unique limit group
        cacheIdBuffer.append(String.valueOf(limitGroup.hashCode()));

        // All cacheId's contain the unique limit Id
        cacheIdBuffer.append(":");
        cacheIdBuffer.append(String.valueOf(limitId.hashCode()));

        // If using capture groups, captured text is hashed and appended
        if (useCaptureGroups) {
            for (int i = 1; i <= groupCount; ++i) {
                cacheIdBuffer.append(":");
                cacheIdBuffer.append(String.valueOf(uriMatcher.group(i).hashCode()));
            }

            // To ignore the issue of ordering in the query string, we generate a cumulative hash code using the XOR
            // operator.
            int queryStringHash = 0;
            boolean captureGroupPresent = false;
            for (Matcher queryMatcher : queryParamMatchers) {
                for (int i = 1; i <= queryMatcher.groupCount(); ++i) {
                    captureGroupPresent = true;
                    queryStringHash ^= queryMatcher.group(i).hashCode();
                }
            }
            if (captureGroupPresent) {
                cacheIdBuffer.append(":");
                cacheIdBuffer.append(String.valueOf(queryStringHash));
            }
        }

        return cacheIdBuffer.toString();
    }
}

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
package org.openrepose.core.services.reporting.metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility class for creating {@link com.codahale.metrics.Metric} names to be
 * used in a {@link com.codahale.metrics.MetricRegistry} and reported by
 * {@link com.codahale.metrics.Reporter}s.
 */
public class MetricNameUtility {

    private static final Map<Character, String> REPLACEMENT_CHARACTER_MAP;

    static {
        Map<Character, String> replacementMap = new HashMap<>();
        replacementMap.put('.', "_");
        REPLACEMENT_CHARACTER_MAP = Collections.unmodifiableMap(replacementMap);
    }

    private MetricNameUtility() {
        // Prevent construction of this utility class.
    }

    /**
     * Replaces all restricted characters with a safe string.
     *
     * @param name a raw name string
     * @return a safe name string
     */
    public static String safeReportingName(String name) {
        StringBuilder safeName = new StringBuilder(name.length());
        for (char c : name.toCharArray()) {
            safeName.append(REPLACEMENT_CHARACTER_MAP.getOrDefault(c, String.valueOf(c)));
        }
        return safeName.toString();
    }
}

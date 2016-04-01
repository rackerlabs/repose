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
package org.openrepose.core.services.rms;

import org.openrepose.core.services.rms.config.StatusCodeMatcher;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author fran
 */
public final class ImmutableStatusCodes {
    private final List<StatusCodeMatcher> statusCodeMatcherList = new LinkedList<>();
    private final Map<String, Pattern> statusCodeRegexes = new HashMap<>();

    private ImmutableStatusCodes(List<StatusCodeMatcher> statusCodes) {
        statusCodeMatcherList.clear();
        statusCodeMatcherList.addAll(statusCodes);

        statusCodeRegexes.clear();
        for (StatusCodeMatcher code : statusCodeMatcherList) {
            statusCodeRegexes.put(code.getId(), Pattern.compile(code.getCodeRegex()));
        }
    }

    public static ImmutableStatusCodes build(List<StatusCodeMatcher> statusCodeMatchers) {
        return new ImmutableStatusCodes(statusCodeMatchers);
    }

    public StatusCodeMatcher getMatchingStatusCode(String statusCode) {
        StatusCodeMatcher matchedCode = null;

        for (StatusCodeMatcher code : statusCodeMatcherList) {
            if (statusCodeRegexes.get(code.getId()).matcher(statusCode).matches()) {
                matchedCode = code;
                break;
            }
        }

        return matchedCode;
    }
}

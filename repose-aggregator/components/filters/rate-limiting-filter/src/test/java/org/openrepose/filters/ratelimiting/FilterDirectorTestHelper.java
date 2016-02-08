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
package org.openrepose.filters.ratelimiting;

import org.openrepose.core.filter.logic.FilterDirector;

import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * TODO:Refactor - Move this class somewhere where other things can get at it (e.g. shared library)
 * TODO:Review/Refactor - Consider following JUnit's assert(...) instead of directorMust
 *
 * @author zinic
 */
public final class FilterDirectorTestHelper {

    public static void directorMustAddHeaderToRequest(FilterDirector filterDirector, String headerKey) {
        assertTrue("FilterDirector must add header, \"" + headerKey + "\" to the request.", filterDirector.requestHeaderManager().headersToAdd().containsKey(headerKey.toLowerCase()));
    }

    public static void directorMustRemoveHeaderToRequest(FilterDirector filterDirector, String headerKey) {
        assertTrue("FilterDirector must remove header, \"" + headerKey + "\" from the request.", filterDirector.requestHeaderManager().headersToRemove().contains(headerKey.toLowerCase()));
    }

    public static void directorMustAddHeaderValueToRequest(FilterDirector filterDirector, String headerKey, String expectedValue) {
        final Set<String> actualValues = filterDirector.requestHeaderManager().headersToAdd().get(headerKey.toLowerCase());

        assertTrue("FilterDirector must add header, \"" + headerKey + "\" with the value, \"" + expectedValue + "\" to the request.", actualValues != null ? actualValues.contains(expectedValue) : false);
    }

    public static void directorMustAddHeaderToResponse(FilterDirector filterDirector, String headerKey) {
        assertTrue("FilterDirector must add header, \"" + headerKey + "\" to the response.", filterDirector.responseHeaderManager().headersToAdd().containsKey(headerKey.toLowerCase()));
    }

    public static void directorMustRemoveHeaderToResponse(FilterDirector filterDirector, String headerKey) {
        assertTrue("FilterDirector must remove header, \"" + headerKey + "\" from the response.", filterDirector.responseHeaderManager().headersToRemove().contains(headerKey.toLowerCase()));
    }

    public static void directorMustAddHeaderValueToResponse(FilterDirector filterDirector, String headerKey, String expectedValue) {
        final Set<String> actualValues = filterDirector.responseHeaderManager().headersToAdd().get(headerKey.toLowerCase());

        assertTrue("FilterDirector must add header, \"" + headerKey + "\" with the value, \"" + expectedValue + "\" to the response.", actualValues != null ? actualValues.contains(expectedValue) : false);
    }
}

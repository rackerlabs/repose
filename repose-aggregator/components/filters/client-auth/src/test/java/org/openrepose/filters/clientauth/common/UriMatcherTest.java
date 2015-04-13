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
package org.openrepose.filters.clientauth.common;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class UriMatcherTest {

    public static class WhenCheckingIfUriOnWhiteList {
        private static final String WADL_REGEX = "/v1.0/application\\.wadl";
        private final List<Pattern> whiteListRegexPatterns = new ArrayList<Pattern>();
        private final UriMatcher uriMatcher;

        public WhenCheckingIfUriOnWhiteList() {
            whiteListRegexPatterns.add(Pattern.compile(WADL_REGEX));
            uriMatcher = new UriMatcher(whiteListRegexPatterns);
        }

        @Test
        public void shouldReturnTrueIfUriMatchesPatternInWhiteList() {
            final String REQUEST_URL = "/v1.0/application.wadl";
            assertTrue(uriMatcher.isUriOnWhiteList(REQUEST_URL));
        }

        @Test
        public void shouldReturnFalseIfUriDoesNotMatchPatternInWhiteList() {
            final String REQUEST_URL = "/v1.0/1234/loadbalancers?param=/application.wadl";
            assertFalse(uriMatcher.isUriOnWhiteList(REQUEST_URL));
        }
    }
}

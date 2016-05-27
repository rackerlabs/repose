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
package org.openrepose.commons.utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 *
 *
 */
@RunWith(Enclosed.class)
public class RegexListTest {

    public static final String MATCH_AGAINST = "match against me",
            FIND_REGEX = "match",
            MATCH_REGEX = ".*me";

    public static class WhenEmpty {

        @Test
        public void shouldReturnNullForFind() {
            assertNull(new RegexList().find(MATCH_AGAINST));
        }

        @Test
        public void shouldReturnNullForMatches() {
            assertNull(new RegexList().matches(MATCH_AGAINST));
        }
    }

    public static class WhenFinding {

        private RegexList regexList;

        @Before
        public void standUp() {
            regexList = new RegexList();
            regexList.add(FIND_REGEX);
        }

        @Test
        public void shouldFind() {
            assertNotNull(regexList.find(MATCH_AGAINST));
        }

        @Test
        public void shouldNotMatch() {
            assertNull(regexList.matches(MATCH_AGAINST));
        }
    }

    public static class WhenMatching {

        private RegexList regexList;

        @Before
        public void standUp() {
            regexList = new RegexList();
            regexList.add(MATCH_REGEX);
        }

        @Test
        public void shouldFind() {
            assertNotNull(regexList.find(MATCH_AGAINST));
        }

        @Test
        public void shouldMatch() {
            assertNotNull(regexList.matches(MATCH_AGAINST));
        }
    }
}

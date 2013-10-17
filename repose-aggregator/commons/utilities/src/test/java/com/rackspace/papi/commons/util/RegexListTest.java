package com.rackspace.papi.commons.util;

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

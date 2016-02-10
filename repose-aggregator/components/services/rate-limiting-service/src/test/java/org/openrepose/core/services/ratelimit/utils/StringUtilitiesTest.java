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
package org.openrepose.core.services.ratelimit.utils;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class StringUtilitiesTest {

    public static class WhenCheckingIfAStringIsBlank {

        @Test
        public void shouldHandleNulls() {
            assertTrue(org.openrepose.commons.utils.StringUtilities.isBlank(null));
        }

        @Test
        public void shouldHandleEmptyStrings() {
            assertTrue(StringUtilities.isBlank(null));
        }

        @Test
        public void shouldHandleBlankStrings() {
            assertTrue(StringUtilities.isBlank("     "));
        }

        @Test
        public void shouldHandleBlankStringsWithNewLines() {
            assertTrue(StringUtilities.isBlank("\n\n"));
        }

        @Test
        public void shouldHandleBlankStringsWithTabs() {
            assertTrue(StringUtilities.isBlank("\t\t"));
        }

        @Test
        public void shouldHandleComplexBlankStrings() {
            assertTrue(StringUtilities.isBlank("\n\n  \t  \t\n  \t\n   \n\t"));
        }

        @Test
        public void shouldRejectComplexNonBlankStrings() {
            assertFalse(StringUtilities.isBlank("\n\n  \t abc123 \t\n  \t\n   \n\t"));
        }

        @Test
        public void shouldRejectNonBlankStrings() {
            assertFalse(StringUtilities.isBlank("zf-adapter"));
        }
    }
}

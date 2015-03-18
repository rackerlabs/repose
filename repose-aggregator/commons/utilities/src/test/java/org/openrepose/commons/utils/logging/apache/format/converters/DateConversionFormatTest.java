/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.commons.utils.logging.apache.format.converters;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class DateConversionFormatTest {

    public static final class WhenGettingPatterns {

        @Test
        public void shouldGetCorrectPattern() {
            String format = DateConversionFormat.getPattern(DateConversionFormat.ISO_8601.name());
            assertNotNull(format);
            assertEquals(DateConversionFormat.ISO_8601.getPattern(), format);
        }
        
        @Test
        public void shouldGetDefaultPattern() {
            String format = DateConversionFormat.getPattern("Doesn't Exist");
            assertNotNull(format);
            assertEquals(DateConversionFormat.RFC_1123.getPattern(), format);
        }
    }

}

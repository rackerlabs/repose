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
package org.openrepose.commons.utils.http.header

import org.junit.Test

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.not
import static org.junit.Assert.assertThat

class HeaderNameTest {
    public static final String HEADER_NAME = "SomeName"

    @Test
    void "getName returns original string"() throws Exception {
        HeaderName headerNameMapKey = HeaderName.wrap(HEADER_NAME)

        assertThat(headerNameMapKey.getName(), equalTo(HEADER_NAME))
    }

    @Test
    void "getName does not change case"() throws Exception {
        HeaderName headerNameMapKey = HeaderName.wrap(HEADER_NAME)

        assertThat(headerNameMapKey.getName(), not(equalTo(HEADER_NAME.toLowerCase())))
    }

    @Test
    void "equals performs case insensitive comparison"() throws Exception {
        HeaderName headerNameMapKey = HeaderName.wrap(HEADER_NAME)
        HeaderName headerNameMapKeyLower = HeaderName.wrap(HEADER_NAME.toLowerCase())

        assertThat(headerNameMapKey, equalTo(headerNameMapKeyLower))
    }

    @Test
    void "generated hashCode is case insensitive"() throws Exception {
        HeaderName headerNameMapKey = HeaderName.wrap(HEADER_NAME)
        HeaderName headerNameMapKeyLower = HeaderName.wrap(HEADER_NAME.toLowerCase())

        assertThat(headerNameMapKey.hashCode(), equalTo(headerNameMapKeyLower.hashCode()))
    }

    @Test
    void "equals properly compares two null header names"() throws Exception {
        HeaderName headerNameMapKeyNull1 = HeaderName.wrap(null)
        HeaderName headerNameMapKeyNull2 = HeaderName.wrap(null)

        assertThat(headerNameMapKeyNull1, equalTo(headerNameMapKeyNull2))
    }

    @Test
    void "equals properly compares null and non-null header names"() throws Exception {
        HeaderName headerNameMapKeyNonNull = HeaderName.wrap(HEADER_NAME)
        HeaderName headerNameMapKeyNull = HeaderName.wrap(null)

        assertThat(headerNameMapKeyNonNull, not(equalTo(headerNameMapKeyNull)))
    }

    @Test
    void "equals properly compares the same object"() throws Exception {
        HeaderName headerNameMapKey = HeaderName.wrap(HEADER_NAME)

        assertThat(headerNameMapKey, equalTo(headerNameMapKey))
    }

    @Test
    void "equals properly compares object of different type"() throws Exception {
        HeaderName headerNameMapKey = HeaderName.wrap(HEADER_NAME)
        String notAHeaderName = ""

        assertThat(headerNameMapKey, not(equalTo(notAHeaderName)))
    }
}

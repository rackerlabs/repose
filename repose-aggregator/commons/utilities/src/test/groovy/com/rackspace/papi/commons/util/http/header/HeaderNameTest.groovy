package com.rackspace.papi.commons.util.http.header
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.core.IsNot.not
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

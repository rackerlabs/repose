package com.rackspace.papi.commons.util.http.header
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.core.IsNot.not
import static org.junit.Assert.assertThat

class HeaderNameMapKeyTest {
    public static final String HEADER_NAME = "SomeName"

    @Test
    void "getName returns original string"() throws Exception {
        HeaderNameMapKey headerNameMapKey = new HeaderNameMapKey(HEADER_NAME)

        assertThat(headerNameMapKey.getName(), equalTo(HEADER_NAME))
    }

    @Test
    void "getName does not change case"() throws Exception {
        HeaderNameMapKey headerNameMapKey = new HeaderNameMapKey(HEADER_NAME)

        assertThat(headerNameMapKey.getName(), not(equalTo(HEADER_NAME.toLowerCase())))
    }

    @Test
    void "equals performs case insensitive comparison"() throws Exception {
        HeaderNameMapKey headerNameMapKey = new HeaderNameMapKey(HEADER_NAME)
        HeaderNameMapKey headerNameMapKeyLower = new HeaderNameMapKey(HEADER_NAME.toLowerCase())

        assertThat(headerNameMapKey, equalTo(headerNameMapKeyLower))
    }

    @Test
    void "generated hashCode is case insensitive"() throws Exception {
        HeaderNameMapKey headerNameMapKey = new HeaderNameMapKey(HEADER_NAME)
        HeaderNameMapKey headerNameMapKeyLower = new HeaderNameMapKey(HEADER_NAME.toLowerCase())

        assertThat(headerNameMapKey.hashCode(), equalTo(headerNameMapKeyLower.hashCode()))
    }
}

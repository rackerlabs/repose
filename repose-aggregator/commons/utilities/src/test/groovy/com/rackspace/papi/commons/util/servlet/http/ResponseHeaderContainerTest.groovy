package com.rackspace.papi.commons.util.servlet.http

import com.rackspace.papi.commons.util.http.header.HeaderNameStringWrapper
import org.junit.Before
import org.junit.Test

import javax.servlet.http.HttpServletResponse

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.hasItems
import static org.hamcrest.MatcherAssert.assertThat
import static org.mockito.Matchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class ResponseHeaderContainerTest {
    public static final List<String> HEADER_NAME_LIST = ["via", "BLAH", "Content-Type"] as List<String>
    public static final List<String> HEADER_VALUE_LIST = [] as List<String>

    ResponseHeaderContainer responseHeaderContainer

    @Before
    void setup() {
        HttpServletResponse response = mock(HttpServletResponse.class)

        when(response.getHeaderNames()).thenReturn(HEADER_NAME_LIST)
        when(response.getHeaders(any(String.class))).thenReturn(HEADER_VALUE_LIST)

        responseHeaderContainer = new ResponseHeaderContainer(response)
    }

    @Test
    void "header names are not modified"() throws Exception {
        assertThat(responseHeaderContainer.getHeaderNames().size(), equalTo(3))
        assertThat(responseHeaderContainer.getHeaderNames(), hasItems(
                new HeaderNameStringWrapper("via"),
                new HeaderNameStringWrapper("BLAH"),
                new HeaderNameStringWrapper("Content-Type")))
    }

    //todo: write tests to cover the rest of the class
}

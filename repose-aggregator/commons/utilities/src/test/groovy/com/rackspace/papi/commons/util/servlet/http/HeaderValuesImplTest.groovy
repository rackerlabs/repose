package com.rackspace.papi.commons.util.servlet.http
import com.rackspace.papi.commons.util.http.header.HeaderNameStringWrapper
import com.rackspace.papi.commons.util.http.header.HeaderValue
import org.junit.Before
import org.junit.Test

import javax.servlet.http.HttpServletRequest

import static junit.framework.Assert.assertNull
import static junit.framework.TestCase.assertFalse
import static junit.framework.TestCase.assertTrue
import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.CoreMatchers.hasItems
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.core.IsEqual.equalTo
import static org.mockito.Matchers.anyString
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class HeaderValuesImplTest {
    Map<HeaderNameStringWrapper, List<HeaderValue>> headers

    HeaderValues headerValues

    @Before
    void setup() {
        HttpServletRequest request = mock(HttpServletRequest.class)

        headers = new HashMap<>();

        when(request.getAttribute(anyString())).thenReturn(headers)
        when(request.getHeaders(eq("Accept"))).thenReturn(Collections.enumeration(Arrays.asList("text/plain")))
        when(request.getHeaders(eq("User-Agent"))).thenReturn(Collections.enumeration(Arrays.asList("custom")))
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList("Accept", "User-Agent")))

        headerValues = HeaderValuesImpl.extract(request)
    }

    @Test
    void "getHeader returns header value if header is present"() throws Exception {
        String acceptValue = headerValues.getHeader("Accept")

        assertThat(acceptValue, equalTo("text/plain"))
    }

    @Test
    void "getHeader returns header value if header is present (case insensitive)"() throws Exception {
        String acceptValue = headerValues.getHeader("AcCePt")

        assertThat(acceptValue, equalTo("text/plain"))
    }

    @Test
    void "getHeader returns null if header is not present"() throws Exception {
        String acceptValue = headerValues.getHeader("not-a-header")

        assertNull(acceptValue)
    }

    @Test
    void "getHeaderValue returns the first value if present"() throws Exception {
        String headerValue = headerValues.getHeaderValue("Accept")

        assertThat(headerValue, equalTo("text/plain"))
    }

    @Test
    void "getHeaderValue returns the first value if present (case insensitive)"() throws Exception {
        String headerValue = headerValues.getHeaderValue("AcCePt")

        assertThat(headerValue, equalTo("text/plain"))
    }

    @Test
    void "getHeaderValue returns null if not present"() throws Exception {
        String headerValue = headerValues.getHeaderValue("not-a-header")

        assertNull(headerValue)
    }

    @Test
    void "getHeaderNames returns an enumeration of header names"() throws Exception {
        Enumeration<String> headerNames = headerValues.getHeaderNames()

        assertThat(Collections.list(headerNames), hasItems("Accept", "User-Agent"))
    }

    @Test
    void "getHeaders returns an enumeration of all header values if the header is present"() throws Exception {
        List<String> acceptHeaders = Collections.list(headerValues.getHeaders("Accept"))

        assertThat(acceptHeaders.size(), equalTo(1))
        assertThat(acceptHeaders, hasItem("text/plain"))
    }

    @Test
    void "getHeaders returns an empty enumeration if header is not present"() throws Exception {
        List<String> acceptHeaders = Collections.list(headerValues.getHeaders("not-a-header"))

        assertTrue(acceptHeaders.isEmpty())
    }

    @Test
    void "getPreferredHeaderValues"() throws Exception {
        //todo
    }

    @Test
    void "getPreferredHeaders"() throws Exception {
        //todo
    }

    @Test
    void "containsHeader returns true if header is present"() throws Exception {
        assertTrue(headerValues.containsHeader("Accept"))
    }

    @Test
    void "containsHeader returns true if header is present (case insensitivity)"() throws Exception {
        assertTrue(headerValues.containsHeader("aCcEpT"))
    }

    @Test
    void "containsHeader returns false if header is not present"() throws Exception {
        assertFalse(headerValues.containsHeader("not-a-header"))
    }

    @Test
    void "getHeaderValues returns the appropriate value if a header is present"() throws Exception {
        List<HeaderValue> acceptHeaderValues = headerValues.getHeaderValues("Accept")

        assertThat(acceptHeaderValues.size(), equalTo(1))
        assertThat(acceptHeaderValues.get(0).getValue(), equalTo("text/plain"))
    }

    @Test
    void "getHeaderValues returns the null if a header is not present"() throws Exception {
        List<HeaderValue> acceptHeaderValues = headerValues.getHeaderValues("not-a-header")

        assertNull(acceptHeaderValues)
    }

    @Test
    void "fromMap returns the first value if present"() throws Exception {
        String headerValue = HeaderValuesImpl.fromMap(headers, "Accept")

        assertThat(headerValue, equalTo("text/plain"))
    }

    @Test
    void "fromMap returns null if not present"() throws Exception {
        String headerValue = HeaderValuesImpl.fromMap(headers, "not-a-header")

        assertNull(headerValue)
    }

    //todo: write tests for public void methods
}

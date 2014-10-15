package org.openrepose.commons.utils.servlet.http

import org.openrepose.commons.utils.http.HttpDate
import org.openrepose.commons.utils.http.header.HeaderName
import org.openrepose.commons.utils.http.header.HeaderValue
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
    Map<HeaderName, List<HeaderValue>> headers

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
        //todo: this is "hard" to test as a unit
    }

    @Test
    void "getPreferredHeaders"() throws Exception {
        //todo: this is "hard" to test as a unit
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

    @Test
    void "addHeader stores a header and associated value(s) so that it can be retrieved"() throws Exception {
        headerValues.addHeader("test-name", "test-value")

        assertThat(headerValues.getHeader("test-name"), equalTo("test-value"))
    }

    @Test
    void "replaceHeader"() throws Exception {
        headerValues.addHeader("test-name", "test-value")

        assertThat(headerValues.getHeader("test-name"), equalTo("test-value"))

        headerValues.replaceHeader("test-name", "new-value")

        assertThat(headerValues.getHeader("test-name"), equalTo("new-value"))
    }

    @Test
    void "removeHeader removes a header and its value(s) so that it cannot be retrieved"() throws Exception {
        headerValues.addHeader("test-name", "test-value")

        assertThat(headerValues.getHeader("test-name"), equalTo("test-value"))

        headerValues.removeHeader("test-name")

        assertNull(headerValues.getHeader("test-name"))
    }

    @Test
    void "clearHeaders removes all headers so that they cannot be retrieved"() throws Exception {
        headerValues.addHeader("test-name", "test-value")

        assertThat(headerValues.getHeader("test-name"), equalTo("test-value"))

        headerValues.clearHeaders()

        assertNull(headerValues.getHeader("test-name"))
    }

    @Test
    void "addDateHeader stores a header with an associated date provided so that is can be retrieved"() throws Exception {
        Date now = new Date()
        long curTime = now.getTime()

        headerValues.addDateHeader("Date", curTime)

        assertThat(headerValues.getHeader("Date"), equalTo(new HttpDate(now).toRFC1123()))
    }

    @Test
    void "replaceDateHeader replaces a given date header with a new value"() throws Exception {
        Date now = new Date()
        long oldTime = now.getTime()

        headerValues.addDateHeader("Date", oldTime)

        assertThat(headerValues.getHeader("Date"), equalTo(new HttpDate(now).toRFC1123()))

        now = new Date()
        long newTime = now.getTime()

        headerValues.replaceDateHeader("Date", newTime)

        assertThat(headerValues.getHeader("Date"), equalTo(new HttpDate(now).toRFC1123()))
    }
}

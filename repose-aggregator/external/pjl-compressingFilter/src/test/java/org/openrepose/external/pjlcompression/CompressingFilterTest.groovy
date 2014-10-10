package org.openrepose.external.pjlcompression

import com.mockrunner.mock.web.MockFilterConfig
import com.mockrunner.mock.web.MockHttpServletRequest
import com.mockrunner.mock.web.MockHttpServletResponse
import com.mockrunner.mock.web.MockServletContext
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.openrepose.external.pjlcompression.CompressingFilter

import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

import static org.hamcrest.CoreMatchers.nullValue
import static org.hamcrest.MatcherAssert.assertThat
import static org.mockito.Matchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify

class CompressingFilterTest {
    private static final String ACCEPT_ENCODING = "Accept-Encoding"
    private static final String CONTENT_ENCODING = "Content-Encoding"

    private CompressingFilter compressingFilter = new CompressingFilter()

    @Before
    public void setup() {
        def filterConfig = new MockFilterConfig()

        filterConfig.setupServletContext(new MockServletContext())

        compressingFilter.init(filterConfig)
        compressingFilter.setForRepose()
    }

    @Test
    public void doFilter_acceptEncodingShouldBeRemovedFromRequest() {
        // Given
        def mockRequest = new MockHttpServletRequest()
        def mockResponse = new MockHttpServletResponse()
        def mockFilterChain = mock(FilterChain)
        def ArgumentCaptor<ServletRequest> capturedRequest = ArgumentCaptor.forClass(ServletRequest)

        mockRequest.addHeader(CONTENT_ENCODING, "identity")
        mockRequest.addHeader(ACCEPT_ENCODING, "identity")

        // When
        compressingFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        // Then
        verify(mockFilterChain).doFilter(capturedRequest.capture(), any(ServletResponse))
        assertThat(((HttpServletRequest) capturedRequest.getValue()).getHeader(ACCEPT_ENCODING), nullValue())
    }
}

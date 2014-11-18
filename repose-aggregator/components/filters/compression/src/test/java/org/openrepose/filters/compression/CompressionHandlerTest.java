package org.openrepose.filters.compression;

import org.openrepose.external.pjlcompression.CompressingFilter;
import org.openrepose.commons.utils.http.HttpStatusCode;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.powerfilter.PowerFilterChain;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.EOFException;
import java.io.IOException;
import java.util.zip.ZipException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CompressingFilter.class)
public class CompressionHandlerTest {
    private CompressingFilter compressingFilter;
    private CompressionHandler compressionHandler;

    private PowerFilterChain filterChain;

    private HttpServletRequest request;
    private ReadableHttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        compressingFilter = mock(CompressingFilter.class);
        compressionHandler = new CompressionHandler(compressingFilter);

        filterChain = mock(PowerFilterChain.class);

        request = mock(HttpServletRequest.class);
        response = mock(ReadableHttpServletResponse.class);
    }

    @Test
    public void handleRequest_nullChain() throws Exception {
        compressionHandler.setFilterChain(null);

        FilterDirector director = compressionHandler.handleRequest(request, response);

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue(), director.getResponseStatusCode());
        assertThat(director.getFilterAction(), equalTo(FilterAction.RETURN));
    }

    @Test
    public void handleRequest_validRequest() throws Exception {
        compressionHandler.setFilterChain(filterChain);
        doNothing().when(compressingFilter, "doFilter", request, response, filterChain);
        when(response.getStatus()).thenReturn(200);

        FilterDirector director = compressionHandler.handleRequest(request, response);

        assertEquals(HttpStatusCode.OK.intValue(), director.getResponseStatusCode());
        assertThat(director.getFilterAction(), equalTo(FilterAction.RETURN));
    }

    @Test
    public void handleRequest_handleGZIPError() throws Exception {
        compressionHandler.setFilterChain(filterChain);
        doThrow(new ZipException("Not in GZIP format"))
                .when(compressingFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        FilterDirector director = compressionHandler.handleRequest(request, response);

        assertEquals(HttpStatusCode.BAD_REQUEST.intValue(), director.getResponseStatusCode());
        assertThat(director.getFilterAction(), equalTo(FilterAction.RETURN));
    }

    @Test
    public void handleRequest_handleEOFError() throws Exception {
        compressionHandler.setFilterChain(filterChain);
        doThrow(new EOFException()).when(compressingFilter)
                .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        FilterDirector director = compressionHandler.handleRequest(request, response);

        assertEquals(HttpStatusCode.BAD_REQUEST.intValue(), director.getResponseStatusCode());
        assertThat(director.getFilterAction(), equalTo(FilterAction.RETURN));
    }

    @Test
    public void handleRequest_handleGenericIOError() throws Exception {
        compressionHandler.setFilterChain(filterChain);
        doThrow(new IOException()).when(compressingFilter)
                .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        FilterDirector director = compressionHandler.handleRequest(request, response);

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue(), director.getResponseStatusCode());
        assertThat(director.getFilterAction(), equalTo(FilterAction.RETURN));
    }

    @Test
    public void handleRequest_handleServletError() throws Exception {
        compressionHandler.setFilterChain(filterChain);
        doThrow(new ServletException()).when(compressingFilter)
                .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        FilterDirector director = compressionHandler.handleRequest(request, response);

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue(), director.getResponseStatusCode());
        assertThat(director.getFilterAction(), equalTo(FilterAction.RETURN));
    }
}

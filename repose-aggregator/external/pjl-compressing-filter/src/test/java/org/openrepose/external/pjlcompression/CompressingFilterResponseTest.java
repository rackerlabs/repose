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
/*
 * Copyright 2004 and onwards Sean Owen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openrepose.external.pjlcompression;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests {@link CompressingFilter} compressing responses.
 *
 * @author Sean Owen
 */
public final class CompressingFilterResponseTest {

    static final String SMALL_DOCUMENT = "Test";
    static final String BIG_DOCUMENT;
    private static final String TEST_ENCODING = "ISO-8859-1";
    private static final String EMPTY = "";

    static {
        // Make up a random, but repeatable String
        Random r = new Random(0xDEADBEEFL);
        byte[] bytes = new byte[10000];
        r.nextBytes(bytes);
        String temp = null;
        try {
            temp = new String(bytes, TEST_ENCODING);
        } catch (UnsupportedEncodingException uee) {
            // can't happen
        }
        BIG_DOCUMENT = temp;
    }

    private MockFilterConfig filterConfig;
    private CompressingFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    private static byte[] getCompressedOutput(byte[] output) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream gzipOut = new GZIPOutputStream(baos);
        gzipOut.write(output);
        gzipOut.finish();
        gzipOut.close();
        return baos.toByteArray();
    }

    @Before
    public void setUp() throws Exception {
        filterConfig = new MockFilterConfig();
        filterConfig.addInitParameter("debug", "true");
        filterConfig.addInitParameter("statsEnabled", "true");
        filterConfig.addInitParameter("excludePathPatterns", ".*badpath.*,whocares");
        filterConfig.addInitParameter("excludeContentTypes", "text/badtype,whatever");
        filterConfig.addInitParameter("excludeUserAgentPatterns", "Nokia.*");
        filter = new CompressingFilter();
        filter.init(filterConfig);
        request = new MockHttpServletRequest();
        request.setMethod("GET");
        response = spy(new MockHttpServletResponse());
    }

    @Test
    public void testSmallOutput() throws Exception {
        verifyOutput(SMALL_DOCUMENT, false);
    }

    @Test
    public void testBigOutput() throws Exception {
        verifyOutput(BIG_DOCUMENT, true);

        CompressingFilterStats stats = (CompressingFilterStats)
                filterConfig.getServletContext().getAttribute(CompressingFilterStats.STATS_KEY);
        assertNotNull(stats);

        assertEquals(0, stats.getNumRequestsCompressed());
        assertEquals(1, stats.getTotalRequestsNotCompressed());
        assertEquals(0.0, stats.getRequestAverageCompressionRatio(), 0.0001);
        assertEquals(0L, stats.getRequestCompressedBytes());
        assertEquals(0L, stats.getRequestInputBytes());

        assertEquals(1, stats.getNumResponsesCompressed());
        assertEquals(0, stats.getTotalResponsesNotCompressed());
        assertEquals(0.9977, stats.getResponseAverageCompressionRatio(), 0.0001);
        assertEquals(10023L, stats.getResponseCompressedBytes());
        assertEquals(10000L, stats.getResponseInputBytes());
    }

    @Test
    public void testAlreadyApplied() throws Exception {
        // add the filter again
        verifyOutput(BIG_DOCUMENT, true);
    }

    @Test
    public void testForceEncoding() throws Exception {
        // force no-compression compression for a big response
        request.setAttribute(CompressingFilter.FORCE_ENCODING_KEY, "identity");
        verifyOutput(BIG_DOCUMENT, false);
    }

    @Test
    public void testNoTransform() throws Exception {
        filterChain = new MockFilterChain(new HttpServlet() {
            @Override
            public void doGet(HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
                response.setHeader("Cache-Control", "no-transform");
                response.getWriter().print(BIG_DOCUMENT);
            }
        });
        verifyOutput(false, BIG_DOCUMENT, false);
    }

    @Test
    public void testExcludePathPatterns1() throws Exception {
        request.setRequestURI("/some/goodpath/index.html");
        verifyOutput(BIG_DOCUMENT, true);
    }

    @Test
    public void testExcludePathPatterns2() throws Exception {
        request.setRequestURI("/some/badpath/index.html");
        verifyOutput(BIG_DOCUMENT, false);
    }

    @Test
    public void testExcludeUserAgentPatterns1() throws Exception {
        request.addHeader("User-Agent", "MSIE5");
        verifyOutput(BIG_DOCUMENT, true);
    }

    @Test
    public void testExcludeUserAgentPatterns2() throws Exception {
        request.addHeader("User-Agent", "Nokia6820");
        verifyOutput(BIG_DOCUMENT, false);
    }

    @Test
    public void testExcludeContentTypes1() throws Exception {
        filterChain = new MockFilterChain(new HttpServlet() {
            @Override
            public void doGet(HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
                response.setContentType("text/badtype; otherstuff");
                response.getWriter().print(BIG_DOCUMENT);
            }
        });
        verifyOutput(false, BIG_DOCUMENT, false);
    }

    @Test
    public void testExcludeContentTypes2() throws Exception {
        filterChain = new MockFilterChain(new HttpServlet() {
            @Override
            public void doGet(HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
                response.setContentType("text/goodtype; otherstuff");
                response.getWriter().print(BIG_DOCUMENT);
            }
        });
        verifyOutput(false, BIG_DOCUMENT, true);
    }

    @Test
    public void testRedirect() throws Exception {
        String redirectLocation = "http://www.google.com/";
        filterChain = new MockFilterChain(new HttpServlet() {
            @Override
            public void doGet(HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
                response.sendRedirect(redirectLocation);
            }
        });

        filter.doFilter(request, response, filterChain);

        assertEquals(302, response.getStatus());
        assertEquals(redirectLocation, response.getRedirectedUrl());
        verify(response, never()).sendError(anyInt());
        verify(response, never()).sendError(anyInt(), anyString());
        assertTrue(response.containsHeader(CompressingFilter.VARY_HEADER));
    }

    @Test
    public void testFlush() throws IOException, ServletException {
        filterChain = new MockFilterChain(new HttpServlet() {
            @Override
            public void doGet(HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
                response.getWriter().print(SMALL_DOCUMENT);
                response.flushBuffer();
                response.getWriter().print(SMALL_DOCUMENT);
            }
        });

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertNull(response.getRedirectedUrl());
        verify(response, never()).sendError(anyInt());
        verify(response, never()).sendError(anyInt(), anyString());
        assertTrue(response.containsHeader(CompressingFilter.VARY_HEADER));
    }

    @Test
    public void testClose() throws IOException, ServletException {
        filterChain = new MockFilterChain(new HttpServlet() {
            @Override
            public void doGet(HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
                response.getWriter().print(SMALL_DOCUMENT);
                response.getWriter().close();
            }
        });

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertNull(response.getRedirectedUrl());
        verify(response, never()).sendError(anyInt());
        verify(response, never()).sendError(anyInt(), anyString());
        assertEquals(SMALL_DOCUMENT, response.getContentAsString());
        assertNull(request.getAttribute(CompressingFilter.COMPRESSED_KEY));
        assertTrue(response.containsHeader(CompressingFilter.VARY_HEADER));
    }

    @Test
    public void testSpuriousFlushClose() throws IOException, ServletException {
        filterChain = new MockFilterChain(new HttpServlet() {
            @Override
            public void doGet(HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
                response.getWriter().print(SMALL_DOCUMENT);
                response.getWriter().close();
                response.getWriter().flush();
                response.getWriter().close();
            }
        });

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertNull(response.getRedirectedUrl());
        verify(response, never()).sendError(anyInt());
        verify(response, never()).sendError(anyInt(), anyString());
        assertEquals(SMALL_DOCUMENT, response.getContentAsString());
        assertNull(request.getAttribute(CompressingFilter.COMPRESSED_KEY));
        assertTrue(response.containsHeader(CompressingFilter.VARY_HEADER));
    }

    @Test
    public void testNoGzipOutput() throws IOException, ServletException {
        doTestNoOutput();
    }

    @Test
    public void testNoZipOutput() throws IOException, ServletException {
        request.addHeader("Content-Encoding", "compress");
        doTestNoOutput();
    }

    private void doTestNoOutput() throws IOException, ServletException {
        filterChain = new MockFilterChain(new HttpServlet() {
            @Override
            public void doGet(HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
                response.getWriter().close();
            }
        });

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertNull(response.getRedirectedUrl());
        verify(response, never()).sendError(anyInt());
        verify(response, never()).sendError(anyInt(), anyString());
        assertEquals(EMPTY, response.getContentAsString());
        assertNull(request.getAttribute(CompressingFilter.COMPRESSED_KEY));

        assertFalse(response.containsHeader("Content-Encoding"));
        assertFalse(response.containsHeader("X-Compressed-By"));
        assertTrue(response.containsHeader(CompressingFilter.VARY_HEADER));
    }

    private void verifyOutput(final String output, boolean shouldCompress) throws IOException, ServletException {
        verifyOutput(true, output, shouldCompress);
    }

    private void verifyOutput(boolean initFilterChain, final String output, boolean shouldCompress) throws IOException, ServletException {
        if (initFilterChain) {
            filterChain = new MockFilterChain(new HttpServlet() {
                @Override
                public void doGet(HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {
                    response.setHeader("ETag", String.valueOf(output.hashCode())); // Fake ETag
                    response.getWriter().print(output);
                }
            });
        }
        request.addHeader("Accept-Encoding", "deflate,gzip");

        filter.doFilter(request, response, filterChain);


        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertNull(response.getRedirectedUrl());
        verify(response, never()).sendError(anyInt());
        verify(response, never()).sendError(anyInt(), anyString());

        if (shouldCompress) {
            assertTrue(response.containsHeader("Vary"));
            byte[] outputBytes = output.getBytes(TEST_ENCODING);
            byte[] expectedBytes = getCompressedOutput(outputBytes);
            byte[] moduleOutput = response.getContentAsByteArray();
            assertFalse(Arrays.equals(outputBytes, moduleOutput));
            assertArrayEquals(expectedBytes, moduleOutput);
            assertEquals(Boolean.TRUE, request.getAttribute(CompressingFilter.COMPRESSED_KEY));

            assertTrue(response.containsHeader("Content-Encoding"));
            assertTrue(response.containsHeader("X-Compressed-By"));
            assertTrue(!response.containsHeader("ETag") || response.getHeader("ETag").endsWith("-gzip"));
        } else {
            assertEquals(output, response.getContentAsString());
            assertNull(request.getAttribute(CompressingFilter.COMPRESSED_KEY));
        }
    }
}

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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests {@link CompressingFilter} compressed requests.
 *
 * @author Sean Owen
 * @since 1.6
 */
public final class CompressingFilterRequestTest {

    private static final byte[] BIG_DOCUMENT;

    static {
        // Make up a random, but repeatable String
        Random r = new Random(0xDEADBEEFL);
        BIG_DOCUMENT = new byte[10000];
        r.nextBytes(BIG_DOCUMENT);
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
        baos.close();
        return baos.toByteArray();
    }

    @Before
    public void setUp() throws Exception {
        filterConfig = new MockFilterConfig();
        filterConfig.addInitParameter("debug", "true");
        filterConfig.addInitParameter("statsEnabled", "true");
        filter = new CompressingFilter();
        filter.init(filterConfig);
        request = new MockHttpServletRequest();
        request.setMethod("GET");
        response = spy(new MockHttpServletResponse());
    }

    @Test
    public void testBigOutput() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
        filterChain = new MockFilterChain(new HttpServlet() {
                @Override
                public void doGet(HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {
                    InputStream sis = request.getInputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = sis.read(buffer)) > 0) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    baos.close();
                }
            });
        request.addHeader("Content-Encoding", "gzip");
        byte[] compressedBigDoc = getCompressedOutput(BIG_DOCUMENT);
        request.setContent(compressedBigDoc);

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertNull(response.getRedirectedUrl());
        verify(response, never()).sendError(anyInt());
        verify(response, never()).sendError(anyInt(), anyString());

        assertTrue(Arrays.equals(BIG_DOCUMENT, baos.toByteArray()));

        CompressingFilterStats stats = (CompressingFilterStats)
                filterConfig.getServletContext().getAttribute(CompressingFilterStats.STATS_KEY);
        assertNotNull(stats);

        assertEquals(1, stats.getNumRequestsCompressed());
        assertEquals(0, stats.getTotalRequestsNotCompressed());
        assertEquals((double) BIG_DOCUMENT.length / (double) compressedBigDoc.length, stats.getRequestAverageCompressionRatio(), 0.0001);
        assertEquals((long) compressedBigDoc.length, stats.getRequestCompressedBytes());
        assertEquals((long) BIG_DOCUMENT.length, stats.getRequestInputBytes());

        assertEquals(0, stats.getNumResponsesCompressed());
        assertEquals(1, stats.getTotalResponsesNotCompressed());
        assertEquals(0.0, stats.getResponseAverageCompressionRatio(), 0.0001);
        assertEquals(0L, stats.getResponseCompressedBytes());
        assertEquals(0L, stats.getResponseInputBytes());
    }
}

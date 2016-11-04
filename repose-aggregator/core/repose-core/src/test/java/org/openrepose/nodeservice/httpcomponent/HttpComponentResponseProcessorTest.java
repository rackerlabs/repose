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
package org.openrepose.nodeservice.httpcomponent;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.verification.VerificationMode;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

public class HttpComponentResponseProcessorTest {

    private HttpResponse response;
    private HttpServletResponse servletResponse;
    private HttpComponentResponseProcessor processor;
    private Map<String, String> headerValues;
    private List<Header> headers;
    private HttpEntity entity;
    private ServletOutputStream out;

    private static List<Header> mockHeaders(Map<String, String> headerValues) {
        final List<Header> headers = new ArrayList<>();
        for (String header : headerValues.keySet()) {
            String values = headerValues.get(header);
            for (String value : values.split(",")) {
                Header header1 = mock(Header.class);
                when(header1.getName()).thenReturn(header);
                when(header1.getValue()).thenReturn(value);
                headers.add(header1);
            }
        }
        return headers;
    }

    @Before
    public void setUp() throws IOException {
        headerValues = new HashMap<>();
        headerValues.put("Header1", "Value1");
        headerValues.put("Header2", "Value21,Value22");
        headerValues.put("Header3", "Value3;q=3");

        headers = HttpComponentResponseProcessorTest.mockHeaders(headerValues);
        entity = mock(HttpEntity.class);
        out = mock(ServletOutputStream.class);
        response = mock(HttpResponse.class);
        servletResponse = mock(HttpServletResponse.class);
        when(response.getAllHeaders()).thenReturn(headers.toArray(new Header[headers.size()]));
        when(response.getEntity()).thenReturn(entity);
        when(servletResponse.getOutputStream()).thenReturn(out);

        processor = new HttpComponentResponseProcessor(response, servletResponse, 200);
    }

    @Test
    public void shouldSetStatus() throws IOException {
        processor.process();
        verify(servletResponse).setStatus(eq(200));
    }

    @Test
    public void shouldSetHeaders() throws IOException {
        processor.process();

        verify(servletResponse).setStatus(eq(200));
        for (Header header : headers) {
            verify(servletResponse).addHeader(header.getName(), header.getValue());
        }
    }

    @Test
    public void shouldWriteResponse() throws IOException {
        processor.process();

        verify(servletResponse).setStatus(eq(200));
        for (Header header : headers) {
            verify(servletResponse).addHeader(header.getName(), header.getValue());
        }

        verify(servletResponse).getOutputStream();
        verify(servletResponse).getOutputStream();
        verify(out).flush();
    }

    @Test
    public void shouldNotWriteExcluded() throws IOException {
        headerValues.put("Connection", "Should be excluded.");
        headerValues.put("Transfer-Encoding", "Should be excluded.");
        headerValues.put("Server", "Should be excluded.");
        headers = HttpComponentResponseProcessorTest.mockHeaders(headerValues);

        processor.process();

        verify(servletResponse).setStatus(eq(200));
        for (Header header : headers) {
            VerificationMode verificationMode = times(header.getValue().contains("excluded") ? 0 : 1);
            verify(servletResponse, verificationMode).addHeader(header.getName(), header.getValue());
        }
    }
}

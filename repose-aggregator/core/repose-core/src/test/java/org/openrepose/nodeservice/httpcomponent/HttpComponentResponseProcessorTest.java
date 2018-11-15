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
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.*;

public class HttpComponentResponseProcessorTest {

    private HttpResponse clientResponse;
    private HttpServletResponse servletResponse;
    private HttpComponentResponseProcessor processor;

    @Before
    public void setUp() {
        clientResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null);
        servletResponse = mock(HttpServletResponse.class);
        processor = new HttpComponentResponseProcessor(clientResponse, servletResponse);
    }

    @Test
    public void shouldSetStatusCodeAndReasonPhrase() throws IOException {
        String reasonPhrase = "I'm definitely not a teapot";
        clientResponse.setStatusLine(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, reasonPhrase));

        processor.process();

        verify(servletResponse).setStatus(HttpStatus.SC_OK, reasonPhrase);
    }

    @Test
    public void shouldSetHeaders() throws IOException {
        List<Header> headers = new LinkedList<>();
        headers.add(new BasicHeader("Header1", "Value1"));
        headers.add(new BasicHeader("Header2", "Value21,Value22"));
        headers.add(new BasicHeader("Header2", "Value23"));
        headers.add(new BasicHeader("Header3", "Value3;q=3"));
        headers.forEach(clientResponse::addHeader);

        processor.process();

        for (Header header : headers) {
            verify(servletResponse).addHeader(header.getName(), header.getValue());
        }
    }

    @Test
    public void shouldNotSetExcludedHeaders() throws IOException {
        List<Header> headers = new LinkedList<>();
        headers.add(new BasicHeader("Connection", "Should be excluded."));
        headers.add(new BasicHeader("Transfer-Encoding", "Should be excluded."));
        headers.add(new BasicHeader("Server", "Should be excluded."));
        headers.forEach(clientResponse::addHeader);

        processor.process();

        for (Header header : headers) {
            verify(servletResponse, never()).addHeader(header.getName(), header.getValue());
        }
    }

    @Test
    public void shouldWriteResponseBody() throws IOException {
        String content = "some content";
        ServletOutputStream servletOut = mock(ServletOutputStream.class);
        when(servletResponse.getOutputStream()).thenReturn(servletOut);
        clientResponse.setEntity(EntityBuilder.create()
            .setText(content)
            .build());

        processor.process();

        verify(servletResponse).getOutputStream();
        verify(servletOut).write(content.getBytes());
    }

    @Test
    public void shouldClearResponseBodyOn304() throws IOException {
        String content = "some content";
        when(servletResponse.getStatus()).thenReturn(HttpStatus.SC_NOT_MODIFIED);
        clientResponse.setEntity(EntityBuilder.create()
            .setText(content)
            .build());

        processor.process();

        verify(servletResponse).resetBuffer();
        verify(servletResponse).setContentLength(0);
        verify(servletResponse, never()).getOutputStream();
    }
}

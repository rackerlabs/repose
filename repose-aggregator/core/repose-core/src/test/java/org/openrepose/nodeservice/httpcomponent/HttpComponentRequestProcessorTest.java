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

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.*;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.core.systemmodel.config.ChunkedEncoding;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.http.HttpHeaders.TRANSFER_ENCODING;
import static org.apache.http.protocol.HTTP.CHUNK_CODING;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class HttpComponentRequestProcessorTest {
    private MockHttpServletRequest request;
    private String[] values1 = {"value11, value12"};
    private String[] values2 = {"value21", "value22"};

    @Before
    public void setUp() {
        request = new MockHttpServletRequest(HttpGet.METHOD_NAME, "/test");

        Arrays.stream(values1).forEach(value -> request.addHeader("header1", value));
        Arrays.stream(values2).forEach(value -> request.addHeader("header2", value));
    }

    @Test
    public void shouldSetMethod() throws IOException, URISyntaxException {
        HttpUriRequest clientRequest = HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org:8080"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.TRUE);

        assertThat(clientRequest.getMethod(), equalTo(request.getMethod()));
    }

    @Test
    public void shouldSetUriFromTargetNotRequest() throws IOException, URISyntaxException {
        String targetPath = "/foo";
        String requestPath = "/bar/baz";
        request.setRequestURI(requestPath);

        HttpUriRequest clientRequest = HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org:8080" + targetPath),
            true,
            Collections.emptyList(),
            ChunkedEncoding.TRUE);

        assertThat(clientRequest.getURI().getPath(), equalTo(targetPath));
        assertThat(clientRequest.getURI().getPath(), not(containsString(requestPath)));
    }

    @Test
    public void shouldSetQueryString() throws IOException, URISyntaxException {
        String query = "param1%5B%5D=value1&param2=value21&param2=value22";
        request.setQueryString(query);

        HttpUriRequest clientRequest = HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org:8080"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.TRUE);

        assertThat(clientRequest.getURI().toString(), allOf(containsString("param1%5B%5D=value1"), containsString("param2=value21"), containsString("param2=value22")));
    }

    @Test
    public void shouldSetHeaders() throws IOException, URISyntaxException {
        HttpUriRequest clientRequest = HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org:8080"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.TRUE);

        List<String> header1Values = Arrays.stream(clientRequest.getHeaders("header1")).map(NameValuePair::getValue).collect(Collectors.toList());
        List<String> header2Values = Arrays.stream(clientRequest.getHeaders("header2")).map(NameValuePair::getValue).collect(Collectors.toList());

        assertThat(header1Values, contains(values1));
        assertThat(header2Values, contains(values2));
    }

    @Test
    public void shouldEncodeConfiguredHeaders() throws Exception {
        request.addHeader("Header1", "バナナ");
        HttpUriRequest clientRequest = HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org:8080"),
            true,
            Collections.singletonList("header1"),
            ChunkedEncoding.TRUE);

        List<String> header1Values = Arrays.stream(clientRequest.getHeaders("header1")).map(NameValuePair::getValue).collect(Collectors.toList());
        List<String> header2Values = Arrays.stream(clientRequest.getHeaders("header2")).map(NameValuePair::getValue).collect(Collectors.toList());

        assertThat(header1Values, hasItem("%E3%83%90%E3%83%8A%E3%83%8A"));
        assertThat(header2Values, contains(values2));
    }

    @Test
    public void shouldRewriteHostHeader() throws IOException, URISyntaxException {
        request.addHeader("Host", "example.com");

        HttpUriRequest clientRequest = HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.TRUE);

        assertThat(clientRequest.getFirstHeader("Host").getValue(), equalTo("www.openrepose.org"));
    }

    @Test
    public void shouldRewriteHostHeaderWithPort() throws IOException, URISyntaxException {
        request.addHeader("Host", "example.com");

        HttpUriRequest clientRequest = HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org:8080"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.TRUE);

        assertThat(clientRequest.getFirstHeader("Host").getValue(), equalTo("www.openrepose.org:8080"));
    }

    @Test(expected = ClassCastException.class)
    public void shouldNotSetRequestBodyOnGet() throws IOException, URISyntaxException {
        String servletRequestContent = "Hello world!";
        request.setMethod(HttpGet.METHOD_NAME);
        request.setContent(servletRequestContent.getBytes(StandardCharsets.ISO_8859_1));

        HttpEntityEnclosingRequest clientRequest = (HttpEntityEnclosingRequest) HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org:8080"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.TRUE);
    }

    @Test(expected = ClassCastException.class)
    public void shouldNotSetRequestBodyOnHead() throws IOException, URISyntaxException {
        String servletRequestContent = "Hello world!";
        request.setMethod(HttpHead.METHOD_NAME);
        request.setContent(servletRequestContent.getBytes(StandardCharsets.ISO_8859_1));

        HttpEntityEnclosingRequest clientRequest = (HttpEntityEnclosingRequest) HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org:8080"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.TRUE);
    }

    @Test(expected = ClassCastException.class)
    public void shouldNotSetRequestBodyOnOptions() throws IOException, URISyntaxException {
        String servletRequestContent = "Hello world!";
        request.setMethod(HttpOptions.METHOD_NAME);
        request.setContent(servletRequestContent.getBytes(StandardCharsets.ISO_8859_1));

        HttpEntityEnclosingRequest clientRequest = (HttpEntityEnclosingRequest) HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org:8080"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.TRUE);
    }

    @Test(expected = ClassCastException.class)
    public void shouldNotSetRequestBodyOnTrace() throws IOException, URISyntaxException {
        String servletRequestContent = "Hello world!";
        request.setMethod(HttpTrace.METHOD_NAME);
        request.setContent(servletRequestContent.getBytes(StandardCharsets.ISO_8859_1));

        HttpEntityEnclosingRequest clientRequest = (HttpEntityEnclosingRequest) HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org:8080"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.TRUE);
    }

    @Test
    public void shouldSetRequestBody() throws IOException, URISyntaxException {
        String servletRequestContent = "Hello world!";
        request.setMethod(HttpPost.METHOD_NAME);
        request.setContent(servletRequestContent.getBytes(StandardCharsets.ISO_8859_1));

        HttpEntityEnclosingRequest clientRequest = (HttpEntityEnclosingRequest) HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org:8080"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.TRUE);

        String clientRequestContent = EntityUtils.toString(clientRequest.getEntity());
        assertThat(clientRequestContent, equalTo(servletRequestContent));
    }

    @Test
    public void shouldSetUnknownContentLengthIfChunkedIsTrue() throws IOException, URISyntaxException {
        String servletRequestContent = "Hello world!";
        request.setMethod(HttpPost.METHOD_NAME);
        request.setContent(servletRequestContent.getBytes(StandardCharsets.ISO_8859_1));

        HttpEntityEnclosingRequest clientRequest = (HttpEntityEnclosingRequest) HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.TRUE);

        assertThat(clientRequest.getEntity().getContentLength(), lessThan(0L));
    }

    @Test
    public void shouldSetActualContentLengthIfChunkedIsFalse() throws IOException, URISyntaxException {
        String servletRequestContent = "Hello world!";
        request.setMethod(HttpPost.METHOD_NAME);
        request.setContent(servletRequestContent.getBytes(StandardCharsets.ISO_8859_1));

        HttpEntityEnclosingRequest clientRequest = (HttpEntityEnclosingRequest) HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.FALSE);

        assertThat(clientRequest.getEntity().getContentLength(), equalTo((long) servletRequestContent.length()));
    }

    @Test
    public void shouldSetUnknownContentLengthIfChunkedIsAutoAndOriginalRequestWasChunked() throws IOException, URISyntaxException {
        String servletRequestContent = "Hello world!";
        request.setMethod(HttpPost.METHOD_NAME);
        request.setContent(servletRequestContent.getBytes(StandardCharsets.ISO_8859_1));
        request.addHeader(TRANSFER_ENCODING, CHUNK_CODING);

        HttpEntityEnclosingRequest clientRequest = (HttpEntityEnclosingRequest) HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.AUTO);

        assertThat(clientRequest.getEntity().getContentLength(), lessThan(0L));
    }

    @Test
    public void shouldSetActualContentLengthIfChunkedIsAutoAndOriginalRequestWasNotChunked() throws IOException, URISyntaxException {
        String servletRequestContent = "Hello world!";
        request.setMethod(HttpPost.METHOD_NAME);
        request.setContent(servletRequestContent.getBytes(StandardCharsets.ISO_8859_1));

        HttpEntityEnclosingRequest clientRequest = (HttpEntityEnclosingRequest) HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.FALSE);

        assertThat(clientRequest.getEntity().getContentLength(), equalTo((long) servletRequestContent.length()));
    }

    @Test(expected = ClassCastException.class)
    public void shouldNotSetEntityOnDeleteWithoutBody() throws Exception {
        request.setMethod(HttpDelete.METHOD_NAME);
        request.addHeader(TRANSFER_ENCODING, CHUNK_CODING);

        HttpEntityEnclosingRequest clientRequest = (HttpEntityEnclosingRequest) HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.TRUE);
    }

    @Test
    public void shouldSetEntityOnDeleteWithBody() throws Exception {
        String servletRequestContent = "Hello world!";
        request.setMethod(HttpDelete.METHOD_NAME);
        request.setContent(servletRequestContent.getBytes(StandardCharsets.ISO_8859_1));
        request.addHeader(TRANSFER_ENCODING, CHUNK_CODING);

        HttpEntityEnclosingRequest clientRequest = (HttpEntityEnclosingRequest) HttpComponentRequestProcessor.process(
            request,
            URI.create("http://www.openrepose.org"),
            true,
            Collections.emptyList(),
            ChunkedEncoding.TRUE);

        assertThat(clientRequest.getEntity().getContentLength(), lessThan(0L));
    }
}

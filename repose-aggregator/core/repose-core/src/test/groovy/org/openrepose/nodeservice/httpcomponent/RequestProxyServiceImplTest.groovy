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
package org.openrepose.nodeservice.httpcomponent

import org.apache.http.HttpEntity
import org.apache.http.HttpVersion
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPatch
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.execchain.HttpResponseProxy
import org.apache.http.message.BasicHttpResponse
import org.apache.http.protocol.HttpContext
import org.apache.logging.log4j.ThreadContext
import org.mockito.ArgumentCaptor
import org.openrepose.commons.utils.logging.TracingHeaderHelper
import org.openrepose.commons.utils.logging.TracingKey
import org.openrepose.core.services.httpclient.HttpClientService
import org.openrepose.core.services.httpclient.HttpClientServiceClient
import org.openrepose.core.systemmodel.config.SystemModel
import org.openrepose.core.systemmodel.config.TracingHeaderConfig
import spock.lang.Specification

import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.*

class RequestProxyServiceImplTest extends Specification {
    RequestProxyServiceImpl requestProxyService
    HttpClientServiceClient httpClient
    HttpClientService httpClientService

    def setup() {
        httpClient = mock(HttpClientServiceClient)
        httpClientService = mock(HttpClientService)
        when(httpClientService.getDefaultClient()).thenReturn(httpClient)
        when(httpClientService.getClient(anyString())).thenReturn(httpClient)
        requestProxyService = new RequestProxyServiceImpl(httpClientService)
    }

    def "Send a patch request with expected body and headers and return expected response"() {
        given:
        StatusLine statusLine = mock(StatusLine)
        when(statusLine.getStatusCode()).thenReturn(418)
        HttpEntity httpEntity = mock(HttpEntity)
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream([1, 2, 3] as byte[]))
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse)
        when(httpResponse.getStatusLine()).thenReturn(statusLine)
        when(httpResponse.getEntity()).thenReturn(httpEntity)
        ArgumentCaptor<HttpPatch> captor = ArgumentCaptor.forClass(HttpPatch)
        when(httpClient.execute(captor.capture(), any(HttpContext) as HttpContext)).thenReturn(httpResponse)

        when:
        byte[] sentBytes = [4, 5, 6] as byte[]
        def response = requestProxyService.patch("http://www.google.com", "key", ["thing": "other thing"], sentBytes, null)
        def request = captor.getValue()
        byte[] readBytes = new byte[3]
        request.getEntity().getContent().read(readBytes)
        byte[] returnedBytes = new byte[3]
        response.data.read(returnedBytes)

        then:
        request.getMethod() == "PATCH"
        request.getURI().toString() == "http://www.google.com/key"
        request.getHeaders("thing").first().value == "other thing"
        readBytes == sentBytes

        response.status == 418
        returnedBytes == [1, 2, 3] as byte[]
    }

    def "a request includes the x-trans-id header for tracing"() {
        given:
        ThreadContext.put(TracingKey.TRACING_KEY, "LOLOL")
        StatusLine statusLine = mock(StatusLine)
        when(statusLine.getStatusCode()).thenReturn(418)
        HttpEntity httpEntity = mock(HttpEntity)
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream([1, 2, 3] as byte[]))
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse)
        when(httpResponse.getStatusLine()).thenReturn(statusLine)
        when(httpResponse.getEntity()).thenReturn(httpEntity)
        ArgumentCaptor<HttpPatch> captor = ArgumentCaptor.forClass(HttpPatch)
        when(httpClient.execute(captor.capture(), any(HttpContext) as HttpContext)).thenReturn(httpResponse)

        when:
        byte[] sentBytes = [4, 5, 6] as byte[]
        def response = requestProxyService.patch("http://www.google.com", "key", ["thing": "other thing"], sentBytes, null)
        def request = captor.getValue()
        byte[] readBytes = new byte[3]
        request.getEntity().getContent().read(readBytes)
        byte[] returnedBytes = new byte[3]
        response.data.read(returnedBytes)

        then:
        request.getMethod() == "PATCH"
        request.getURI().toString() == "http://www.google.com/key"
        request.getHeaders("thing").first().value == "other thing"
        TracingHeaderHelper.getTraceGuid(request.getHeaders("X-Trans-Id").first().value) == "LOLOL"
        readBytes == sentBytes

        response.status == 418
        returnedBytes == [1, 2, 3] as byte[]
        ThreadContext.clearAll()
    }

    def "a request does not include the x-trans-id header for tracing when disabled"() {
        given:
        SystemModel systemModel = mock(SystemModel)
        TracingHeaderConfig tracingHeader = mock(TracingHeaderConfig)
        when(systemModel.getTracingHeader()).thenReturn(tracingHeader)
        when(tracingHeader.isEnabled()).thenReturn(false)
        StatusLine statusLine = mock(StatusLine)
        when(statusLine.getStatusCode()).thenReturn(418)
        HttpEntity httpEntity = mock(HttpEntity)
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream([1, 2, 3] as byte[]))
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse)
        when(httpResponse.getStatusLine()).thenReturn(statusLine)
        when(httpResponse.getEntity()).thenReturn(httpEntity)
        ArgumentCaptor<HttpPatch> captor = ArgumentCaptor.forClass(HttpPatch)
        when(httpClient.execute(captor.capture(), any(HttpContext) as HttpContext)).thenReturn(httpResponse)

        when:
        byte[] sentBytes = [4, 5, 6] as byte[]
        def response = requestProxyService.patch("http://www.google.com", "key", ["thing": "other thing"], sentBytes, null)
        def request = captor.getValue()
        byte[] readBytes = new byte[3]
        request.getEntity().getContent().read(readBytes)
        byte[] returnedBytes = new byte[3]
        response.data.read(returnedBytes)

        then:
        request.getMethod() == "PATCH"
        request.getURI().toString() == "http://www.google.com/key"
        request.getHeaders("thing").first().value == "other thing"
        request.getHeaders("X-Trans-Id").size() == 0
        readBytes == sentBytes

        response.status == 418
        returnedBytes == [1, 2, 3] as byte[]
        ThreadContext.clearAll()
    }

    def "get(uri, headers, null) will try to use the default connection pool"() {
        given:
        when(httpClient.execute(any(HttpUriRequest), any(HttpContext) as HttpContext))
                .thenReturn(new HttpResponseProxy(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK"), null))

        when:
        requestProxyService.get("http://www.google.com", Collections.emptyMap(), null)

        then:
        verification { verify(httpClientService).getClient(null) }
    }

    def "get(uri, headers, connPoolId) will try to use a given connection pool"() {
        given:
        String testPoolId = "test-pool-id"
        when(httpClient.execute(any(HttpUriRequest), any(HttpContext) as HttpContext))
                .thenReturn(new HttpResponseProxy(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK"), null))
        when:
        requestProxyService.get("http://www.google.com", Collections.emptyMap(), testPoolId)

        then:
        verification { verify(httpClientService).getClient(testPoolId) }
    }

    def "get(uri, extraUri, headers, null) will try to use the default connection pool"() {
        given:
        when(httpClient.execute(any(HttpUriRequest), any(HttpContext) as HttpContext))
                .thenReturn(new HttpResponseProxy(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK"), null))
        when:
        requestProxyService.get("http://www.google.com", "key", Collections.emptyMap(), null)

        then:
        verification { verify(httpClientService).getClient(null) }
    }

    def "get(uri, extraUri, headers, connPoolId) will try to use a given connection pool"() {
        given:
        String testPoolId = "test-pool-id"
        when(httpClient.execute(any(HttpUriRequest), any(HttpContext) as HttpContext))
                .thenReturn(new HttpResponseProxy(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK"), null))

        when:
        requestProxyService.get("http://www.google.com", "key", Collections.emptyMap(), testPoolId)

        then:
        verification { verify(httpClientService).getClient(testPoolId) }
    }

    def "put(uri, headers, body, null) will try to use the default connection pool"() {
        given:
        when(httpClient.execute(any(HttpUriRequest), any(HttpContext) as HttpContext))
                .thenReturn(new HttpResponseProxy(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK"), null))

        when:
        requestProxyService.put("http://www.google.com", Collections.emptyMap(), [] as byte[], null)

        then:
        verification { verify(httpClientService).getClient(null) }
    }

    def "put(uri, headers, body, connPoolId) will try to use a given connection pool"() {
        given:
        String testPoolId = "test-pool-id"
        when(httpClient.execute(any(HttpUriRequest), any(HttpContext) as HttpContext))
                .thenReturn(new HttpResponseProxy(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK"), null))

        when:
        requestProxyService.put("http://www.google.com", Collections.emptyMap(), [] as byte[], testPoolId)

        then:
        verification { verify(httpClientService).getClient(testPoolId) }
    }

    def "put(uri, path, headers, body, null) will try to use the default connection pool"() {
        given:
        when(httpClient.execute(any(HttpUriRequest), any(HttpContext) as HttpContext))
                .thenReturn(new HttpResponseProxy(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK"), null))

        when:
        requestProxyService.put("http://www.google.com", "", Collections.emptyMap(), [] as byte[], null)

        then:
        verification { verify(httpClientService).getClient(null) }
    }

    def "put(uri, path, headers, body, connPoolId) will try to use a given connection pool"() {
        given:
        String testPoolId = "test-pool-id"
        when(httpClient.execute(any(HttpUriRequest), any(HttpContext) as HttpContext))
                .thenReturn(new HttpResponseProxy(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK"), null))

        when:
        requestProxyService.put("http://www.google.com", "", Collections.emptyMap(), [] as byte[], testPoolId)

        then:
        verification { verify(httpClientService).getClient(testPoolId) }
    }

    def "patch(uri, path, headers, body, null) will try to use the default connection pool"() {
        given:
        when(httpClient.execute(any(HttpUriRequest), any(HttpContext) as HttpContext))
                .thenReturn(new HttpResponseProxy(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK"), null))

        when:
        requestProxyService.patch("http://www.google.com", "", Collections.emptyMap(), [] as byte[], null)

        then:
        verification { verify(httpClientService).getClient(null) }
    }

    def "patch(uri, path, headers, body, connPoolId) will try to use a given connection pool"() {
        given:
        String testPoolId = "test-pool-id"
        when(httpClient.execute(any(HttpUriRequest), any(HttpContext) as HttpContext))
                .thenReturn(new HttpResponseProxy(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK"), null))

        when:
        requestProxyService.patch("http://www.google.com", "", Collections.emptyMap(), [] as byte[], testPoolId)

        then:
        verification { verify(httpClientService).getClient(testPoolId) }
    }

    def "delete(uri, path, headers, null) will try to use the default connection pool"() {
        given:
        when(httpClient.execute(any(HttpUriRequest), any(HttpContext) as HttpContext))
                .thenReturn(new HttpResponseProxy(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK"), null))

        when:
        requestProxyService.patch("http://www.google.com", "", Collections.emptyMap(), [] as byte[], null)

        then:
        verification { verify(httpClientService).getClient(null) }
    }

    def "delete(uri, path, headers, connPoolId) will try to use a given connection pool"() {
        given:
        String testPoolId = "test-pool-id"
        when(httpClient.execute(any(HttpUriRequest), any(HttpContext) as HttpContext))
                .thenReturn(new HttpResponseProxy(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK"), null))

        when:
        requestProxyService.delete("http://www.google.com", "", Collections.emptyMap(), testPoolId)

        then:
        verification { verify(httpClientService).getClient(testPoolId) }
    }

    def verification(Closure closure) {
        closure()
        return true
    }
}

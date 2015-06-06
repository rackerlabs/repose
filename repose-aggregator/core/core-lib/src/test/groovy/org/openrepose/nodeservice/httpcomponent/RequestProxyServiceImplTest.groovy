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
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPatch
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.logging.log4j.ThreadContext
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.openrepose.core.logging.TracingKey
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.healthcheck.HealthCheckService
import org.openrepose.core.services.httpclient.ExtendedHttpClient
import org.openrepose.core.services.httpclient.HttpClientResponse
import org.openrepose.core.services.httpclient.HttpClientService
import spock.lang.Specification

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class RequestProxyServiceImplTest extends Specification {
    RequestProxyServiceImpl requestProxyService
    ExtendedHttpClient httpExtendedClient
    CloseableHttpClient httpClient

    def setup() {
        httpExtendedClient = mock(ExtendedHttpClient)
        httpClient = mock(CloseableHttpClient)
        HttpClientResponse httpClientResponse = mock(HttpClientResponse)
        when(httpClientResponse.getExtendedHttpClient()).thenReturn(httpExtendedClient)
        when(httpExtendedClient.getHttpClient()).thenReturn(httpClient)
        HttpClientService httpClientService = mock(HttpClientService)
        when(httpClientService.getClient(Mockito.any(String))).thenReturn(httpClientResponse)
        requestProxyService = new RequestProxyServiceImpl(
                mock(ConfigurationService.class),
                mock(HealthCheckService.class),
                httpClientService,
                "cluster",
                "node")
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
        when(httpClient.execute(captor.capture())).thenReturn(httpResponse)

        when:
        byte[] sentBytes = [4, 5, 6] as byte[]
        def response = requestProxyService.patch("http://www.google.com", "key", ["thing": "other thing"], sentBytes)
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
        when(httpClient.execute(captor.capture())).thenReturn(httpResponse)

        when:
        byte[] sentBytes = [4, 5, 6] as byte[]
        def response = requestProxyService.patch("http://www.google.com", "key", ["thing": "other thing"], sentBytes)
        def request = captor.getValue()
        byte[] readBytes = new byte[3]
        request.getEntity().getContent().read(readBytes)
        byte[] returnedBytes = new byte[3]
        response.data.read(returnedBytes)

        then:
        request.getMethod() == "PATCH"
        request.getURI().toString() == "http://www.google.com/key"
        request.getHeaders("thing").first().value == "other thing"
        request.getHeaders("X-Trans-Id").first().value == "LOLOL"
        readBytes == sentBytes

        response.status == 418
        returnedBytes == [1, 2, 3] as byte[]
        ThreadContext.clearAll()
    }

}

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
package org.openrepose.commons.utils.http

import org.apache.http.Header
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpRequestBase
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.junit.internal.matchers.TypeSafeMatcher
import org.mockito.ArgumentCaptor
import org.openrepose.core.services.httpclient.HttpClientContainer
import org.openrepose.core.services.httpclient.HttpClientService

import javax.ws.rs.core.MediaType

import static org.junit.Assert.assertThat
import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.*

class ServiceClientTest {

    private ServiceClient serviceClient

    private HttpClientService mockClientService
    private HttpClient mockHttpClient
    private HttpClientContainer mockClientResponse
    private ArgumentCaptor<HttpRequestBase> requestCaptor

    @Before
    void setup() {
        HttpResponse mockHttpResponse = mock(HttpResponse)
        StatusLine mockStatusLine = mock(StatusLine)
        mockClientService = mock(HttpClientService)
        mockHttpClient = mock(HttpClient)
        mockClientResponse = mock(HttpClientContainer)
        requestCaptor = ArgumentCaptor.forClass(HttpRequestBase)

        when(mockClientService.getClient(anyString())).thenReturn(mockClientResponse)
        when(mockClientResponse.getHttpClient()).thenReturn(mockHttpClient)
        when(mockHttpClient.execute(requestCaptor.capture())).thenReturn(mockHttpResponse)
        when(mockHttpResponse.getAllHeaders()).thenReturn(null)
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine)
        when(mockStatusLine.getStatusCode()).thenReturn(200)

        serviceClient = new ServiceClient("test-pool", mockClientService)
    }

    @Test
    void "when post is called with headers, then a request should be made with those headers"() {
        Map<String, String> headers = new HashMap<>()
        headers.put("x-foo", "foo")
        headers.put("x-bar", "bar")

        serviceClient.post("http://example.com", headers, "body", MediaType.TEXT_PLAIN_TYPE)

        verify(mockHttpClient).execute(any(HttpRequestBase))
        assertThat(requestCaptor.getValue().getFirstHeader("x-foo"), hasValue("foo"))
        assertThat(requestCaptor.getValue().getFirstHeader("x-bar"), hasValue("bar"))
    }

    Matcher<Header> hasValue(String value) {
        return new TypeSafeMatcher<Header>() {

            @Override
            boolean matchesSafely(Header item) {
                boolean result = false
                if (item != null) {
                    result = item.getValue() == value
                }
                return result
            }

            @Override
            void describeTo(Description description) {
                description.appendText("header should have a value of ").appendValue(value)
            }
        }
    }
}

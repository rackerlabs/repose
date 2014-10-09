package org.openrepose.commons.utils.http

import com.rackspace.papi.service.httpclient.HttpClientResponse
import com.rackspace.papi.service.httpclient.HttpClientService
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpRequestBase
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor

import javax.ws.rs.core.MediaType

import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue
import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.*

class ServiceClientTest {

    private ServiceClient serviceClient

    private HttpClient mockHttpClient
    private ArgumentCaptor<HttpRequestBase> requestCaptor

    @Before
    void setup() {
        HttpClientService mockClientService = mock(HttpClientService)
        HttpClientResponse mockClientResponse = mock(HttpClientResponse)
        HttpResponse mockHttpResponse = mock(HttpResponse)
        StatusLine mockStatusLine = mock(StatusLine)
        mockHttpClient = mock(HttpClient)
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
        assertTrue(requestCaptor.getValue().containsHeader("x-foo"))
        assertTrue(requestCaptor.getValue().containsHeader("x-bar"))
        assertThat(requestCaptor.getValue().getFirstHeader("x-foo").getValue(), equalTo("foo"))
        assertThat(requestCaptor.getValue().getFirstHeader("x-bar").getValue(), equalTo("bar"))
    }
}

package com.rackspace.papi.service.serviceclient.akka

import com.rackspace.papi.commons.util.http.ServiceClient
import com.rackspace.papi.commons.util.http.ServiceClientResponse
import com.rackspace.papi.service.httpclient.HttpClientResponse
import com.rackspace.papi.service.httpclient.HttpClientService
import org.apache.commons.io.IOUtils
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpRequestBase
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor

import javax.ws.rs.core.MediaType

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.assertEquals
import static org.mockito.Matchers.*
import static org.mockito.Mockito.*

class AkkaServiceClientImplTest {

    private static final String AUTH_TOKEN_HEADER = "X-Auth-Token"
    private static final String ACCEPT_HEADER = "Accept"

    private AkkaServiceClientImpl akkaServiceClientImpl
    private String userToken
    private String adminToken
    private String targetHostUri
    ArgumentCaptor<HttpRequestBase> requestCaptor
    HttpClientService httpClientService
    String returnString = "getinput"
    HttpClient httpClient

    @Before
    public void setUp() {
        httpClientService = mock(HttpClientService.class)

        HttpClientResponse httpClientResponse = mock(HttpClientResponse.class)

        when(httpClientService.getMaxConnections(anyString())).thenReturn(20)
        when(httpClientService.getClient(anyString())).thenReturn(httpClientResponse)

        httpClient = mock(HttpClient.class)
        when(httpClientResponse.getHttpClient()).thenReturn(httpClient)

        HttpResponse httpResponse = mock(HttpResponse.class)
        requestCaptor = ArgumentCaptor.forClass(HttpRequestBase.class)
        when(httpClient.execute(requestCaptor.capture())).thenReturn(httpResponse)

        HttpEntity entity = mock(HttpEntity.class)
        when(httpResponse.getEntity()).thenReturn(entity)
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(returnString.getBytes("UTF-8")))

        StatusLine statusLine = mock(StatusLine.class)
        when(statusLine.getStatusCode()).thenReturn(200)
        when(httpResponse.getStatusLine()).thenReturn(statusLine)

        akkaServiceClientImpl = new AkkaServiceClientImpl(httpClientService)
        userToken = "userToken"
        adminToken = "adminToken"
        targetHostUri = "targetHostUri"
    }

    @Test
    public void testValidateToken() {
        final Map<String, String> headers = new HashMap<String, String>()
        ((HashMap<String, String>) headers).put(ACCEPT_HEADER, MediaType.APPLICATION_XML)
        ((HashMap<String, String>) headers).put(AUTH_TOKEN_HEADER, "admin token")
        ServiceClientResponse serviceClientResponse = akkaServiceClientImpl.get(userToken, targetHostUri, headers)
        org.junit.Assert.assertEquals("Should retrieve service client with response", serviceClientResponse.getStatusCode(), 200)
    }

    @Test
    public void testGetAdminToken() {
        akkaServiceClientImpl.post(adminToken, targetHostUri, new HashMap(), "", MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE)
        assertThat(requestCaptor.value.getFirstHeader("Content-Type").value, equalTo(MediaType.APPLICATION_JSON))
        assertThat(requestCaptor.value.getFirstHeader("Accept").value, equalTo(MediaType.APPLICATION_JSON))
    }

    @Test
    public void shouldExpireItemInFutureMap() {
        final Map<String, String> headers = new HashMap<String, String>()
        ((HashMap<String, String>) headers).put(ACCEPT_HEADER, MediaType.APPLICATION_XML)
        ((HashMap<String, String>) headers).put(AUTH_TOKEN_HEADER, "admin token")
        akkaServiceClientImpl.get(userToken, targetHostUri, headers)

        Thread.sleep(500)

        akkaServiceClientImpl.get(userToken, targetHostUri, headers)

        verify(httpClient, times(2)).execute(anyObject())
    }

    @Test
    public void testServiceResponseReusable() {
        final Map<String, String> headers = new HashMap<String, String>()
        ((HashMap<String, String>) headers).put(ACCEPT_HEADER, MediaType.APPLICATION_XML)
        ((HashMap<String, String>) headers).put(AUTH_TOKEN_HEADER, "admin token")
        ServiceClientResponse serviceClientResponse1 = akkaServiceClientImpl.get(userToken, targetHostUri, headers)
        ServiceClientResponse serviceClientResponse2 = akkaServiceClientImpl.get(userToken, targetHostUri, headers)

        StringWriter writer1 = new StringWriter()
        IOUtils.copy(serviceClientResponse1.data, writer1, "UTF-8")
        String returnString1 = writer1.toString()

        StringWriter writer2 = new StringWriter()
        IOUtils.copy(serviceClientResponse2.data, writer2, "UTF-8")
        String returnString2 = writer2.toString()

        assertEquals(returnString1, returnString2)
        assertEquals(returnString, returnString2)
        assertEquals(returnString, returnString1)

    }
}

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
import org.junit.Test

import javax.ws.rs.core.MediaType

import static org.junit.Assert.assertEquals
import static org.mockito.Matchers.*
import static org.mockito.Mockito.*

class AkkaServiceClientImplTest {



    private AkkaServiceClientImpl akkaServiceClientImpl;
    private String tenant;
    private String userToken;
    private String targetHostUri;
    ServiceClientResponse<String> serviceClientResponseGet, serviceClientResponsePost;
    HttpClientService httpClientService;
    ServiceClient serviceClient;
    String returnString = "getinput"
    HttpClient httpClient

    @org.junit.Before
    public void setUp() {


        serviceClientResponseGet = new ServiceClientResponse(200,new ByteArrayInputStream(returnString.getBytes("UTF-8")));
        httpClientService  = mock(HttpClientService.class) ;

        HttpClientResponse httpClientResponse =mock(HttpClientResponse.class)

        when(httpClientService.getMaxConnections(anyString())).thenReturn(20);
        when(httpClientService.getClient(anyString())).thenReturn(httpClientResponse);

        httpClient = mock(HttpClient.class);
        when(httpClientResponse.getHttpClient()).thenReturn(httpClient);

        HttpResponse httpResponse = mock(HttpResponse.class)
        when(httpClient.execute(anyObject())).thenReturn(httpResponse);

        HttpEntity entity = mock(HttpEntity.class)
        when(httpResponse.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(returnString.getBytes("UTF-8")));

        StatusLine statusLine = mock(StatusLine.class)
        when(statusLine.getStatusCode()).thenReturn(200)
        when(httpResponse.getStatusLine()).thenReturn(statusLine);

        serviceClient = mock(ServiceClient.class);
        when(serviceClient.get(anyString(), any(Map.class)))
                .thenReturn(serviceClientResponseGet);
        when(serviceClient.getPoolSize()).thenReturn(100)

        akkaServiceClientImpl = new AkkaServiceClientImpl(httpClientService);
        userToken = "userToken";
        targetHostUri = "targetHostUri";
    }

    @org.junit.Test
    public void testValidateToken() {
        final String AUTH_TOKEN_HEADER = "X-Auth-Token";
        final String ACCEPT_HEADER = "Accept";
        final Map<String, String> headers = new HashMap<String, String>();
        ((HashMap<String, String>) headers).put(ACCEPT_HEADER, MediaType.APPLICATION_XML);
        ((HashMap<String, String>) headers).put(AUTH_TOKEN_HEADER, "admin token");
        ServiceClientResponse serviceClientResponse = akkaServiceClientImpl.get(userToken, targetHostUri,  headers );
        org.junit.Assert.assertEquals("Should retrive service client with response", serviceClientResponse.getStatusCode(), 200);
    }

    @org.junit.Test
    public void shouldExpireItemInFutureMap() {
        final String AUTH_TOKEN_HEADER = "X-Auth-Token";
        final String ACCEPT_HEADER = "Accept";
        final Map<String, String> headers = new HashMap<String, String>();
        ((HashMap<String, String>) headers).put(ACCEPT_HEADER, MediaType.APPLICATION_XML);
        ((HashMap<String, String>) headers).put(AUTH_TOKEN_HEADER, "admin token");
        akkaServiceClientImpl.get(userToken, targetHostUri,  headers );

        Thread.sleep(500)

        akkaServiceClientImpl.get(userToken, targetHostUri,  headers );

        verify(httpClient, times(2)).execute(anyObject())
    }

    @Test
    public void testServiceResponseReusable() {
        final String AUTH_TOKEN_HEADER = "X-Auth-Token";
        final String ACCEPT_HEADER = "Accept";
        final Map<String, String> headers = new HashMap<String, String>();
        ((HashMap<String, String>) headers).put(ACCEPT_HEADER, MediaType.APPLICATION_XML);
        ((HashMap<String, String>) headers).put(AUTH_TOKEN_HEADER, "admin token");
        ServiceClientResponse serviceClientResponse1 = akkaServiceClientImpl.get(userToken, targetHostUri,  headers );
        ServiceClientResponse serviceClientResponse2 = akkaServiceClientImpl.get(userToken, targetHostUri,  headers );

        StringWriter writer1 = new StringWriter();
        IOUtils.copy(serviceClientResponse1.data, writer1, "UTF-8");
        String returnString1 = writer1.toString();

        StringWriter writer2 = new StringWriter()
        IOUtils.copy(serviceClientResponse2.data, writer2, "UTF-8");
        String returnString2 = writer2.toString()

        assertEquals(returnString1, returnString2)
        assertEquals(returnString, returnString2)
        assertEquals(returnString, returnString1)

    }
}

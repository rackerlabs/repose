package com.rackspace.papi.service.authclient.akka
import com.rackspace.papi.commons.util.http.ServiceClient
import com.rackspace.papi.commons.util.http.ServiceClientResponse

import javax.ws.rs.core.MediaType

import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class AkkaAuthenticationClientImplTest {



    private AkkaAuthenticationClientImpl akkaAuthenticationClientImpl;
    private String tenant;
    private String userToken;
    private String targetHostUri;
    ServiceClientResponse<String> serviceClientResponseGet, serviceClientResponsePost;
    ServiceClient serviceClient;

    @org.junit.Before
    public void setUp() {


        serviceClientResponseGet = new ServiceClientResponse(500,new ByteArrayInputStream("getinput".getBytes("UTF-8")));

        serviceClient = mock(ServiceClient.class);
        when(serviceClient.get(anyString(), any(Map.class)))
                .thenReturn(serviceClientResponseGet);

        //serviceClientResponseGet = mock(ServiceClientResponse.class);
        //when(serviceClientResponseGet.getStatusCode()).thenReturn(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue());


        akkaAuthenticationClientImpl = new AkkaAuthenticationClientImpl(serviceClient);
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
        ServiceClientResponse serviceClientResponse = akkaAuthenticationClientImpl.validateToken(userToken, targetHostUri,  headers );
        org.junit.Assert.assertEquals("Should retrive service client with response", serviceClientResponse.getStatusCode(), 200);
    }


}

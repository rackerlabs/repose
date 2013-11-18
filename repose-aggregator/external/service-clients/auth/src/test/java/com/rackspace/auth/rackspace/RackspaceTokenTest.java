/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.auth.rackspace;

import com.rackspace.auth.AuthServiceException;
import com.rackspace.auth.ResponseUnmarshaller;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspacecloud.docs.auth.api.v1.FullToken;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author kush5342
 */
public class RackspaceTokenTest {
     private final String inputUser = "345897";
     private final String inputType = "MOSSO";
     private final String userId = "usertest1";
     private ExtractorResult<String> result = new ExtractorResult<String>(inputUser, inputType);
     private final String inputToken = "aaaaa-aaaa-ddd-aaa-aaaa";
     private ServiceClientResponse serviceClientResponse;
     private final String authEndpoint = "https://n01.endpoint.auth.rackspacecloud.com/v2.0";
     private final Map<String, String> headers = new HashMap<String, String>();
     private ServiceClient serviceClient;
     private ResponseUnmarshaller responseUnmarshaller;
     private RackspaceToken rackspaceToken;
     private FullToken tokenResponse;
    
    
    @Before
    public void setUp() throws JAXBException{
        
            serviceClient = mock(ServiceClient.class);
            serviceClientResponse = mock(ServiceClientResponse.class);

            JAXBContext jaxbContext = JAXBContext.newInstance(com.rackspacecloud.docs.auth.api.v1.ObjectFactory.class);

            responseUnmarshaller = new ResponseUnmarshaller(jaxbContext);

            headers.put("Accept", MediaType.APPLICATION_XML);
       
            String response = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><token xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" "
                    + "created=\"2012-04-04T11:38:49.000-05:00\" userId=\"" + userId + "\" userURL=\"https://n01.endpoint.auth.rackspacecloud.com/v2.0/usertest1\" "
                    + "id=\"9f83bee1-305e-4ad6-8140-9c1e7e27200b\" expires=\"2012-04-05T11:38:49.000-05:00\"/>";

            InputStream data = new ByteArrayInputStream(response.getBytes());

            when(serviceClientResponse.getData()).thenReturn(data);
            when(serviceClientResponse.getStatusCode()).thenReturn(200);
            when(serviceClient.get(authEndpoint + "/token/" + inputToken, headers, "belongsTo", inputUser, "type", inputType)).thenReturn(serviceClientResponse);
            tokenResponse = responseUnmarshaller.unmarshall(serviceClientResponse.getData(), FullToken.class);
            rackspaceToken=new RackspaceToken(result.getResult(), tokenResponse);
    }
   
    /**
     * Test of getTenantId method, of class RackspaceToken.
     */
    @Test
    public void testGetTenantId() {
        
        String expResult = "345897";
        String result = rackspaceToken.getTenantId();
        assertEquals(expResult, result);
       
    }

    /**
     * Test of getTenantName method, of class RackspaceToken.
     */
    @Test
    public void testGetTenantName() {
   
        String expResult = "345897";
        String result = rackspaceToken.getTenantName();
        assertEquals(expResult, result);
      
    }

    /**
     * Test of getUserId method, of class RackspaceToken.
     */
    @Test
    public void testGetUserId() {
     
        String expResult = "usertest1";
        String result = rackspaceToken.getUserId();
        assertEquals(expResult, result);
     
    }

    /**
     * Test of getTokenId method, of class RackspaceToken.
     */
    @Test
    public void testGetTokenId() {
  
        String expResult = "9f83bee1-305e-4ad6-8140-9c1e7e27200b";
        String result = rackspaceToken.getTokenId();
        assertEquals(expResult, result);

    }

    /**
     * Test of getExpires method, of class RackspaceToken.
     */
    @Test
    public void testGetExpires() {
   
        long expResult = (long) 1333643929000L;
        long result = rackspaceToken.getExpires();
        assertEquals(expResult, result);
      
    }

    /**
     * Test of getUsername method, of class RackspaceToken.
     */
    @Test(expected=UnsupportedOperationException.class)
    public void testGetUsername() {

        String expResult = "";
        String result = rackspaceToken.getUsername();
        assertEquals(expResult, result);
   
    }

    /**
     * Test of getRoles method, of class RackspaceToken.
     */
   @Test(expected=UnsupportedOperationException.class)
    public void testGetRoles() {

        String expResult = "";
        String result = rackspaceToken.getRoles();
        assertEquals(expResult, result);
       
    }

    /**
     * Test of getImpersonatorTenantId method, of class RackspaceToken.
     */
    @Test
    public void testGetImpersonatorTenantId() {

        String expResult = "";
        String result = rackspaceToken.getImpersonatorTenantId();
        assertEquals(expResult, result);
      
    }

    /**
     * Test of getImpersonatorUsername method, of class RackspaceToken.
     */
    @Test
    public void testGetImpersonatorUsername() {
   
        String expResult = "";
        String result = rackspaceToken.getImpersonatorUsername();
        assertEquals(expResult, result);
     
    }
}
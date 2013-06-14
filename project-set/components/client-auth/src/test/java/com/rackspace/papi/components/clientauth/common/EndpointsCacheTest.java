/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.clientauth.common;

import com.rackspace.auth.AuthToken;
import com.rackspace.papi.commons.util.io.ObjectSerializer;
import static com.rackspace.papi.components.clientauth.common.UserAuthTokenCacheTest.CACHE_PREFIX;
import static com.rackspace.papi.components.clientauth.common.UserAuthTokenCacheTest.VALID_USER;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.impl.StoredElementImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 *
 * @author kush5342
 */
public class EndpointsCacheTest {
    
    
    public static final String VALID_USER = "user", VALID_AUTH_TOKEN = "abcdef", CACHE_PREFIX = "prefix";
    private static final String ENDPOINTS_CACHE_PREFIX = "openstack.endpoints.cache";

     public static EndpointsCache infoCache;
     public static AuthToken originalUser;
     public static Datastore mockedDatastore;
    

    
    @BeforeClass
    public static void setUpClass() throws Exception {
        
         originalUser = mock(AuthToken.class, withSettings().serializable());
         when(originalUser.getUserId()).thenReturn("userId");
         when(originalUser.getUsername()).thenReturn("username");
         when(originalUser.getExpires()).thenReturn(10000l);
         when(originalUser.getRoles()).thenReturn("roles");
         when(originalUser.getTokenId()).thenReturn("token");
         mockedDatastore = mock(Datastore.class);
         
         final String cacheFullName =ENDPOINTS_CACHE_PREFIX + "." + VALID_USER; 
         
         final StoredElement storedElement = new StoredElementImpl(cacheFullName, ObjectSerializer.instance().writeObject(originalUser));
         when(mockedDatastore.get(eq(cacheFullName))).thenReturn(storedElement);
         
         infoCache = new EndpointsCache(mockedDatastore, ENDPOINTS_CACHE_PREFIX) ;
      
    }
    



    /**
     * Test of storeEndpoints method, of class EndpointsCache.
     */
    @Test
    public void testStoreEndpoints() throws Exception {
        String endpoints = "endpoint";
        int ttl = 200;
        infoCache.storeEndpoints("token", endpoints, ttl);
     
    }
    
        /**
     * Test of getEndpoints method, of class EndpointsCache.
     */
    @Test
    public void testGetEndpoints() {
     
        String expResult = "endpoint";
        String result = infoCache.getEndpoints("token");
       // assertEquals(expResult, result);
   
    }
}
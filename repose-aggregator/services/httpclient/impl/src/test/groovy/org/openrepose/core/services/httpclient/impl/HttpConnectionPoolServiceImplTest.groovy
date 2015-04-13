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
package org.openrepose.core.services.httpclient.impl

import org.apache.http.client.HttpClient
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.params.CoreConnectionPNames
import org.junit.Before
import org.junit.Test
import org.openrepose.core.service.httpclient.config.HttpConnectionPoolConfig
import org.openrepose.core.service.httpclient.config.PoolType
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.healthcheck.HealthCheckService
import org.openrepose.core.services.httpclient.HttpClientResponse

import static org.junit.Assert.*
import static org.mockito.Mockito.*

class HttpConnectionPoolServiceImplTest {

    HttpConnectionPoolConfig poolCfg;
    HttpConnectionPoolServiceImpl srv;
    String POOL1_ID = "POOL1_ID"
    Boolean POOL1_DEFAULT = false
    Integer POOL1_MAX_CON = 10
    Integer POOL1_SO_TIMEOUT = 20000
    String POOL2_ID = "POOL2_ID"
    Boolean POOL2_DEFAULT = true
    Integer POOL2_MAX_CON = 20
    Integer POOL2_SO_TIMEOUT = 60000
    String POOLU_ID = "POOLU_ID"
    // Retrieve the defaults defined in the XSD.
    PoolType poolType = new PoolType()
    Integer POOLU_SO_TIMEOUT = poolType.getHttpSocketTimeout()

    @Before
    void setUp() {
        List<PoolType> pools = PoolTypeHelper.createListOfPools(2, 2)
        PoolType poolType = pools.get(0);
        poolType.setId(POOL1_ID)
        poolType.setDefault(POOL1_DEFAULT)
        poolType.setHttpConnManagerMaxTotal(POOL1_MAX_CON)
        poolType.setHttpSocketTimeout(POOL1_SO_TIMEOUT)
        poolType = pools.get(1);
        poolType.setId(POOL2_ID)
        poolType.setDefault(POOL2_DEFAULT)
        poolType.setHttpConnManagerMaxTotal(POOL2_MAX_CON)
        poolType.setHttpSocketTimeout(POOL2_SO_TIMEOUT)

        poolCfg = new HttpConnectionPoolConfig();
        poolCfg.pool.addAll(pools);

        srv = new HttpConnectionPoolServiceImpl(mock(ConfigurationService.class), mock(HealthCheckService.class));
        srv.configure(poolCfg);
    }

    @Test
    void testGetClient() {
        HttpClient client = srv.getClient(POOL1_ID).getHttpClient();
        assertEquals("Should retrieve requested client", POOL1_SO_TIMEOUT, client.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT));
    }

    @Test
    void testGetAvailablePools() {
        assertEquals("Pool Service should have two client pools available", 2, srv.getAvailableClients().size());
    }

    @Test
    void testHttpRandomConnectionPool() {
        HttpClient client = srv.getClient(POOLU_ID).getHttpClient();
        assertEquals("Should retrieve default client", POOLU_SO_TIMEOUT, client.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT));
    }

    @Test
    void getDefaultClientPoolByPassingNull() {
        HttpClient client = srv.getClient(null).getHttpClient();
        assertEquals("Should retrieve default client", POOL2_SO_TIMEOUT, client.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT));
    }

    @Test
    void shouldReturnIfClientIsOrIsntAvailable() {
        assertTrue("Should return true if client is available", srv.isAvailable(POOL1_ID));
        assertFalse("Should return false if client is not available", srv.isAvailable(POOLU_ID));
    }

    @Test
    void shouldReturnPoolDefaultIfNotConfigured() {
        HttpConnectionPoolServiceImpl cpool = new HttpConnectionPoolServiceImpl(mock(ConfigurationService.class), mock(HealthCheckService.class));
        HttpClientResponse clientResponse = cpool.getClient(null);

        assertNotNull(clientResponse);
        assertNotNull(clientResponse.getHttpClient());
    }

    @Test
    void shouldHaveSameDefaultSettingsWhenNoConfigVsDefaultConfig() {
        // Create a pool service with NO configuration and obtain an HttpClient
        HttpConnectionPoolServiceImpl poolNotConfigured = new HttpConnectionPoolServiceImpl(mock(ConfigurationService.class), mock(HealthCheckService.class));
        HttpClient client1 = poolNotConfigured.getClient(null).getHttpClient();

        // Create a pool service with a default only configuration and obtain an HttpClient
        HttpConnectionPoolServiceImpl poolConfiguredWithDefault = new HttpConnectionPoolServiceImpl(mock(ConfigurationService.class), mock(HealthCheckService.class));
        HttpConnectionPoolConfig poolCfg = new HttpConnectionPoolConfig();
        poolCfg.pool.add(new PoolType())
        poolConfiguredWithDefault.configure(poolCfg)
        HttpClient client2 = poolNotConfigured.getClient(null).getHttpClient();

        // Verify that all parameters are the same when NO config vs default config
        assertEquals(client1.getParams().getParameter(CoreConnectionPNames.MAX_LINE_LENGTH), client2.getParams().getParameter(CoreConnectionPNames.MAX_LINE_LENGTH));
        assertEquals(client1.getParams().getParameter(CoreConnectionPNames.CONNECTION_TIMEOUT), client2.getParams().getParameter(CoreConnectionPNames.CONNECTION_TIMEOUT))
        assertEquals(client1.getParams().getParameter(CoreConnectionPNames.MAX_HEADER_COUNT), client2.getParams().getParameter(CoreConnectionPNames.MAX_HEADER_COUNT));
        assertEquals(client1.getParams().getParameter(CoreConnectionPNames.TCP_NODELAY), client2.getParams().getParameter(CoreConnectionPNames.TCP_NODELAY));
        assertEquals(client1.getParams().getParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE), client2.getParams().getParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE));
        assertEquals(client1.getConnectionKeepAliveStrategy().timeout, client2.getConnectionKeepAliveStrategy().timeout);

        Map props1 = client1.connectionManager.properties;
        Map props2 = client1.connectionManager.properties;

        assertEquals(props1.get("defaultMaxPerRoute"), props2.get("defaultMaxPerRoute"));
        assertEquals(props1.get("maxTotal"), props2.get("maxTotal"));
    }

    @Test
    void shouldShutdownAllConnectionPools() {
        HttpConnectionPoolServiceImpl cpool = new HttpConnectionPoolServiceImpl(mock(ConfigurationService.class), mock(HealthCheckService.class));
        HttpClient mockClient = mock(HttpClient.class)
        ClientConnectionManager mockConnMgr = mock(ClientConnectionManager.class)
        cpool.poolMap.put("MOCK", mockClient)
        when(mockClient.getConnectionManager()).thenReturn(mockConnMgr)

        try {
            cpool.shutdown()
        } catch (NullPointerException npe) {
            //TODO: THIS IS A SUPER DIRTY HACK BECAUSE THIS TES IT JUST BEING SHOVED INTO HERE.
            //TODO: THIS CLASS NEEDS TO BE REFACTORED TO NOT FAIL LIKE THIS
        }
        verify(mockConnMgr).shutdown()
    }

    @Test
    void shouldShutdownAllExistingConnectionPoolsDuringReconfigure() {
        HttpClient mockClient = mock(HttpClient.class)
        ClientConnectionManager mockConnMgr = mock(PoolingClientConnectionManager.class)
        when(mockClient.getConnectionManager()).thenReturn(mockConnMgr)

        srv.poolMap.put("MOCK", mockClient)
        poolCfg = new HttpConnectionPoolConfig();
        srv.configure(poolCfg);

        verify(mockConnMgr).closeExpiredConnections()
    }

    @Test
    void shouldRegisterUserWhenGettingNullClient() {
        HttpClientResponse clientResponse = srv.getClient(null);
        assertNotNull(clientResponse)
        assertTrue(srv.httpClientUserManager.registeredClientUsers.containsKey(clientResponse.clientInstanceId))
    }

    @Test
    void shouldRegisterUserWhenGettingEmptyClient() {
        HttpClientResponse clientResponse = srv.getClient("");
        assertNotNull(clientResponse)
        assertTrue(srv.httpClientUserManager.registeredClientUsers.containsKey(clientResponse.clientInstanceId))
    }

    @Test
    void shouldRegisterUserWhenGettingNamedDefaultClient() {
        HttpClientResponse clientResponse = srv.getClient(HttpConnectionPoolServiceImpl.DEFAULT_POOL_ID);
        assertNotNull(clientResponse)
        assertTrue(srv.httpClientUserManager.registeredClientUsers.containsKey(clientResponse.clientInstanceId))
    }

    @Test
    void shouldRegisterUserWhenGettingNonDefaultClient() {
        HttpClientResponse clientResponse = srv.getClient(POOLU_ID);
        assertNotNull(clientResponse)
        assertTrue(srv.httpClientUserManager.registeredClientUsers.containsKey(clientResponse.clientInstanceId))
    }

    @Test
    void shouldReleaseUserFromClientWhenBothAreValid() {
        HttpClientResponse clientResponse = srv.getClient(POOLU_ID);

        assertTrue(srv.httpClientUserManager.registeredClientUsers.containsKey(clientResponse.clientInstanceId))
        assertEquals(1, srv.httpClientUserManager.registeredClientUsers.get(clientResponse.clientInstanceId).size())

        srv.releaseClient(clientResponse)

        assertEquals(0, srv.httpClientUserManager.registeredClientUsers.get(clientResponse.clientInstanceId).size())
    }

    @Test
    void getMaxConnections() {
        int maxConnectionsOne = srv.getMaxConnections(POOL1_ID);
        int maxConnectionsTwo = srv.getMaxConnections(POOL2_ID);
        int maxConnectionsUnk = srv.getMaxConnections(POOLU_ID);
        int maxConnectionsNul = srv.getMaxConnections(null);

        assertEquals(POOL1_MAX_CON, maxConnectionsOne);
        assertEquals(POOL2_MAX_CON, maxConnectionsTwo);
        assertEquals(POOL2_MAX_CON, maxConnectionsUnk);
        assertEquals(POOL2_MAX_CON, maxConnectionsNul);
    }

    @Test
    void getSocketTimeouts() {
        int soTimeoutOne = srv.getSocketTimeout(POOL1_ID);
        int soTimeoutTwo = srv.getSocketTimeout(POOL2_ID);
        int soTimeoutUnk = srv.getSocketTimeout(POOLU_ID);
        int soTimeoutNul = srv.getSocketTimeout(null);

        assertEquals(POOL1_SO_TIMEOUT, soTimeoutOne);
        assertEquals(POOL2_SO_TIMEOUT, soTimeoutTwo);
        assertEquals(POOL2_SO_TIMEOUT, soTimeoutUnk);
        assertEquals(POOL2_SO_TIMEOUT, soTimeoutNul);
    }
}

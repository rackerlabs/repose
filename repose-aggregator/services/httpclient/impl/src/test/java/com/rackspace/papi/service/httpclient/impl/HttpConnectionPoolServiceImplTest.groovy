package com.rackspace.papi.service.httpclient.impl

import com.rackspace.papi.service.httpclient.HttpClientResponse
import com.rackspace.papi.service.httpclient.config.HttpConnectionPoolConfig
import com.rackspace.papi.service.httpclient.config.PoolType
import org.apache.http.client.HttpClient
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.params.CoreConnectionPNames
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class HttpConnectionPoolServiceImplTest {

    HttpConnectionPoolConfig poolCfg;
    HttpConnectionPoolServiceImpl srv;
    Boolean POOL1_TCPNODELAY = false
    Boolean POOL2_TCPNODELAY = true

    @Before
    void setUp() {
        List<PoolType> pools = PoolTypeHelper.createListOfPools(2, 2)
        pools.get(0).setHttpTcpNodelay(POOL1_TCPNODELAY)
        pools.get(1).setHttpTcpNodelay(POOL2_TCPNODELAY)

        poolCfg = new HttpConnectionPoolConfig();
        poolCfg.pool.addAll(pools);

        srv = new HttpConnectionPoolServiceImpl();
        srv.configure(poolCfg);
    }


    @Test
    void testGetClient() {
        HttpClient client = srv.getClient("pool1").getHttpClient();
        assertEquals("Should retrieve requested client", client.getParams().getParameter(CoreConnectionPNames.TCP_NODELAY), POOL1_TCPNODELAY);
    }

    @Test
    void testGetAvailablePools() {
        assertEquals("Pool Service should have two client pools available", srv.getAvailableClients().size(), 2);
    }

    @Test
    void testHttpRandomConnectionPool() {
        HttpClient client = srv.getClient("nonexistent client").getHttpClient();
        assertEquals("Should retrive default client", client.getParams().getParameter(CoreConnectionPNames.TCP_NODELAY), POOL2_TCPNODELAY);
    }

    @Test
    void getDefaultClientPoolByPassingNull() {
        HttpClient client = srv.getClient(null).getHttpClient();
        assertEquals("Should retrive default client", client.getParams().getParameter(CoreConnectionPNames.TCP_NODELAY), POOL2_TCPNODELAY);
    }

    @Test
    void shouldReturnIfClientIsOrIsntAvailable() {
        assertTrue("Should return true if client is available", srv.isAvailable("pool1"));
        assertFalse("Should return false if client is not available", srv.isAvailable("nonexistent pool"));
    }

    @Test
    void shouldReturnPoolDefaultIfNotConfigured() {
        HttpConnectionPoolServiceImpl cpool = new HttpConnectionPoolServiceImpl();
        HttpClientResponse clientResponse = cpool.getClient(null);

        assertNotNull(clientResponse);
        assertNotNull(clientResponse.getHttpClient());
    }

    @Test
    void shouldHaveSameDefaultSettingsWhenNoConfigVsDefaultConfig() {
        // Create a pool service with NO configuration and obtain an HttpClient
        HttpConnectionPoolServiceImpl poolNotConfigured = new HttpConnectionPoolServiceImpl();
        HttpClient client1 = poolNotConfigured.getClient(null).getHttpClient();

        // Create a pool service with a default only configuration and obtain an HttpClient
        HttpConnectionPoolServiceImpl poolConfiguredWithDefault = new HttpConnectionPoolServiceImpl();
        HttpConnectionPoolConfig poolCfg = new HttpConnectionPoolConfig();
        poolCfg.pool.add(new PoolType())
        poolConfiguredWithDefault.configure(poolCfg)
        HttpClient client2 = poolNotConfigured.getClient(null).getHttpClient();

        // Verify that all parameters are the same when NO config vs default config
        assertEquals(client1.getParams().getParameter(CoreConnectionPNames.MAX_LINE_LENGTH), client2.getParams().getParameter(CoreConnectionPNames.MAX_LINE_LENGTH));
        assertEquals(client1.getParams().getParameter(CoreConnectionPNames.CONNECTION_TIMEOUT), client2.getParams().getParameter(CoreConnectionPNames.CONNECTION_TIMEOUT))
        assertEquals(client1.getParams().getParameter(CoreConnectionPNames.MAX_HEADER_COUNT), client2.getParams().getParameter(CoreConnectionPNames.MAX_HEADER_COUNT));
        assertEquals(client1.getParams().getParameter(CoreConnectionPNames.TCP_NODELAY), client2.getParams().getParameter(CoreConnectionPNames.TCP_NODELAY));
        assertEquals(client1.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT), client2.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT));
        assertEquals(client1.getParams().getParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE), client2.getParams().getParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE));
        assertEquals(client1.getConnectionKeepAliveStrategy().timeout, client2.getConnectionKeepAliveStrategy().timeout);

        Map props1 = client1.connectionManager.properties;
        Map props2 = client1.connectionManager.properties;

        assertEquals(props1.get("defaultMaxPerRoute"), props2.get("defaultMaxPerRoute"));
        assertEquals(props1.get("maxTotal"), props2.get("maxTotal"));
    }


    @Test
    void shouldShutdownAllConnectionPools() {
        HttpConnectionPoolServiceImpl cpool = new HttpConnectionPoolServiceImpl();
        HttpClient mockClient = mock(HttpClient.class)
        ClientConnectionManager mockConnMgr = mock(ClientConnectionManager.class)
        cpool.poolMap.put("MOCK", mockClient)
        when(mockClient.getConnectionManager()).thenReturn(mockConnMgr)

        cpool.shutdown()
        verify(mockConnMgr).shutdown()
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
        HttpClientResponse clientResponse = srv.getClient("defaultPool");
        assertNotNull(clientResponse)
        assertTrue(srv.httpClientUserManager.registeredClientUsers.containsKey(clientResponse.clientInstanceId))
    }

    @Test
    void shouldRegisterUserWhenGettingNonDefaultClient() {
        HttpClientResponse clientResponse = srv.getClient("pool");
        assertNotNull(clientResponse)
        assertTrue(srv.httpClientUserManager.registeredClientUsers.containsKey(clientResponse.clientInstanceId))
    }

    @Test
    void shouldReleaseUserFromClientWhenBothAreValid() {
        HttpClientResponse clientResponse = srv.getClient("pool");

        assertTrue(srv.httpClientUserManager.registeredClientUsers.containsKey(clientResponse.clientInstanceId))
        assertTrue(srv.httpClientUserManager.registeredClientUsers.get(clientResponse.clientInstanceId).size() == 1)

        srv.releaseClient(clientResponse)

        assertTrue(srv.httpClientUserManager.registeredClientUsers.get(clientResponse.clientInstanceId).size() == 0)
    }
}

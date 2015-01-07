package org.openrepose.services.httpclient.impl

import org.apache.http.client.HttpClient
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.params.CoreConnectionPNames
import org.junit.Before
import org.junit.Test
import org.openrepose.core.service.httpclient.config.HttpConnectionPoolConfig
import org.openrepose.core.service.httpclient.config.PoolType
import org.openrepose.services.httpclient.HttpClientResponse

import static org.junit.Assert.*
import static org.mockito.Mockito.*

class HttpConnectionPoolServiceImplTest {

    HttpConnectionPoolConfig poolCfg;
    HttpConnectionPoolServiceImpl srv;
    String  POOL1_ID = "POOL1_ID"
    Integer POOL1_SO_TIMEOUT = 20000
    String  POOL2_ID = "POOL2_ID"
    Integer POOL2_SO_TIMEOUT = 60000
    String  POOLU_ID = "POOLU_ID"

    @Before
    void setUp() {
        List<PoolType> pools = PoolTypeHelper.createListOfPools(2, 2)
        PoolType poolType = pools.get(0);
        poolType.setId(POOL1_ID)
        poolType.setHttpSocketTimeout(POOL1_SO_TIMEOUT)
        poolType = pools.get(1);
        poolType.setId(POOL2_ID)
        poolType.setHttpSocketTimeout(POOL2_SO_TIMEOUT)

        poolCfg = new HttpConnectionPoolConfig();
        poolCfg.pool.addAll(pools);

        srv = new HttpConnectionPoolServiceImpl();
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
}

package com.rackspace.papi.service.httpclient.impl

import com.rackspace.papi.service.httpclient.HttpClientResponse
import com.rackspace.papi.service.httpclient.HttpClientService
import com.rackspace.papi.service.httpclient.config.HttpConnectionPoolConfig
import com.rackspace.papi.service.httpclient.config.PoolType
import org.apache.http.client.HttpClient
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.params.CoreConnectionPNames
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*
import static org.mockito.Mockito.*

class HttpConnectionPoolServiceImplTest {


    private PoolType poolType1, poolType2;
    private final static int CONN_TIMEOUT = 30000;
    private final static int MAX_HEADERS = 100;
    private final static int MAX_LINE = 50;
    private final static int SOC_TIMEOUT = 40000;
    private final static boolean TCP_NODELAY = false;
    private final static boolean TCP_NODELAY2 = true;
    private final static int SOC_BUFF_SZ = 1023;
    private final static int MAX_PER_ROUTE = 50;
    private final static int MAX_TOTAL = 300;

    HttpConnectionPoolConfig poolCfg;

    HttpClientService srv;

    @Before
    void setUp() {

        poolType1 = new PoolType();

        poolType1.setHttpConnectionMaxHeaderCount(MAX_HEADERS);
        poolType1.setHttpConnectionMaxLineLength(MAX_LINE);
        poolType1.setHttpConnectionMaxStatusLineGarbage(10);
        poolType1.setHttpConnectionTimeout(CONN_TIMEOUT);
        poolType1.setHttpConnManagerMaxPerRoute(MAX_PER_ROUTE);
        poolType1.setHttpConnManagerMaxTotal(MAX_TOTAL);
        poolType1.setHttpSocketBufferSize(SOC_BUFF_SZ);
        poolType1.setHttpSocketTimeout(SOC_TIMEOUT);
        poolType1.setHttpTcpNodelay(TCP_NODELAY);
        poolType1.setId("pool");



        poolType2 = new PoolType();

        poolType2.setHttpConnectionMaxHeaderCount(MAX_HEADERS);
        poolType2.setHttpConnectionMaxLineLength(MAX_LINE);
        poolType2.setHttpConnectionMaxStatusLineGarbage(10);
        poolType2.setHttpConnectionTimeout(CONN_TIMEOUT);
        poolType2.setHttpConnManagerMaxPerRoute(MAX_PER_ROUTE);
        poolType2.setHttpConnManagerMaxTotal(MAX_TOTAL);
        poolType2.setHttpSocketBufferSize(SOC_BUFF_SZ);
        poolType2.setHttpSocketTimeout(SOC_TIMEOUT);
        poolType2.setHttpTcpNodelay(TCP_NODELAY2);
        poolType2.setDefault(true);
        poolType2.setId("defaultPool");

        List<PoolType> pools = new ArrayList<PoolType>();

        pools.add(poolType2);
        pools.add(poolType1);

        poolCfg = new HttpConnectionPoolConfig();
        poolCfg.pool.addAll(pools);

        srv = new HttpConnectionPoolServiceImpl();
        srv.configure(poolCfg);


    }


    @Test
    void testGetClient() {
        HttpClient client = srv.getClient("pool").getHttpClient();
        assertEquals("Should retrive default client", client.getParams().getParameter(CoreConnectionPNames.TCP_NODELAY), false);
    }

    @Test
    void testGetAvailablePools() {
        assertEquals("Pool Service should have two client pools available", srv.getAvailableClients().size(), 2);
    }

    @Test
    void testHttpRandomConnectionPool() {
        HttpClient client = srv.getClient("nonexistent client").getHttpClient();
        assertEquals("Should retrive default client", client.getParams().getParameter(CoreConnectionPNames.TCP_NODELAY), true);
    }

    @Test
    void getDefaultClientPoolByPassingNull() {
        HttpClient client = srv.getClient(null).getHttpClient();
        assertEquals("Should retrive default client", client.getParams().getParameter(CoreConnectionPNames.TCP_NODELAY), true);
    }

    @Test
    void shouldReturnIfClientIsOrIsntAvailable() {
        assertTrue("Should return true if client is available", srv.isAvailable("pool"));
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
}

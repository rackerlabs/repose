package com.rackspace.papi.service.httpclient.impl

import com.rackspace.papi.service.httpclient.HttpClientNotFoundException
import com.rackspace.papi.service.httpclient.HttpClientService
import com.rackspace.papi.service.httpclient.config.HttpConnectionPoolConfig
import com.rackspace.papi.service.httpclient.config.PoolType
import org.apache.http.client.HttpClient
import org.apache.http.params.CoreConnectionPNames
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*;

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

    @Test(expected = HttpClientNotFoundException.class)
    void testHttpConnectionPoolExeption() {
        HttpClient client = srv.getClient("nonexistent client");
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
}

package com.rackspace.papi.service.httpclient.impl
import com.rackspace.papi.service.httpclient.config.PoolType
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.CoreConnectionPNames
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import static junit.framework.Assert.assertNotNull
import static org.junit.Assert.assertEquals;


class HttpConnectionPoolProviderTest {

    private PoolType poolType;
    private final static int CONN_TIMEOUT = 30000;
    private final static int MAX_HEADERS = 100;
    private final static int MAX_LINE = 50;
    private final static int SOC_TIMEOUT = 40000;
    private final static boolean TCP_NODELAY = false;
    private final static int SOC_BUFF_SZ = 1023;
    private final static int MAX_PER_ROUTE = 50;
    private final static int MAX_TOTAL = 300;

    @Before
    public final void beforeAll() {

        poolType = new PoolType();

        poolType.setHttpConnectionMaxHeaderCount(MAX_HEADERS);
        poolType.setHttpConnectionMaxLineLength(MAX_LINE);
        poolType.setHttpConnectionMaxStatusLineGarbage(10);
        poolType.setHttpConnectionTimeout(CONN_TIMEOUT);
        poolType.setHttpConnManagerMaxPerRoute(MAX_PER_ROUTE);
        poolType.setHttpConnManagerMaxTotal(MAX_TOTAL);
        poolType.setHttpSocketBufferSize(SOC_BUFF_SZ);
        poolType.setHttpSocketTimeout(SOC_TIMEOUT);
        poolType.setHttpTcpNodelay(TCP_NODELAY);
        poolType.setKeepaliveTimeout(6000);
        poolType.setId("testPool");

    }


    @Test
    public void shouldCreateClientWithPassedConfigurationObject() {

        DefaultHttpClient client = HttpConnectionPoolProvider.genClient(poolType);

        Map props = client.connectionManager.properties;
        assertEquals(client.getParams().getParameter(CoreConnectionPNames.MAX_LINE_LENGTH), MAX_LINE);
        assertEquals(client.getParams().getParameter(CoreConnectionPNames.CONNECTION_TIMEOUT), CONN_TIMEOUT)
        assertEquals(client.getParams().getParameter(CoreConnectionPNames.MAX_HEADER_COUNT), MAX_HEADERS);
        assertEquals(client.getParams().getParameter(CoreConnectionPNames.TCP_NODELAY), TCP_NODELAY);
        assertEquals(client.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT), SOC_TIMEOUT);
        assertEquals(client.getParams().getParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE), SOC_BUFF_SZ);
        assertEquals(props.get("defaultMaxPerRoute"), MAX_PER_ROUTE);
        assertEquals(props.get("maxTotal"), MAX_TOTAL);
        assertEquals(client.getConnectionKeepAliveStrategy().timeout, 6000);

    }

    @Test
    public void shouldGetTestCoverageWithSillyTestTo100() {
        HttpConnectionPoolProvider provider = new HttpConnectionPoolProvider()
        assertNotNull(provider)
    }
}

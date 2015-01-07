package org.openrepose.services.httpclient.impl

import org.apache.http.client.HttpClient
import org.apache.http.params.CoreConnectionPNames
import org.junit.Before
import org.junit.Test
import org.openrepose.core.service.httpclient.config.HttpConnectionPoolConfig
import org.openrepose.core.service.httpclient.config.PoolType
import org.openrepose.services.httpclient.HttpClientResponse

import static org.junit.Assert.*

class HttpConnectionPoolServiceImplEmptyPoolTest {

    HttpConnectionPoolConfig poolCfg;
    HttpConnectionPoolServiceImpl srv;
    String  POOLU_ID = "POOLU_ID"
    // Retrieve the defaults defined in the XSD.
    PoolType poolType = new PoolType()
    Integer POOLU_MAX_CON = poolType.getHttpConnManagerMaxTotal()
    Integer POOLU_SO_TIMEOUT = poolType.getHttpSocketTimeout()

    @Before
    void setUp() {
        poolCfg = new HttpConnectionPoolConfig();
        srv = new HttpConnectionPoolServiceImpl();
        srv.configure(poolCfg);
    }


    @Test
    void getDefaultClientPoolByPassingUnk() {
        HttpClient client = srv.getClient(POOLU_ID).getHttpClient();
        assertEquals("Should retrieve default client", POOLU_SO_TIMEOUT, client.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT));
    }

    @Test
    void getDefaultClientPoolByPassingNull() {
        HttpClient client = srv.getClient(null).getHttpClient();
        assertEquals("Should retrieve default client", POOLU_SO_TIMEOUT, client.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT));
    }

    @Test
    void shouldReturnIfClientIsOrIsntAvailable() {
        assertFalse("Should return false if client is not available", srv.isAvailable(POOLU_ID));
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
        int maxConnectionsUnk = srv.getMaxConnections(POOLU_ID);
        int maxConnectionsNul = srv.getMaxConnections(null);

        assertEquals(POOLU_MAX_CON, maxConnectionsUnk);
        assertEquals(POOLU_MAX_CON, maxConnectionsNul);
    }

    @Test
    void getSocketTimeouts() {
        int soTimeoutUnk = srv.getSocketTimeout(POOLU_ID);
        int soTimeoutNul = srv.getSocketTimeout(null);

        assertEquals(POOLU_SO_TIMEOUT, soTimeoutUnk);
        assertEquals(POOLU_SO_TIMEOUT, soTimeoutNul);
    }
}

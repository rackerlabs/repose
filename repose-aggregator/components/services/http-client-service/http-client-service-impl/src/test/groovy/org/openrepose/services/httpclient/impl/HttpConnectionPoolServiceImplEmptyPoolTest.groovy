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
package org.openrepose.services.httpclient.impl

import org.apache.http.client.HttpClient
import org.apache.http.params.CoreConnectionPNames
import org.junit.Before
import org.junit.Test
import org.openrepose.core.service.httpclient.config.HttpConnectionPoolConfig
import org.openrepose.core.service.httpclient.config.PoolType
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.healthcheck.HealthCheckService
import org.openrepose.core.services.httpclient.HttpClientResponse
import org.openrepose.core.services.httpclient.impl.HttpConnectionPoolServiceImpl

import static org.junit.Assert.*
import static org.mockito.Mockito.mock

class HttpConnectionPoolServiceImplEmptyPoolTest {

    HttpConnectionPoolConfig poolCfg;
    HttpConnectionPoolServiceImpl srv;
    String POOLU_ID = "POOLU_ID"
    // Retrieve the defaults defined in the XSD.
    PoolType poolType = new PoolType()
    Integer POOLU_MAX_CON = poolType.getHttpConnManagerMaxTotal()
    Integer POOLU_SO_TIMEOUT = poolType.getHttpSocketTimeout()

    @Before
    void setUp() {
        poolCfg = new HttpConnectionPoolConfig();
        srv = new HttpConnectionPoolServiceImpl(mock(ConfigurationService), mock(HealthCheckService));
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

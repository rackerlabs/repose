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

import io.opentracing.mock.MockTracer
import org.apache.http.client.HttpClient
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.params.CoreConnectionPNames
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openrepose.core.service.httpclient.config.HttpConnectionPoolConfig
import org.openrepose.core.service.httpclient.config.PoolType
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.healthcheck.HealthCheckService
import org.openrepose.core.services.healthcheck.HealthCheckServiceProxy
import org.openrepose.core.services.httpclient.HttpClientContainer

import static org.hamcrest.Matchers.hasKey
import static org.junit.Assert.*
import static org.mockito.Mockito.*

class HttpConnectionPoolServiceImplTest {

    HttpConnectionPoolConfig poolCfg
    HttpConnectionPoolServiceImpl srv
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
    ConfigurationService configurationService = mock(ConfigurationService)
    HealthCheckService healthCheckService = mock(HealthCheckService)
    MockTracer tracer = new MockTracer()
    String configurationRoot = ""

    @Before
    void setUp() {
        when(healthCheckService.register()).thenReturn(mock(HealthCheckServiceProxy))

        List<PoolType> pools = PoolTypeHelper.createListOfPools(2, 2)
        PoolType poolType = pools.get(0)
        poolType.setId(POOL1_ID)
        poolType.setDefault(POOL1_DEFAULT)
        poolType.setHttpConnManagerMaxTotal(POOL1_MAX_CON)
        poolType.setHttpSocketTimeout(POOL1_SO_TIMEOUT)
        poolType = pools.get(1)
        poolType.setId(POOL2_ID)
        poolType.setDefault(POOL2_DEFAULT)
        poolType.setHttpConnManagerMaxTotal(POOL2_MAX_CON)
        poolType.setHttpSocketTimeout(POOL2_SO_TIMEOUT)

        poolCfg = new HttpConnectionPoolConfig()
        poolCfg.pool.addAll(pools)

        srv = new HttpConnectionPoolServiceImpl(
            configurationService, healthCheckService, tracer, configurationRoot, "1.two.III")
        srv.configure(poolCfg)
        srv.with {
            initialized = true
        }
    }

    @After
    void destroy() {
        srv.destroy()
    }

    @Test(expected = IllegalStateException.class)
    void testGetDefaultClientInitializationException() {
        srv = new HttpConnectionPoolServiceImpl(
            configurationService, healthCheckService, tracer, configurationRoot, "1.two.III")
        srv.getDefaultClient()
    }

    @Test(expected = IllegalStateException.class)
    void testGetClientInitializationException() {
        srv = new HttpConnectionPoolServiceImpl(
            configurationService, healthCheckService, tracer, configurationRoot, "1.two.III")
        srv.getClient("foo")
    }

    @Test(expected = IllegalStateException.class)
    void testReleaseClientInitializationException() {
        srv = new HttpConnectionPoolServiceImpl(
            configurationService, healthCheckService, tracer, configurationRoot, "1.two.III")
        srv.releaseClient(null)
    }

    @Test(expected = IllegalStateException.class)
    void testIsAvailableInitializationException() {
        srv = new HttpConnectionPoolServiceImpl(
            configurationService, healthCheckService, tracer, configurationRoot, "1.two.III")
        srv.isAvailable("foo")
    }

    @Test(expected = IllegalStateException.class)
    void testGetAvailableClientsInitializationException() {
        srv = new HttpConnectionPoolServiceImpl(
            configurationService, healthCheckService, tracer, configurationRoot, "1.two.III")
        srv.getAvailableClients()
    }

    @Test
    void getDefaultClient() {
        HttpClient client = srv.getDefaultClient().getHttpClient()
        assertEquals("Should retrieve default client", POOL2_SO_TIMEOUT, client.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT))
    }

    @Test
    void testGetClient() {
        HttpClient client = srv.getClient(POOL1_ID).getHttpClient()
        assertEquals("Should retrieve requested client", POOL1_SO_TIMEOUT, client.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT))
    }

    @Test
    void testGetAvailablePools() {
        assertEquals("Pool Service should have two client pools available", 2, srv.getAvailableClients().size())
    }

    @Test
    void testHttpRandomConnectionPool() {
        HttpClient client = srv.getClient(POOLU_ID).getHttpClient()
        assertEquals("Should retrieve default client", POOL2_SO_TIMEOUT, client.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT))
    }

    @Test
    void getDefaultClientPoolByPassingNull() {
        HttpClient client = srv.getClient(null).getHttpClient()
        assertEquals("Should retrieve default client", POOL2_SO_TIMEOUT, client.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT))
    }

    @Test
    void shouldReturnIfClientIsOrIsntAvailable() {
        assertTrue("Should return true if client is available", srv.isAvailable(POOL1_ID))
        assertFalse("Should return false if client is not available", srv.isAvailable(POOLU_ID))
    }

    @Test
    void shouldShutdownAllConnectionPools() {
        HttpConnectionPoolServiceImpl cpool = new HttpConnectionPoolServiceImpl(
            mock(ConfigurationService.class), mock(HealthCheckService.class), new MockTracer(), configurationRoot, "1.two.III")
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
        poolCfg = new HttpConnectionPoolConfig()
        srv.configure(poolCfg)

        verify(mockConnMgr).closeExpiredConnections()
    }

    @Test
    void shouldRegisterUserWhenGettingNullClient() {
        HttpClientContainer clientResponse = srv.getClient(null)
        assertNotNull(clientResponse)
        assertThat(srv.httpClientUserManager.registeredClientUsers, hasKey(clientResponse.clientInstanceId))
    }

    @Test
    void shouldRegisterUserWhenGettingEmptyClient() {
        HttpClientContainer clientResponse = srv.getClient("")
        assertNotNull(clientResponse)
        assertThat(srv.httpClientUserManager.registeredClientUsers, hasKey(clientResponse.clientInstanceId))
    }

    @Test
    void shouldRegisterUserWhenGettingDefaultClient() {
        HttpClientContainer clientResponse = srv.getDefaultClient()
        assertNotNull(clientResponse)
        assertThat(srv.httpClientUserManager.registeredClientUsers, hasKey(clientResponse.clientInstanceId))
    }

    @Test
    void shouldRegisterUserWhenGettingNonDefaultClient() {
        HttpClientContainer clientResponse = srv.getClient(POOLU_ID)
        assertNotNull(clientResponse)
        assertThat(srv.httpClientUserManager.registeredClientUsers, hasKey(clientResponse.clientInstanceId))
    }

    @Test
    void shouldReleaseUserFromClientWhenBothAreValid() {
        HttpClientContainer clientResponse = srv.getClient(POOLU_ID)

        assertThat(srv.httpClientUserManager.registeredClientUsers, hasKey(clientResponse.clientInstanceId))
        assertEquals(1, srv.httpClientUserManager.registeredClientUsers.get(clientResponse.clientInstanceId).size())

        srv.releaseClient(clientResponse)

        assertEquals(0, srv.httpClientUserManager.registeredClientUsers.get(clientResponse.clientInstanceId).size())
    }
}

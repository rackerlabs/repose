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

import org.apache.http.Header
import org.apache.http.client.params.ClientPNames
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.CoreConnectionPNames
import org.junit.Before
import org.junit.Test
import org.openrepose.core.service.httpclient.config.HeaderListType
import org.openrepose.core.service.httpclient.config.HeaderType
import org.openrepose.core.service.httpclient.config.PoolType

class HttpConnectionPoolProviderTest {

    private final static int CONN_TIMEOUT = 30000
    private final static int MAX_HEADERS = 100
    private final static int MAX_LINE = 50
    private final static int SOC_TIMEOUT = 40000
    private final static boolean TCP_NODELAY = false
    private final static int SOC_BUFF_SZ = 1023
    private final static int MAX_PER_ROUTE = 50
    private final static int MAX_TOTAL = 300

    private PoolType poolType

    @Before
    public final void beforeAll() {
        poolType = new PoolType()

        poolType.setHttpConnectionMaxHeaderCount(MAX_HEADERS)
        poolType.setHttpConnectionMaxLineLength(MAX_LINE)
        poolType.setHttpConnectionMaxStatusLineGarbage(10)
        poolType.setHttpConnectionTimeout(CONN_TIMEOUT)
        poolType.setHttpConnManagerMaxPerRoute(MAX_PER_ROUTE)
        poolType.setHttpConnManagerMaxTotal(MAX_TOTAL)
        poolType.setHttpSocketBufferSize(SOC_BUFF_SZ)
        poolType.setHttpSocketTimeout(SOC_TIMEOUT)
        poolType.setHttpTcpNodelay(TCP_NODELAY)
        poolType.setKeepaliveTimeout(6000)
        poolType.setId("testPool")
    }

    @Test
    public void "should create client with passed-in configuration object"() {
        DefaultHttpClient client = HttpConnectionPoolProvider.genClient(poolType) as DefaultHttpClient

        Map props = client.connectionManager.properties
        assert client.getParams().getParameter(CoreConnectionPNames.MAX_LINE_LENGTH) == MAX_LINE
        assert client.getParams().getParameter(CoreConnectionPNames.CONNECTION_TIMEOUT) == CONN_TIMEOUT
        assert client.getParams().getParameter(CoreConnectionPNames.MAX_HEADER_COUNT) == MAX_HEADERS
        assert client.getParams().getParameter(CoreConnectionPNames.TCP_NODELAY) == TCP_NODELAY
        assert client.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT) == SOC_TIMEOUT
        assert client.getParams().getParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE) == SOC_BUFF_SZ
        assert props.get("defaultMaxPerRoute") == MAX_PER_ROUTE
        assert props.get("maxTotal") == MAX_TOTAL
        assert client.getConnectionKeepAliveStrategy().timeout == 6000
    }

    @Test
    public void "should get test coverage with silly test to 100"() {
        HttpConnectionPoolProvider provider = new HttpConnectionPoolProvider()
        assert provider
    }

    @Test
    public void "should add header parameter when configured"() {
        def headerListType = new HeaderListType()
        headerListType.getHeader().addAll(
                [new HeaderType(name: "lol", value: "potatoes"),
                 new HeaderType(name: "serious-business", value: "tomatoes")])
        poolType.setHeaders(headerListType)

        DefaultHttpClient client = HttpConnectionPoolProvider.genClient(poolType) as DefaultHttpClient

        def parameter = client.getParams().getParameter(ClientPNames.DEFAULT_HEADERS)
        assert parameter
        assert parameter in Collection
        parameter = parameter as Collection<?>
        assert parameter.size() == 2
        assert parameter[0] in Header
        assert parameter[0].name == "lol"
        assert parameter[0].value == "potatoes"
        assert parameter[1].name == "serious-business"
        assert parameter[1].value == "tomatoes"
    }

    @Test
    public void "should not add header parameter when not configured"() {
        poolType.setHeaders(null)

        DefaultHttpClient client = HttpConnectionPoolProvider.genClient(poolType) as DefaultHttpClient

        assert !client.getParams().getParameter(ClientPNames.DEFAULT_HEADERS)
    }
}

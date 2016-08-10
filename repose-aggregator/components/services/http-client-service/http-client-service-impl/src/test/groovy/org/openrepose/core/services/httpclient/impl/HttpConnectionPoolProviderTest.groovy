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
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.params.ClientPNames
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.CoreConnectionPNames
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openrepose.core.service.httpclient.config.HeaderListType
import org.openrepose.core.service.httpclient.config.HeaderType
import org.openrepose.core.service.httpclient.config.PoolType

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.nio.charset.Charset

class HttpConnectionPoolProviderTest {

    private final static int CONN_TIMEOUT = 30000
    private final static int MAX_HEADERS = 100
    private final static int MAX_LINE = 50
    private final static int SOC_TIMEOUT = 40000
    private final static boolean TCP_NODELAY = false
    private final static int SOC_BUFF_SZ = 1023
    private final static int MAX_PER_ROUTE = 50
    private final static int MAX_TOTAL = 300
    private final static Charset CHARSET_UTF8 = Charset.forName("UTF-8")

    private PoolType poolType
    private Server server


    @Before
    public final void beforeEach() {
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

    @After
    public final void afterEach() {
        server?.stop()
    }

    @Test
    public void "should create client with passed-in configuration object"() {
        DefaultHttpClient client = HttpConnectionPoolProvider.genClient("", poolType) as DefaultHttpClient

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

        DefaultHttpClient client = HttpConnectionPoolProvider.genClient("", poolType) as DefaultHttpClient

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

        DefaultHttpClient client = HttpConnectionPoolProvider.genClient("", poolType) as DefaultHttpClient

        assert !client.getParams().getParameter(ClientPNames.DEFAULT_HEADERS)
    }

    @Test
    public void "should create a pool that can connect to a server with client auth using keystore and truststore when configured"() {
        server = new Server()

        // SSL Context Factory
        def sslContextFactory = new SslContextFactory()
        sslContextFactory.keyStoreResource = Resource.newClassPathResource("server.jks")
        sslContextFactory.keyStorePassword = "password"
        sslContextFactory.keyManagerPassword = "password"
        sslContextFactory.needClientAuth = true
        sslContextFactory.trustStoreResource = Resource.newClassPathResource("client.jks")
        sslContextFactory.trustStorePassword = "password"

        // SSL HTTP Configuration
        def httpConfiguration = new HttpConfiguration()
        httpConfiguration.addCustomizer new SecureRequestCustomizer()

        // SSL Connector
        def sslConnector = new ServerConnector(
                server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(httpConfiguration)
        )
        sslConnector.setPort(0)

        // Start the server
        def statusCode = HttpServletResponse.SC_OK
        def responseContent = "The is the plain text test body data.\n".getBytes(CHARSET_UTF8)
        def contentType = "text/plain;charset=utf-8"
        // Make this the only endpoint for the server
        server.connectors = [sslConnector] as Connector[]
        server.handler = new AbstractHandler() {
            @Override
            void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                response.status = statusCode
                response.contentType = contentType
                baseRequest.handled = true
                response.outputStream.write responseContent
            }
        }
        server.start()
        def serverPort = ((ServerConnector) server.connectors[0]).getLocalPort()

        // Add the client creds to the connection pool
        poolType.setKeystoreFilename(HttpConnectionPoolProviderTest.class.getResource("/client.jks").getFile())
        poolType.setKeystorePassword("password")
        poolType.setKeyPassword("password")
        poolType.setTruststoreFilename(HttpConnectionPoolProviderTest.class.getResource("/server.jks").getFile())
        poolType.setTruststorePassword("password")

        def configRoot = new File(HttpConnectionPoolProviderTest.class.getResource("/client.jks").getFile()).getParent()
        DefaultHttpClient client = HttpConnectionPoolProvider.genClient(configRoot, poolType) as DefaultHttpClient
        def httpGet = new HttpGet("https://localhost:" + serverPort)
        def httpResponse = client.execute(httpGet)

        assert httpResponse.statusLine.statusCode == statusCode
        assert httpResponse.entity.contentType.value == contentType
        assert Arrays.equals(httpResponse.entity.content.bytes, responseContent)
    }

    @Test
    public void "should create a pool that can connect to a server with client auth using single keystore when configured"() {
        server = new Server()

        // SSL Context Factory
        def sslContextFactory = new SslContextFactory()
        sslContextFactory.keyStoreResource = Resource.newClassPathResource("single.jks")
        sslContextFactory.keyStorePassword = "password"
        sslContextFactory.keyManagerPassword = "password"
        sslContextFactory.needClientAuth = true

        // SSL HTTP Configuration
        def httpConfiguration = new HttpConfiguration()
        httpConfiguration.addCustomizer new SecureRequestCustomizer()

        // SSL Connector
        def sslConnector = new ServerConnector(
                server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(httpConfiguration)
        )
        sslConnector.setPort(0)

        // Start the server
        def statusCode = HttpServletResponse.SC_OK
        def responseContent = "The is the plain text test body data.\n".getBytes(CHARSET_UTF8)
        def contentType = "text/plain;charset=utf-8"
        // Make this the only endpoint for the server
        server.connectors = [sslConnector] as Connector[]
        server.handler = new AbstractHandler() {
            @Override
            void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                response.status = statusCode
                response.contentType = contentType
                baseRequest.handled = true
                response.outputStream.write responseContent
            }
        }
        server.start()
        def serverPort = ((ServerConnector) server.connectors[0]).getLocalPort()

        // Add the client creds to the connection pool
        poolType.setKeystoreFilename(HttpConnectionPoolProviderTest.class.getResource("/single.jks").getFile())
        poolType.setKeystorePassword("password")
        poolType.setKeyPassword("password")

        def configRoot = new File(HttpConnectionPoolProviderTest.class.getResource("/single.jks").getFile()).getParent()
        DefaultHttpClient client = HttpConnectionPoolProvider.genClient(configRoot, poolType) as DefaultHttpClient
        def httpGet = new HttpGet("https://localhost:" + serverPort)
        def httpResponse = client.execute(httpGet)

        assert httpResponse.statusLine.statusCode == statusCode
        assert httpResponse.entity.contentType.value == contentType
        assert Arrays.equals(httpResponse.entity.content.bytes, responseContent)
    }
}

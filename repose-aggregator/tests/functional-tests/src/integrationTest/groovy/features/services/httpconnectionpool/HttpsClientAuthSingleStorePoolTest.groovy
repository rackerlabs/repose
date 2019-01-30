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
package features.services.httpconnectionpool

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import scaffold.category.Services
import spock.lang.Shared

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.concurrent.TimeUnit

import static org.openrepose.framework.test.ReposeLauncher.MAX_STARTUP_TIME

@Category(Services)
class HttpsClientAuthSingleStorePoolTest extends ReposeValveTest {

    @Shared
    def Server server
    @Shared
    def File singleFile
    @Shared
    def statusCode = HttpServletResponse.SC_OK
    @Shared
    def responseContent = "The is the plain text test body data.\n".getBytes(Charset.forName("UTF-8"))
    @Shared
    def contentType = "text/plain;charset=utf-8"
    @Shared
    def identityEndpoint
    @Shared
    def MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        deproxy = new Deproxy()

        reposeLogSearch.cleanLog()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        // Have to manually copy binary files, because the applyConfigs() attempts to substitute template parameters
        // when they are found and it breaks everything. :(
        def singleFileOrig = new File(repose.configurationProvider.configTemplatesDir, "common/single.jks")
        singleFile = new File(repose.configDir, "single.jks")
        def singleFileDest = new FileOutputStream(singleFile)
        Files.copy(singleFileOrig.toPath(), singleFileDest)
        params.targetPort = startJettyServer()
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/clientauth/common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/clientauth/singlestore", params)

        fakeIdentityV2Service = new MockIdentityV2Service(params.identityPort, params.targetPort)
        identityEndpoint = deproxy.addEndpoint(params.identityPort, 'identity service', null, fakeIdentityV2Service.handler)

        repose.start()
        reposeLogSearch.awaitByString("Repose ready", 1, MAX_STARTUP_TIME, TimeUnit.SECONDS)
    }

    def startJettyServer() {
        server = new Server()

        // SSL Context Factory
        def sslContextFactory = new SslContextFactory()
        sslContextFactory.keyStorePath = singleFile.absolutePath
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

        // Start the server with only the one endpoint for the server
        server.connectors = [sslConnector] as Connector[]
        server.handler = new AbstractHandler() {
            @Override
            void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                if (request.getHeader("x-tenant-id").equals(fakeIdentityV2Service.client_tenantid)
                        && request.getHeader("x-tenant-name").equals(fakeIdentityV2Service.client_tenantname)
                ) {
                    response.status = statusCode
                    response.contentType = contentType
                    baseRequest.handled = true
                    response.outputStream.write responseContent
                } else {
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                }
            }
        }
        server.start()
        return ((ServerConnector) server.connectors[0]).getLocalPort()
    }

    def cleanupSpec() {
        server?.stop()
    }

    def "Execute a non-SSL request to Repose which will perform both Auth-N and communicate with the origin service using the default connection pool w/ Client Auth"() {
        //A simple request should go through
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = "mytenant"
            client_tenantname = "mytenantname"
        }
        def request = new HttpGet("http://localhost:$properties.reposePort")
        request.addHeader('X-Auth-Token', fakeIdentityV2Service.client_token)
        def client = HttpClients.createDefault()

        when:
        def response = client.execute(request)

        then:
        assert response.statusLine.statusCode == statusCode
        assert response.entity.contentType.value == contentType
        assert Arrays.equals(response.entity.content.bytes, responseContent)
    }
}

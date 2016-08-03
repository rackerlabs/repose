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

import framework.ReposeValveTest
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.util.ssl.SslContextFactory
import spock.lang.Shared

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class HttpsClientAuthSingleStoreTest extends ReposeValveTest {

    @Shared
    def Server server
    @Shared
    def File singleFile
    @Shared
    def statusCode = HttpServletResponse.SC_OK
    @Shared
    def responseContent = "The is the plain text test body data.\n".bytes
    @Shared
    def contentType = "text/plain;charset=utf-8"

    def setupSpec() {
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

        repose.start()
        reposeLogSearch.awaitByString("Repose ready", 1, 60, TimeUnit.SECONDS)
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
        def https_config = new HttpConfiguration()
        https_config.addCustomizer new SecureRequestCustomizer()

        // SSL Connector
        def sslConnector = new ServerConnector(
                server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(https_config)
        )
        sslConnector.setPort(0)

        // Start the server with only the one endpoint for the server
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
        return ((ServerConnector) server.connectors[0]).getLocalPort()
    }

    def cleanupSpec() {
        server?.stop()
    }

    def "Execute a non-SSL request to Repose which will in turn use the default connection pool w/ Client Auth to communicate with the origin service"() {
        //A simple request should go through
        given:
        def client = new DefaultHttpClient()

        when:
        def response = client.execute(new HttpGet("http://localhost:$properties.reposePort"))

        then:
        assert response.statusLine.statusCode == statusCode
        assert response.entity.contentType.value == contentType
        assert Arrays.equals(response.entity.content.bytes, responseContent)
    }
}

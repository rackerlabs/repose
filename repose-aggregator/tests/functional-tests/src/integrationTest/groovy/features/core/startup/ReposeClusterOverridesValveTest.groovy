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
package features.core.startup

import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.AbstractHandler
import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeValveTest
import spock.lang.Unroll

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class ReposeClusterOverridesValveTest extends ReposeValveTest {
    static String RESPONSE_BODY = "RESPONSE BODY"
    static char[] PASSWORD = "password".toCharArray()
    static int reposePort1
    static int reposePort2

    static org.eclipse.jetty.server.Server originService

    def setupSpec() {
        originService = new org.eclipse.jetty.server.Server(0)
        originService.setHandler(new AbstractHandler() {
            @Override
            void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                response.setContentType("text/plain")
                response.outputStream.println(RESPONSE_BODY)
                response.setStatus(HttpServletResponse.SC_OK)
                baseRequest.setHandled(true)
            }
        })
        originService.start()
        properties.targetPort = ((ServerConnector) originService.connectors[0]).localPort


        reposePort1 = properties.reposePort
        reposePort2 = PortFinder.instance.getNextOpenPort()

        def params = properties.getDefaultTemplateParams()
        params += [
                'reposePort1'      : reposePort1,
                'reposePort2'      : reposePort2,
                'repose.cluster.id': 'repose1',
                'repose.node.id'   : 'node1',
        ]

        repose.configurationProvider.applyConfigs("common", params)
        // Have to manually copy binary files, because the applyConfigs() attempts to substitute template parameters
        // when they are found and it breaks everything. :(
        def serverFileOrig = new File(configTemplates, "common/server.jks")
        def serverFile = new File(configDirectory, "server.jks")
        def serverFileDest = new FileOutputStream(serverFile)
        Files.copy(serverFileOrig.toPath(), serverFileDest)
        def clientFileOrig = new File(configTemplates, "common/client.jks")
        def clientFile = new File(configDirectory, "client.jks")
        def clientFileDest = new FileOutputStream(clientFile)
        Files.copy(clientFileOrig.toPath(), clientFileDest)
        def bogusFileOrig = new File(configTemplates, "common/bogus.jks")
        def bogusFile = new File(configDirectory, "bogus.jks")
        def bogusFileDest = new FileOutputStream(bogusFile)
        Files.copy(bogusFileOrig.toPath(), bogusFileDest)
        repose.configurationProvider.applyConfigs("features/core/startup/override", params)

        reposeLogSearch.cleanLog()

        repose.start(killOthersBeforeStarting: false, waitOnJmxAfterStarting: false)
        reposeLogSearch.awaitByString("node1 -- Repose ready", 1, 60, TimeUnit.SECONDS)
        reposeLogSearch.awaitByString("node2 -- Repose ready", 1, 60, TimeUnit.SECONDS)
    }

    @Unroll("When using client side keystore #keystore and truststore #truststore to access port #port should succeed.")
    def "Can execute a simple request via SSL"() {
        given:
        def keystoreFile = new File(configDirectory, keystore)
        def truststoreFile = new File(configDirectory, truststore)
        def sslContext = SSLContexts.custom()
                .loadKeyMaterial(keystoreFile, PASSWORD, PASSWORD) // Key this client is presenting.
                .loadTrustMaterial(truststoreFile, PASSWORD) // Key that is being accepted from server.
                .build()
        def sf = new SSLConnectionSocketFactory(
                sslContext,
                null,
                null,
                NoopHostnameVerifier.INSTANCE
        )
        def client = HttpClients.custom().setSSLSocketFactory(sf).build()

        when:
        def response = client.execute(new HttpGet("https://localhost:${port}"))

        then:
        response.getStatusLine().statusCode == HttpServletResponse.SC_OK
        response.getEntity().content.readLines().get(0).contains(RESPONSE_BODY)
        response.getFirstHeader("via").getValue().contains(via)

        where:
        keystore     | truststore   | port        | via
        "client.jks" | "server.jks" | reposePort1 | "repose1"
        "server.jks" | "client.jks" | reposePort2 | "repose2"
    }

    @Unroll("When using client side keystore #keystore and truststore #truststore to access port #port should fail.")
    def "Requests with an incorrect client certificate fail"() {
        given:
        def keystoreFile = new File(configDirectory, keystore)
        def truststoreFile = new File(configDirectory, truststore)
        def sslContext = SSLContexts.custom()
                .loadKeyMaterial(keystoreFile, PASSWORD, PASSWORD) // Key this client is presenting.
                .loadTrustMaterial(truststoreFile, PASSWORD) // Key that is being accepted from server.
                .build()
        def sf = new SSLConnectionSocketFactory(
                sslContext,
                null,
                null,
                NoopHostnameVerifier.INSTANCE
        )
        def client = HttpClients.custom().setSSLSocketFactory(sf).build()

        when:
        client.execute(new HttpGet("https://localhost:${port}"))

        then:
        thrown IOException

        where:
        keystore     | truststore  | port
        "client.jks" | "bogus.jks" | reposePort1
        "server.jks" | "bogus.jks" | reposePort2
    }

    def cleanupSpec() {
        repose?.stop()
        originService?.stop()
    }
}

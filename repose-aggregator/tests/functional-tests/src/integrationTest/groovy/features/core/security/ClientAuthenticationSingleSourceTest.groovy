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
package features.core.security

import framework.ReposeValveTest
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Created by adrian on 7/19/16.
 */
class ClientAuthenticationSingleSourceTest extends ReposeValveTest {

    def setupSpec() {
        reposeLogSearch.cleanLog()
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/security/clientauth", params)
        repose.configurationProvider.applyConfigs("features/core/security/clientauth/singlestore", params)

        //Have to manually copy the keystore, because the applyConfigs breaks everything :(
        def sourceKeystore = new File(repose.configurationProvider.configTemplatesDir, "features/core/security/clientauth/singlestore/keystore.jks")
        def keystoreFile = new File(repose.configDir, "keystore.jks")
        def destinationKeystore = new FileOutputStream(keystoreFile)
        Files.copy(sourceKeystore.toPath(), destinationKeystore)

        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        reposeLogSearch.awaitByString("Repose ready", 1, 60, TimeUnit.SECONDS)
    }

    def cleanupSpec() {
        deproxy?.shutdown()
        repose?.stop()
    }

    def "Can execute a simple request via SSL"() {
        //A simple request should go through
        given:
        def keystoreFile = new File(repose.configDir, "keystore.jks")
        def truststoreFile = new File(repose.configDir, "keystore.jks")

        def keystorePass = "buttsbuttsbutts"
        def truststorePass = "buttsbuttsbutts"

        def sslContext = SSLContexts.custom()
                .loadKeyMaterial(truststoreFile, truststorePass.toCharArray()) // Key this client is presenting.
                .loadTrustMaterial(keystoreFile, keystorePass.toCharArray(), TrustSelfSignedStrategy.INSTANCE) // Key that is being accepted from server.
                .build()
        def sf = new SSLConnectionSocketFactory(
                sslContext,
                null,
                null,
                NoopHostnameVerifier.INSTANCE
        )
        def client = HttpClients.custom().setSSLSocketFactory(sf).build()

        when:
        def response = client.execute(new HttpGet("https://localhost:$properties.reposePort"))

        then:
        response.getStatusLine().statusCode == 200
    }

    def "Requests without a client certificate fail"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint)

        then:
        mc.receivedResponse.code == "401"
    }
}

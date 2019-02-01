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

import org.apache.http.client.ClientProtocolException
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Core

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import static org.openrepose.framework.test.ReposeLauncher.MAX_STARTUP_TIME

@Category(Core)
class SSLClientAuthenticationTest extends ReposeValveTest {

    def setupSpec() {
        reposeLogSearch.cleanLog()
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/security/clientauth", params)

        // Have to manually copy binary files, because the applyConfigs() attempts to substitute template parameters
        // when they are found and it breaks everything. :(
        def serverFileOrig = new File(repose.configurationProvider.configTemplatesDir, "common/server.jks")
        def serverFileDest = new FileOutputStream(new File(repose.configDir, "server.jks"))
        Files.copy(serverFileOrig.toPath(), serverFileDest)
        def clientFileOrig = new File(repose.configurationProvider.configTemplatesDir, "common/client.jks")
        def clientFileDest = new FileOutputStream(new File(repose.configDir, "client.jks"))
        Files.copy(clientFileOrig.toPath(), clientFileDest)

        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        reposeLogSearch.awaitByString("Repose ready", 1, MAX_STARTUP_TIME, TimeUnit.SECONDS)
    }

    def "Can execute a simple request via SSL"() {
        //A simple request should go through
        given:
        def serverFile = new File(repose.configDir, "server.jks")
        def clientFile = new File(repose.configDir, "client.jks")

        def serverPass = "password".toCharArray()
        def clientPass = "password".toCharArray()

        def sslContext = SSLContexts.custom()
                .loadKeyMaterial(clientFile, clientPass, clientPass) // Key this client is presenting.
                .loadTrustMaterial(serverFile, serverPass) // Key that is being accepted from server.
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
        HttpClients.createDefault()
                .execute(new HttpGet(reposeEndpoint))

        then:
        thrown ClientProtocolException
    }
}

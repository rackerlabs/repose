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

import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

import java.nio.file.Files

/**
 * Make sure we can start up with SSL configuration parameters
 */
class SSLConfigurationStartup extends ReposeValveTest {

    def setupSpec() {
        cleanLogDirectory()
        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/security/simplessl", params)

        // Have to manually copy binary files, because the applyConfigs() attempts to substitute template parameters
        // when they are found and it breaks everything. :(
        def serverFileOrig = new File(repose.configurationProvider.configTemplatesDir, "common/server.jks")
        def serverFileDest = new FileOutputStream(new File(repose.configDir, "server.jks"))
        Files.copy(serverFileOrig.toPath(), serverFileDest)

        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    static def params

    @Unroll
    def 'Can execute a simple request via SSL using TLSv1.2 and #cipher'() {
        given:
        def sslContext = SSLContexts.custom().loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE).build()
        def sf = new SSLConnectionSocketFactory(
            sslContext,
            (String[]) ['TLSv1.2'],
            (String[]) [cipher],
            NoopHostnameVerifier.INSTANCE
        )
        def client = HttpClients.custom().setSSLSocketFactory(sf).build()
        def get = new HttpGet("https://localhost:$properties.reposePort")

        when:
        def response = client.execute(get)

        then:
        response.getStatusLine().statusCode == 200

        where:
        cipher << [
            'TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA',
            'TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256',
            'TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256',
            'TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA',
            'TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384',
            'TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384'
        ]
    }

    @Unroll
    def "Can't execute a simple request via SSL using #protocol and #cipher"() {
        given:
        def sslContext = SSLContexts.custom().loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE).build()
        def sf = new SSLConnectionSocketFactory(
            sslContext,
            (String[]) [protocol],
            (String[]) [cipher],
            NoopHostnameVerifier.INSTANCE
        )
        def client = HttpClients.custom().setSSLSocketFactory(sf).build()
        def get = new HttpGet("https://localhost:$properties.reposePort")

        when:
        client.execute(get)

        then:
        thrown IOException

        where:
        protocol     | cipher
        'SSLv3'      | 'SSL_DHE_RSA_WITH_DES_CBC_SHA'
        'TLSv1'      | 'TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA'
        'TLSv1.1'    | 'TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA'
    }
}

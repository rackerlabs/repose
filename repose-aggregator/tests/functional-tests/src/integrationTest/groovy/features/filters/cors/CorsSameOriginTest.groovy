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
package features.filters.cors

import framework.ReposeValveTest
import org.apache.http.HttpResponse
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.apache.http.util.EntityUtils
import org.rackspace.deproxy.ClientConnector
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.DeproxyHttpRequest
import org.rackspace.deproxy.Header
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.RequestParams
import org.rackspace.deproxy.Response
import spock.lang.Shared

import javax.servlet.http.HttpServletResponse

class CorsSameOriginTest extends ReposeValveTest {

    @Shared
    int reposeSslPort

    @Shared
    String reposeSslEndpoint

    def setupSpec() {
        reposeLogSearch.cleanLog()

        reposeSslPort = PortFinder.Singleton.getNextOpenPort()
        reposeSslEndpoint = "https://$properties.targetHostname:$reposeSslPort"

        def params = properties.getDefaultTemplateParams() + [reposeSslPort: reposeSslPort]
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/cors/sameorigin", params)

        // add self-signed SSL certificate support to Deproxy
        CloseableHttpClient client = HttpClients.custom()
                .setSSLSocketFactory(new SSLConnectionSocketFactory(
                        SSLContexts.custom().loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE).build(),
                        NoopHostnameVerifier.INSTANCE))
                .build()
        ClientConnector sslConnector = { Request request, boolean https, def host, def port, RequestParams requestParams ->
            def scheme = https ? 'https' : 'http'
            def deproxyRequest = new DeproxyHttpRequest(request, scheme, host as String, port)

            HttpResponse response = client.execute(deproxyRequest)

            def body
            if (response.entity.contentType != null &&
                    response.entity.contentType.value.toLowerCase().startsWith("text/")) {
                body = EntityUtils.toString(response.getEntity())
            } else {
                body = EntityUtils.toByteArray(response.getEntity())
            }

            new Response(response.statusLine.statusCode,
                    response.statusLine.reasonPhrase,
                    response.getAllHeaders().collect { new Header(it.getName(), it.getValue()) },
                    body)
        }

        deproxy = new Deproxy(null, sslConnector)
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def "Can execute a simple request via SSL using Deproxy"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeSslEndpoint, method: "GET")

        then:
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_OK
    }

    def "Can execute a simple request using Deproxy"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "GET")

        then:
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_OK
    }
}

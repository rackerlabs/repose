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
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.http.CorsHttpHeader
import org.rackspace.deproxy.ClientConnector
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.DeproxyHttpRequest
import org.rackspace.deproxy.Header
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.RequestParams
import org.rackspace.deproxy.Response
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Unroll

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
            if (response.entity?.contentType?.value?.toLowerCase()?.startsWith("text/")) {
                body = EntityUtils.toString(response.entity)
            } else if (response.entity) {
                body = EntityUtils.toByteArray(response.entity)
            } else {
                body = null
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

    @Ignore
    @Unroll
    def "Request considered non-CORS due to no Origin header with URI scheme '#scheme', method '#method', Host '#host', X-Forwarded-Host '#forwardedHost'"() {
        given: "the correct Repose endpoint is used depending on which scheme (http or https) we want to use"
        def endpoint = (scheme == "https") ? reposeSslEndpoint : reposeEndpoint

        and: "the headers are set for Host and maybe X-Forwarded-Host but not Origin"
        def headers = [(CommonHttpHeader.HOST.toString()): host]
        if (forwardedHost) {
            headers += [(CommonHttpHeader.X_FORWARDED_HOST.toString()): forwardedHost]
        }

        when:
        MessageChain mc = deproxy.makeRequest(url: endpoint, method: method, headers: headers)

        then: "the response status is OK"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_OK

        and: "the request makes it to the origin service"
        mc.getHandlings().size() == 1

        and: "none of the CORS headers are added to the response"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString()).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString()).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS.toString()).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS.toString()).isEmpty()

        and: "the Vary header is set"
        mc.receivedResponse.headers.contains("Vary")

        where:
        [scheme, method, host, forwardedHost] <<
                [["http", "https"],
                 ["GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "TRACE", "OPTIONS"],
                 ["openrepose.com", "openrepose.com:8080", "test.repose.site", "dev.repose.site:8443", "10.8.8.8", "10.8.4.4:9090", "[2001:db8:cafe::17]", "[2001:db8:cafe::17]:5555"],
                 [null, "test.repose.site", "dev.repose.site:8080, test.repose.site", "10.8.8.8", "10.8.4.4:9090", "[2001:db8:cafe::17]", "[2001:db8:cafe::17]:5555"]]
                        .combinations()
    }

    @Unroll
    def "Request considered non-CORS due to Origin '#origin' matching Host '#host' with URI scheme '#scheme', method '#method'"() {
        given: "the correct Repose endpoint is used depending on which scheme (http or https) we want to use"
        def endpoint = (scheme == "https") ? reposeSslEndpoint : reposeEndpoint

        and: "the headers are set for Host and Origin to match explicitly"
        def headers = [
                (CommonHttpHeader.HOST.toString()): host,
                (CorsHttpHeader.ORIGIN.toString()): origin]

        when:
        MessageChain mc = deproxy.makeRequest(url: endpoint, method: method, headers: headers)

        then: "the response status is OK"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_OK

        and: "the request makes it to the origin service"
        mc.getHandlings().size() == 1

        and: "none of the CORS headers are added to the response"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString()).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString()).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS.toString()).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS.toString()).isEmpty()

        and: "the Vary header is set"
        mc.receivedResponse.headers.contains("Vary")

        where:
        scheme  | method   | host                       | origin
        "http"  | "GET"    | "[2001:db8:cafe::17]:5555" | "http://[2001:db8:cafe::17]:5555"
        "https" | "HEAD"   | "[2001:db8:cafe::17]"      | "https://[2001:db8:cafe::17]"
        "http"  | "POST"   | "10.8.8.8:9090"            | "http://10.8.8.8:9090"
        "https" | "PUT"    | "10.8.4.4"                 | "https://10.8.4.4"
        "http"  | "PATCH"  | "cors.not.allowed:8080"    | "http://cors.not.allowed:8080"
        "https" | "DELETE" | "cors.not.allowed"         | "https://cors.not.allowed"
    }

    @Unroll
    def "Request considered non-CORS due to Origin '#origin' matching X-Forwarded-Host '#forwardedHost' with URI scheme '#scheme', method '#method', Host 'origin.service:9090'"() {
        given: "the correct Repose endpoint is used depending on which scheme (http or https) we want to use"
        def endpoint = (scheme == "https") ? reposeSslEndpoint : reposeEndpoint

        and: "the headers are set for X-Forwarded-Host and Origin to match explicitly and Host to some other value"
        def headers = [
                (CommonHttpHeader.X_FORWARDED_HOST.toString()): forwardedHost,
                (CorsHttpHeader.ORIGIN.toString())            : origin,
                (CommonHttpHeader.HOST.toString())            : "origin.service:9090"]

        when:
        MessageChain mc = deproxy.makeRequest(url: endpoint, method: method, headers: headers)

        then: "the response status is OK"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_OK

        and: "the request makes it to the origin service"
        mc.getHandlings().size() == 1

        and: "none of the CORS headers are added to the response"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString()).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString()).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS.toString()).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS.toString()).isEmpty()

        and: "the Vary header is set"
        mc.receivedResponse.headers.contains("Vary")

        where:
        scheme  | method    | forwardedHost              | origin
        "http"  | "GET"     | "[2001:db8:cafe::17]:5555" | "http://[2001:db8:cafe::17]:5555"
        "https" | "HEAD"    | "[2001:db8:cafe::17]"      | "https://[2001:db8:cafe::17]"
        "http"  | "POST"    | "10.8.8.8:9090"            | "http://10.8.8.8:9090"
        "https" | "PUT"     | "10.8.4.4"                 | "https://10.8.4.4"
        "http"  | "TRACE"   | "cors.not.allowed:8080"    | "http://cors.not.allowed:8080"
        "https" | "OPTIONS" | "cors.not.allowed"         | "https://cors.not.allowed"
    }

    @Unroll
    def "Request considered CORS actual request due to Origin '#origin' not matching X-Forwarded-Host 'will.not.match:3030' despite Host '#host' matching with URI scheme '#scheme', method '#method'"() {
        given: "the correct Repose endpoint is used depending on which scheme (http or https) we want to use"
        def endpoint = (scheme == "https") ? reposeSslEndpoint : reposeEndpoint

        and: "the headers are set for Host and Origin to match explicitly but X-Forwarded-Host to some other value"
        def headers = [
                (CommonHttpHeader.X_FORWARDED_HOST.toString()): "will.not.match:3030",
                (CommonHttpHeader.HOST.toString())            : host,
                (CorsHttpHeader.ORIGIN.toString())            : origin]

        when:
        MessageChain mc = deproxy.makeRequest(url: endpoint, method: method, headers: headers)

        then: "the response status is OK"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_OK

        and: "the request makes it to the origin service"
        mc.getHandlings().size() == 1

        and: "the CORS headers for an actual request are added"
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()) == origin
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString()) == 'true'

        and: "the CORS headers for a preflight request are not added"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString()).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS.toString()).isEmpty()

        and: "the Vary header is set"
        mc.receivedResponse.headers.contains("Vary")

        where:
        scheme  | method    | host                       | origin
        "http"  | "GET"     | "[2001:db8:cafe::17]:5555" | "http://[2001:db8:cafe::17]:5555"
        "https" | "HEAD"    | "[2001:db8:cafe::17]"      | "https://[2001:db8:cafe::17]"
        "http"  | "POST"    | "10.8.8.8:9090"            | "http://10.8.8.8:9090"
        "https" | "PUT"     | "10.8.4.4"                 | "https://10.8.4.4"
        "http"  | "PATCH"   | "test.repose.site"         | "http://test.repose.site"
        "https" | "DELETE"  | "dev.repose.site:8443"     | "https://dev.repose.site:8443"
        "http"  | "TRACE"   | "openrepose.com"           | "http://test.repose.site"
        "https" | "OPTIONS" | "openrepose.com:9443"      | "https://openrepose.com:9443"
    }

    @Unroll
    def "Request considered CORS preflight request due to Origin '#origin' not matching X-Forwarded-Host 'will.not.match:3030' despite Host '#host' matching with URI scheme '#scheme', Access-Control-Request-Method '#requestedMethod'"() {
        given: "the correct Repose endpoint is used depending on which scheme (http or https) we want to use"
        def endpoint = (scheme == "https") ? reposeSslEndpoint : reposeEndpoint

        and: "the headers are set for Host and Origin to match explicitly but X-Forwarded-Host to some other value"
        def headers = [
                (CommonHttpHeader.X_FORWARDED_HOST.toString())           : "will.not.match:3030",
                (CommonHttpHeader.HOST.toString())                       : host,
                (CorsHttpHeader.ORIGIN.toString())                       : origin,
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString()): requestedMethod]

        when:
        MessageChain mc = deproxy.makeRequest(url: endpoint, method: "OPTIONS", headers: headers)

        then: "the response status is OK"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_OK

        and: "the request does not make it to the origin service"
        mc.getHandlings().isEmpty()

        and: "the CORS headers for a preflight request are added"
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()) == origin
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString()).size() == 1
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString()).tokenize(',').contains(requestedMethod)
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString()) == 'true'

        and: "the 'Access-Control-Allow-Headers' header is not set since none were requested"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS.toString()).isEmpty()

        and: "the CORS headers for an actual request are not added"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS.toString()).isEmpty()

        and: "the 'Vary' header is set with the correct values for an OPTIONS request"
        mc.receivedResponse.headers.contains("Vary")
        mc.receivedResponse.headers.findAll("Vary") == ['origin', 'access-control-request-headers', 'access-control-request-method']

        where:
        scheme  | requestedMethod | host                       | origin
        "http"  | "GET"           | "[2001:db8:cafe::17]:5555" | "http://[2001:db8:cafe::17]:5555"
        "https" | "HEAD"          | "[2001:db8:cafe::17]"      | "https://[2001:db8:cafe::17]"
        "http"  | "POST"          | "10.8.8.8:9090"            | "http://10.8.8.8:9090"
        "https" | "PUT"           | "10.8.4.4"                 | "https://10.8.4.4"
        "http"  | "PATCH"         | "test.repose.site"         | "http://test.repose.site"
        "https" | "DELETE"        | "dev.repose.site:8443"     | "https://dev.repose.site:8443"
        "http"  | "TRACE"         | "openrepose.com"           | "http://test.repose.site"
        "https" | "OPTIONS"       | "openrepose.com:9443"      | "https://openrepose.com:9443"
    }
/*
    @Unroll
    def "Only the first X-Forwarded-Host #forwardedHost will be used when determining if request is a same-origin request"() {
        //
    }

    @Unroll
    def "URI Scheme #scheme and X-Forwarded-Host/Host #host will match Origin #origin and be considered a same-origin request"() {
        //
        "http"  | "OPTIONS" | "[2001:db8:cafe::17]:5555" | "http://[2001:db8:cafe::17]:5555"
        "http"  | "OPTIONS" | "[2001:db8:cafe::17]:5555" | "http://[2001:db8:cafe:0:0:0:0:17]:5555"
        "http"  | "OPTIONS" | "[2001:db8:cafe::17]:5555" | "http://[2001:db8:cafe:0000:0000:0000:0000:0017]:5555"
    }

    @Unroll
    def "URI Scheme #scheme and X-Forwarded-Host/Host #host will NOT match Origin #origin and NOT be considered a same-origin request"() {
        //
    }*/
}

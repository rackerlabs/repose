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
package features.core.headers

import org.openrepose.framework.test.ReposeConfigurationProvider
import org.openrepose.framework.test.ReposeValveLauncher
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll


class SplitHeadersOnCommaTest extends ReposeValveTest {

    static int originServicePort
    static int reposePort
    static String url
    static ReposeConfigurationProvider reposeConfigProvider

    def setupSpec() {
        deproxy = new Deproxy()
        originServicePort = properties.targetPort
        deproxy.addEndpoint(originServicePort)

        reposePort = properties.reposePort
        url = "http://localhost:${reposePort}"

        reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configTemplates)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                url,
                properties.getConfigDirectory(),
                reposePort
        )
        repose.enableDebug()

        def params = properties.getDefaultTemplateParams()

        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/core/headers", params)

        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)

        repose.waitForNon500FromUrl(url)
    }

    @Unroll("Requests - headers defined by RFC: #headerName with \"#headerValue\" should split into #expectedCount parts")
    def "Requests - headers defined by the rfc as comma-separated lists should be split on commas in requests"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: url, headers: headers)

        then: "the request should make it to the origin service with the header appropriately split"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName(headerName) == expectedCount


        where:
        headerName         | headerValue                                                          | expectedCount
        "Accept"           | "text/plain"                                                         | 1
        "Accept"           | "text/plain, *"                                                      | 2
        "Accept"           | "text/plain;q=0.2, application/xml;q=0.8"                            | 2
        "Accept"           | "text/plain, application/xml, application/json"                      | 3
        "Accept-Charset"   | "ISO-8859-1"                                                         | 1
        "Accept-Charset"   | "ISO-8859-1, *"                                                      | 2
        "Accept-Charset"   | "US_ASCII, *"                                                        | 2
        "Accept-Charset"   | "UTF-8, US-ASCII, ISO-8859-1"                                        | 3
        "Accept-Language"  | "*"                                                                  | 1
        "Accept-Language"  | "en-US, *"                                                           | 2
        "Accept-Language"  | "en-US, en-gb"                                                       | 2
        "Accept-Language"  | "da, en-gb;q=0.8, en;q=0.7"                                          | 3
        "Accept-Language"  | "en, en-US, en-cockney, i-cherokee, x-pig-latin"                     | 5
        "Allow"            | ""                                                                   | 0
        "Allow"            | "GET"                                                                | 1
        "Allow"            | "GET, OPTIONS"                                                       | 2
        "Allow"            | "GET, POST, PUT, DELETE"                                             | 4
        "Cache-Control"    | "no-cache"                                                           | 1
        "Cache-Control"    | "max-age = 3600, max-stale"                                          | 2
        "Cache-Control"    | "max-age = 3600, max-stale = 2500, no-transform"                     | 3
        "Content-Encoding" | "identity"                                                           | 1
        "Content-Encoding" | "identity, identity"                                                 | 2
        "Content-Encoding" | "identity, identity, identity"                                       | 3
        "Content-Language" | "da"                                                                 | 1
        "Content-Language" | "mi, en"                                                             | 2
        "Content-Language" | "en, en-US, en-cockney, i-cherokee, x-pig-latin"                     | 5
        "Pragma"           | "no-cache"                                                           | 1
        "Pragma"           | "no-cache, x-pragma"                                                 | 2
        "Pragma"           | "no-cache, x-pragma-1, x-pragma-2"                                   | 3
        "Warning"          | "199 fred \"Misc. warning\""                                         | 1
        "Warning"          | "199 fred \"Misc. warning\", 199 ethel \"Another warning\""          | 2
        "WWW-Authenticate" | "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="                                 | 1
        "WWW-Authenticate" | "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==, Basic QWxhZGRpbjpwbGVhc2U/Cg==" | 1
        "Accept-Encoding"  | "identity"                                                           | 1
        "Accept-Encoding"  | "*"                                                                  | 1
        "Accept-Encoding"  | "compress, gzip"                                                     | 2
        "Accept-Encoding"  | "gzip;q=1.0, identity;q=0.5, *;q=0"                                  | 3
        "Accept-Encoding"  | "gzip;q=0.9, identity;q=0.5, *;q=0.1"                                | 3
        "Accept-Encoding"  | "gzip;q=0.9, *;q=0.1, identity;q=0.5"                                | 3
        "Accept-Encoding"  | "identity;q=0.5, gzip;q=0.9, *;q=0.1"                                | 3
        "Accept-Encoding"  | "gzip;q=0.9, identity;q=0.5"                                         | 2
        "Accept-Encoding"  | "gzip;q=0.9, identity;q=0.5 *;q=0.6"                                 | 2
        "Accept-Encoding"  | ""                                                                   | 0

        // Connection, Expect, Proxy-Authenticate, TE, Trailer, Transfer-Encoding, Upgrade are all hop-by-hop headers, and thus wouldn't be forwarded by a proxy

    }

    @Unroll("Requests - Via header with \"#headerValue\" should result in #expectedCount parts")
    def "Requests - Via header is special, because Repose will add one"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: url, headers: headers)

        then: "the request should make it to the origin service with the header appropriately split"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName(headerName) == expectedCount


        where:
        headerName | headerValue                                 | expectedCount
        "Via"      | "HTTP/2.0 pseudonym"                        | 2
        "Via"      | "HTTP/2.0 pseudonym (this, is, a, comment)" | 2
        "Via"      | "1.0 fred, 1.1 nowhere.com (Apache/1.1)"    | 2
        "Via"      | "1.0 ricky, 1.1 ethel, 1.1 fred, 1.0 lucy"  | 2
        "Via"      | "1.0 ricky, 1.1 mertz, 1.0 lucy"            | 2
    }

    @Unroll("Requests - headers defined by RFC: #headerName with \"#headerValue\" should not be split")
    def "Requests - headers not defined by the rfc as comma-separated lists should not be split on commas in requests"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        def mc = deproxy.makeRequest(url: url, headers: headers)

        then: "the request should make it to the origin service with the header appropriately split"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName(headerName) == 1

        where:
        headerName         | headerValue
        "User-Agent"       | "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:23.0) Gecko/20100101 Firefox/23.0"
        "Accept-Ranges"    | "bytes, something"
        "Content-type"     | "text/plain, application/json"
        "If-Match"         | "entity-tag-1, entity-tag-2"
        "If-None-Match"    | "entity-tag-1, entity-tag-2"
        "Server"           | "ServerSoft/1.2.3 libwww/9.53.7b2 (comment, comment, comment)"
        "Vary"             | "header-1, header-2, header-3"
        "WWW-Authenticate" | "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
        "WWW-Authenticate" | "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==, Basic QWxhZGRpbjpwbGVhc2U/Cg=="
    }

    @Unroll("Requests - headers not defined by RFC: #headerName with \"#headerValue\" should split into #expectedCount parts")
    def "Requests - headers not defined by the rfc, but still known to Repose to be comma-separated lists should be split on commas in requests"() {


        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        def mc = deproxy.makeRequest(url: url, headers: headers)

        then: "the request should make it to the origin service with the header appropriately split"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName(headerName) == expectedCount


        where:
        headerName    | headerValue            | expectedCount
        "X-PP-Groups" | "group1"               | 1
        "X-PP-Groups" | "group1,group2"        | 2
        "X-PP-Groups" | "group1,group2,group3" | 3

    }

    @Unroll("Requests - headers not defined by RFC: #headerName with \"#headerValue\" should not be split")
    def "Requests - headers not defined by the rfc, and not known to Repose should not be split on commas in requests"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        def mc = deproxy.makeRequest(url: url, headers: headers)

        then: "the request should make it to the origin service with the header appropriately split"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName(headerName) == 1

        where:
        headerName        | headerValue
        "X-Random-Header" | "Value1,Value2"
    }


    @Unroll("Responses - headers defined by RFC: #headerName with \"#headerValue\" should split into #expectedCount parts")
    def "Responses - headers defined by the rfc as comma-separated lists should be split on commas in requests"() {

        when:
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: url, defaultHandler: { new Response(200, null, headers) })

        then: "the response from the origin service should be returned to the client with the header appropriately split"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.getCountByName(headerName) == expectedCount


        where:
        headerName         | headerValue                                                          | expectedCount
        "Accept"           | "text/plain"                                                         | 1
        "Accept"           | "text/plain, *"                                                      | 2
        "Accept"           | "text/plain;q=0.2, application/xml;q=0.8"                            | 2
        "Accept"           | "text/plain, application/xml, application/json"                      | 3
        "Accept-Charset"   | "ISO-8859-1"                                                         | 1
        "Accept-Charset"   | "ISO-8859-1, *"                                                      | 2
        "Accept-Charset"   | "US_ASCII, *"                                                        | 2
        "Accept-Charset"   | "UTF-8, US-ASCII, ISO-8859-1"                                        | 3
        "Accept-Language"  | "*"                                                                  | 1
        "Accept-Language"  | "en-US, *"                                                           | 2
        "Accept-Language"  | "en-US, en-gb"                                                       | 2
        "Accept-Language"  | "da, en-gb;q=0.8, en;q=0.7"                                          | 3
        "Accept-Language"  | "en, en-US, en-cockney, i-cherokee, x-pig-latin"                     | 5
        "Allow"            | ""                                                                   | 0
        "Allow"            | "GET"                                                                | 1
        "Allow"            | "GET, OPTIONS"                                                       | 2
        "Allow"            | "GET, POST, PUT, DELETE"                                             | 4
        "Cache-Control"    | "no-cache"                                                           | 1
        "Cache-Control"    | "max-age = 3600, max-stale"                                          | 2
        "Cache-Control"    | "max-age = 3600, max-stale = 2500, no-transform"                     | 3
        "Content-Encoding" | "identity"                                                           | 1
        "Content-Encoding" | "identity, identity"                                                 | 2
        "Content-Encoding" | "identity, identity, identity"                                       | 3
        "Content-Language" | "da"                                                                 | 1
        "Content-Language" | "mi, en"                                                             | 2
        "Content-Language" | "en, en-US, en-cockney, i-cherokee, x-pig-latin"                     | 5
        "Pragma"           | "no-cache"                                                           | 1
        "Pragma"           | "no-cache, x-pragma"                                                 | 2
        "Pragma"           | "no-cache, x-pragma-1, x-pragma-2"                                   | 3
        "Warning"          | "199 fred \"Misc. warning\""                                         | 1
        "Warning"          | "199 fred \"Misc. warning\", 199 ethel \"Another warning\""          | 2
        "WWW-Authenticate" | "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="                                 | 1
        "WWW-Authenticate" | "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==, Basic QWxhZGRpbjpwbGVhc2U/Cg==" | 1
        "Accept-Encoding"  | "identity"                                                           | 1
        "Accept-Encoding"  | "*"                                                                  | 1
        "Accept-Encoding"  | "compress, gzip"                                                     | 2
        "Accept-Encoding"  | "gzip;q=1.0, identity;q=0.5, *;q=0"                                  | 3
        "Accept-Encoding"  | "gzip;q=0.9, identity;q=0.5, *;q=0.1"                                | 3
        "Accept-Encoding"  | "gzip;q=0.9, *;q=0.1, identity;q=0.5"                                | 3
        "Accept-Encoding"  | "identity;q=0.5, gzip;q=0.9, *;q=0.1"                                | 3
        "Accept-Encoding"  | "gzip;q=0.9, identity;q=0.5"                                         | 2
        "Accept-Encoding"  | "gzip;q=0.9, identity;q=0.5 *;q=0.6"                                 | 2
        "Accept-Encoding"  | ""                                                                   | 0

        // Connection, Expect, Proxy-Authenticate, TE, Trailer, Transfer-Encoding, Upgrade are all hop-by-hop headers, and thus wouldn't be forwarded by a proxy

    }

    @Unroll("Responses - Via header with \"#headerValue\" should not be split")
    def "Responses - Via header will not be split, but Repose will add to it"() {

        when:
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: url, defaultHandler: { new Response(200, null, headers) })

        then: "the response from the origin service should be returned to the client with the header appropriately split"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.getCountByName(headerName) == 1


        where:
        headerName | headerValue                                 | expectedCount
        "Via"      | "HTTP/2.0 pseudonym"                        | 1
        "Via"      | "HTTP/2.0 pseudonym (this, is, a, comment)" | 1
        "Via"      | "1.0 fred, 1.1 nowhere.com (Apache/1.1)"    | 1
        "Via"      | "1.0 ricky, 1.1 ethel, 1.1 fred, 1.0 lucy"  | 1
        "Via"      | "1.0 ricky, 1.1 mertz, 1.0 lucy"            | 1
    }

    @Unroll("Responses - headers defined by RFC: #headerName with \"#headerValue\" should not be split")
    def "Responses - headers not defined by the rfc as comma-separated lists should not be split on commas in requests"() {

        when:
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        def mc = deproxy.makeRequest(url: url, defaultHandler: { new Response(200, null, headers) })

        then: "the response from the origin service should be returned to the client without the header being split"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.getCountByName(headerName) == 1

        where:
        headerName      | headerValue
        "User-Agent"    | "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:23.0) Gecko/20100101 Firefox/23.0"
        "Accept-Ranges" | "bytes, something"
        "Content-type"  | "text/plain, application/json"
        "If-Match"      | "entity-tag-1, entity-tag-2"
        "If-None-Match" | "entity-tag-1, entity-tag-2"
        // TODO: Uncomment this as part of: https://repose.atlassian.net/browse/REP-5320
        //"Server"        | "ServerSoft/1.2.3 libwww/9.53.7b2 (comment, comment, comment)"
        "Vary"          | "header-1, header-2, header-3"
    }

    @Unroll("Responses - headers not defined by RFC: #headerName with \"#headerValue\" should split into #expectedCount parts")
    def "Responses - headers not defined by the rfc, but still known to Repose to be comma-separated lists should be split on commas in requests"() {

        when:
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        def mc = deproxy.makeRequest(url: url, defaultHandler: { new Response(200, null, headers) })

        then: "the response from the origin service should be returned to the client with the header appropriately split"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.getCountByName(headerName) == expectedCount


        where:
        headerName    | headerValue            | expectedCount
        "X-PP-Groups" | "group1"               | 1
        "X-PP-Groups" | "group1,group2"        | 2
        "X-PP-Groups" | "group1,group2,group3" | 3

    }

    @Unroll("Responses - headers not defined by RFC: #headerName with \"#headerValue\" should not be split")
    def "Responses - headers not defined by the rfc, and not known to Repose should not be split on commas in requests"() {

        when:
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        def mc = deproxy.makeRequest(url: url, defaultHandler: { new Response(200, null, headers) })

        then: "the response from the origin service should be returned to the client without the header being split"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.getCountByName(headerName) == 1

        where:
        headerName        | headerValue
        "X-Random-Header" | "Value1,Value2"
    }
}

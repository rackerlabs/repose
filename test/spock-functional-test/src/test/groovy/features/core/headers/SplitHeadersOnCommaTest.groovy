package features.core.headers

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.HeaderCollection
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.PortFinder
import spock.lang.Unroll


class SplitHeadersOnCommaTest extends ReposeValveTest {

    static int originServicePort
    static int reposePort
    static int shutdownPort
    static String url
    static ReposeConfigurationProvider reposeConfigProvider

    def setupSpec() {
        deproxy = new Deproxy()
        originServicePort = PortFinder.Singleton.getNextOpenPort()
        deproxy.addEndpoint(originServicePort)

        reposePort = PortFinder.Singleton.getNextOpenPort()
        shutdownPort = PortFinder.Singleton.getNextOpenPort()
        url = "http://localhost:${reposePort}"

        reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configSamples)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                url,
                properties.getConfigDirectory(),
                reposePort,
                shutdownPort
        )
        repose.enableDebug()

        def params = [
                'targetPort': originServicePort,
                'reposePort': reposePort,
        ]

        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigsRuntime("common")
        reposeConfigProvider.applyConfigsRuntime("features/core/headers", params)

        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)

        repose.waitForNon500FromUrl(url)
    }

    @Unroll("Headers defined by RFC: #headerName with \"#headerValue\" should split into #expectedCount parts")
    def "headers defined by the rfc as comma-separated lists should be split on commas in requests"() {

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
        "WWW-Authenticate" | "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==, Basic QWxhZGRpbjpwbGVhc2U/Cg==" | 2

        // Connection, Expect, Proxy-Authenticate, TE, Trailer, Transfer-Encoding, Upgrade are all hop-by-hop headers, and thus wouldn't be forwarded by a proxy

    }

    @Unroll("Via header with \"#headerValue\" should result in #expectedCount parts")
    def "Via header is special, because Repose will add one"() {

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
        "Via"      | "1.0 fred, 1.1 nowhere.com (Apache/1.1)"    | 3
        "Via"      | "1.0 ricky, 1.1 ethel, 1.1 fred, 1.0 lucy"  | 5
        "Via"      | "1.0 ricky, 1.1 mertz, 1.0 lucy"            | 4
    }


    @Unroll("Accept-Encoding header with \"#headerValue\" should result in #expectedResult parts")
    def "Accept-Encoding header is special, because Repose will pick values with highest qvalue"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: url, headers: headers)

        then: "the request should make it to the origin service with the header appropriately split"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName(headerName) == expectedCount
        mc.handlings[0].request.headers[headerName] == expectedResult


        where:
        headerName        | headerValue                           | expectedCount | expectedResult
        "Accept-Encoding" | "identity"                            | 1             | "identity"
        "Accept-Encoding" | "*"                                   | 1             | "*"
        "Accept-Encoding" | "compress, gzip"                      | 1             | "compress, gzip"
        "Accept-Encoding" | "gzip;q=1.0, identity;q=0.5, *;q=0"   | 1             | "gzip;q=1.0"
        "Accept-Encoding" | "gzip;q=0.9, identity;q=0.5, *;q=0.1" | 1             | "gzip;q=0.9"
        "Accept-Encoding" | "gzip;q=0.9, *;q=0.1, identity;q=0.5" | 1             | "gzip;q=0.9"
        "Accept-Encoding" | "identity;q=0.5, gzip;q=0.9, *;q=0.1" | 1             | "gzip;q=0.9"
        "Accept-Encoding" | "gzip;q=0.9, identity;q=0.5"          | 1             | "gzip;q=0.9"
        "Accept-Encoding" | "gzip;q=0.9, identity;q=0.5 *;q=0.6"  | 1             | "gzip;q=0.9"
        "Accept-Encoding" | ""                                    | 0             | null
    }

    @Unroll("Headers defined by RFC: #headerName with \"#headerValue\" should not be split")
    def "headers not defined by the rfc as comma-separated lists should not be split on commas in requests"() {

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
        headerName      | headerValue
        "User-Agent"    | "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:23.0) Gecko/20100101 Firefox/23.0"
        "Accept-Ranges" | "bytes, something"
        "Content-type"  | "text/plain, application/json"
        "If-Match"      | "entity-tag-1, entity-tag-2"
        "If-None-Match" | "entity-tag-1, entity-tag-2"
        "Server"        | "ServerSoft/1.2.3 libwww/9.53.7b2 (comment, comment, comment)"
        "Vary"          | "header-1, header-2, header-3"
    }

    @Unroll("Headers not defined by RFC: #headerName with \"#headerValue\" should split into #expectedCount parts")
    def "headers not defined by the rfc, but still known to Repose to be comma-separated lists should be split on commas in requests"() {


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

    @Unroll("Headers not defined by RFC: #headerName with \"#headerValue\" should not be split")
    def "headers not defined by the rfc, and not known to Repose should not be split on commas in requests"() {

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

    def cleanupSpec() {

        if (repose) {
            repose.stop()
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }


}

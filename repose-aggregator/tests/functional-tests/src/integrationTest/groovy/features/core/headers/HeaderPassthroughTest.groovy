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

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Ignore
import spock.lang.Unroll

class HeaderPassthroughTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()

        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/headers", params)

        repose.start(
            killOthersBeforeStarting: false,
            waitOnJmxAfterStarting: false)

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("Requests - headers defined by RFC: #headerName with \"#headerValue\" should pass through without modification")
    def "Requests - headers defined by the RFC as comma-separated lists should be passed through unchanged in requests"() {

        when: "make a request with the given header and value"
        def headers = [
            'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then: "the request should make it to the origin service with the header appropriately split"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName(headerName) == 1
        mc.handlings[0].request.headers.findAll(headerName).contains(headerValue)


        where:
        headerName         | headerValue
        "Accept"           | "text/plain"
        "Accept"           | "text/plain, *"
        "Accept"           | "text/plain;q=0.2, application/xml;q=0.8"
        "Accept"           | "text/plain, application/xml, application/json"
        "Accept-Charset"   | "ISO-8859-1"
        "Accept-Charset"   | "ISO-8859-1, *"
        "Accept-Charset"   | "US_ASCII, *"
        "Accept-Charset"   | "UTF-8, US-ASCII, ISO-8859-1"
        "Accept-Language"  | "*"
        "Accept-Language"  | "en-US, *"
        "Accept-Language"  | "en-US, en-gb"
        "Accept-Language"  | "da, en-gb;q=0.8, en;q=0.7"
        "Accept-Language"  | "en, en-US, en-cockney, i-cherokee, x-pig-latin"
        "Allow"            | ""
        "Allow"            | "GET"
        "Allow"            | "GET, OPTIONS"
        "Allow"            | "GET, POST, PUT, DELETE"
        "Cache-Control"    | "no-cache"
        "Cache-Control"    | "max-age = 3600, max-stale"
        "Cache-Control"    | "max-age = 3600, max-stale = 2500, no-transform"
        "Content-Encoding" | "identity"
        "Content-Encoding" | "identity, identity"
        "Content-Encoding" | "identity, identity, identity"
        "Content-Language" | "da"
        "Content-Language" | "mi, en"
        "Content-Language" | "en, en-US, en-cockney, i-cherokee, x-pig-latin"
        "Pragma"           | "no-cache"
        "Pragma"           | "no-cache, x-pragma"
        "Pragma"           | "no-cache, x-pragma-1, x-pragma-2"
        "Warning"          | "199 fred \"Misc. warning\""
        "Warning"          | "199 fred \"Misc. warning\", 199 ethel \"Another warning\""
        "WWW-Authenticate" | "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
        "WWW-Authenticate" | "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==, Basic QWxhZGRpbjpwbGVhc2U/Cg=="
        "Accept-Encoding"  | "identity"
        "Accept-Encoding"  | "*"
        "Accept-Encoding"  | "compress, gzip"
        "Accept-Encoding"  | "gzip;q=1.0, identity;q=0.5, *;q=0"
        "Accept-Encoding"  | "gzip;q=0.9, identity;q=0.5, *;q=0.1"
        "Accept-Encoding"  | "gzip;q=0.9, *;q=0.1, identity;q=0.5"
        "Accept-Encoding"  | "identity;q=0.5, gzip;q=0.9, *;q=0.1"
        "Accept-Encoding"  | "gzip;q=0.9, identity;q=0.5"
        "Accept-Encoding"  | "gzip;q=0.9, identity;q=0.5 *;q=0.6"
        "Accept-Encoding"  | ""

        // Connection, Expect, Proxy-Authenticate, TE, Trailer, Transfer-Encoding, Upgrade are all hop-by-hop headers, and thus wouldn't be forwarded by a proxy

    }

    @Unroll("Requests - Via header with \"#headerValue\" should have a value added for Repose")
    def "Requests - Via header is special, because Repose will add one"() {

        when: "make a request with the given header and value"
        def headers = [
            'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then: "the request should make it to the origin service with the header appropriately split"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName(headerName) == 2
        mc.handlings[0].request.headers.findAll(headerName).contains(headerValue)


        where:
        headerName | headerValue
        "Via"      | "HTTP/2.0 pseudonym"
        "Via"      | "HTTP/2.0 pseudonym (this, is, a, comment)"
        "Via"      | "1.0 fred, 1.1 nowhere.com (Apache/1.1)"
        "Via"      | "1.0 ricky, 1.1 ethel, 1.1 fred, 1.0 lucy"
        "Via"      | "1.0 ricky, 1.1 mertz, 1.0 lucy"
    }

    @Unroll("Requests - headers not defined by RFC: #headerName with \"#headerValue\" should pass through without modification")
    def "Requests - headers not defined by the RFC as comma-separated lists should be passed through unchanged in requests"() {

        when: "make a request with the given header and value"
        def headers = [
            'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        def mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then: "the request should make it to the origin service with the header appropriately split"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName(headerName) == 1
        mc.handlings[0].request.headers.getFirstValue(headerName) == headerValue

        where:
        headerName         | headerValue
        "User-Agent"       | "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:23.0) Gecko/20100101 Firefox/23.0"
        "user-agent"       | "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        "Accept-Ranges"    | "bytes, something"
        "Content-type"     | "text/plain, application/json"
        "If-Match"         | "entity-tag-1, entity-tag-2"
        "If-None-Match"    | "entity-tag-1, entity-tag-2"
        "Server"           | "ServerSoft/1.2.3 libwww/9.53.7b2 (comment, comment, comment)"
        "Vary"             | "header-1, header-2, header-3"
        "WWW-Authenticate" | "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
        "WWW-Authenticate" | "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==, Basic QWxhZGRpbjpwbGVhc2U/Cg=="
        "x-pp-user"        | "usertest1, usertest2, usertest3"
    }

    @Unroll("Requests - headers not defined by RFC: #headerName with \"#headerValue\" should pass through without modification")
    def "Requests - headers not defined by the RFC, but still known to Repose to be comma-separated lists should be passed through unchanged in requests"() {

        when: "make a request with the given header and value"
        def headers = [
            'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        def mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then: "the request should make it to the origin service with the header appropriately split"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName(headerName) == 1
        mc.handlings[0].request.headers.getFirstValue(headerName) == headerValue


        where:
        headerName    | headerValue
        "X-PP-Groups" | "group1"
        "X-PP-Groups" | "group1,group2"
        "X-PP-Groups" | "group1,group2,group3"
        "X-PP-Roles"  | "group1"
        "X-PP-Roles"  | "group1,group2"
        "X-PP-Roles"  | "group1,group2,group3"
    }

    @Unroll("Requests - headers not defined by RFC: #headerName with \"#headerValue\" should pass through without modification")
    def "Requests - headers not defined by the RFC, and not known to Repose should be passed through unchanged in requests"() {

        when: "make a request with the given header and value"
        def headers = [
            'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        def mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then: "the request should make it to the origin service with the header appropriately split"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName(headerName) == 1
        mc.handlings[0].request.headers.getFirstValue(headerName) == headerValue

        where:
        headerName        | headerValue
        "X-Random-Header" | "Value1,Value2"
    }


    @Unroll("Responses - headers defined by RFC: #headerName with \"#headerValue\" should pass through without modification")
    def "Responses - headers defined by the RFC as comma-separated lists should be passed through unchanged in responses"() {

        when:
        def headers = [
            'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, defaultHandler: { new Response(200, null, headers) })

        then: "the response from the origin service should be returned to the client with the header appropriately split"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.getCountByName(headerName) == 1
        mc.receivedResponse.headers.getFirstValue(headerName) == headerValue


        where:
        headerName         | headerValue
        "Accept"           | "text/plain"
        "Accept"           | "text/plain, *"
        "Accept"           | "text/plain;q=0.2, application/xml;q=0.8"
        "Accept"           | "text/plain, application/xml, application/json"
        "Accept-Charset"   | "ISO-8859-1"
        "Accept-Charset"   | "ISO-8859-1, *"
        "Accept-Charset"   | "US_ASCII, *"
        "Accept-Charset"   | "UTF-8, US-ASCII, ISO-8859-1"
        "Accept-Language"  | "*"
        "Accept-Language"  | "en-US, *"
        "Accept-Language"  | "en-US, en-gb"
        "Accept-Language"  | "da, en-gb;q=0.8, en;q=0.7"
        "Accept-Language"  | "en, en-US, en-cockney, i-cherokee, x-pig-latin"
        "Allow"            | ""
        "Allow"            | "GET"
        "Allow"            | "GET, OPTIONS"
        "Allow"            | "GET, POST, PUT, DELETE"
        "Cache-Control"    | "no-cache"
        "Cache-Control"    | "max-age = 3600, max-stale"
        "Cache-Control"    | "max-age = 3600, max-stale = 2500, no-transform"
        "Content-Encoding" | "identity"
        "Content-Encoding" | "identity, identity"
        "Content-Encoding" | "identity, identity, identity"
        "Content-Language" | "da"
        "Content-Language" | "mi, en"
        "Content-Language" | "en, en-US, en-cockney, i-cherokee, x-pig-latin"
        "Pragma"           | "no-cache"
        "Pragma"           | "no-cache, x-pragma"
        "Pragma"           | "no-cache, x-pragma-1, x-pragma-2"
        "Warning"          | "199 fred \"Misc. warning\""
        "Warning"          | "199 fred \"Misc. warning\", 199 ethel \"Another warning\""
        "WWW-Authenticate" | "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
        "WWW-Authenticate" | "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==, Basic QWxhZGRpbjpwbGVhc2U/Cg=="
        "Accept-Encoding"  | "identity"
        "Accept-Encoding"  | "*"
        "Accept-Encoding"  | "compress, gzip"
        "Accept-Encoding"  | "gzip;q=1.0, identity;q=0.5, *;q=0"
        "Accept-Encoding"  | "gzip;q=0.9, identity;q=0.5, *;q=0.1"
        "Accept-Encoding"  | "gzip;q=0.9, *;q=0.1, identity;q=0.5"
        "Accept-Encoding"  | "identity;q=0.5, gzip;q=0.9, *;q=0.1"
        "Accept-Encoding"  | "gzip;q=0.9, identity;q=0.5"
        "Accept-Encoding"  | "gzip;q=0.9, identity;q=0.5 *;q=0.6"
        "Accept-Encoding"  | ""

        // Connection, Expect, Proxy-Authenticate, TE, Trailer, Transfer-Encoding, Upgrade are all hop-by-hop headers, and thus wouldn't be forwarded by a proxy

    }

    @Unroll("Responses - Via header with \"#headerValue\" should not be split")
    def "Responses - Via header will not be split, but Repose will add to it"() {

        when:
        def headers = [
            'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, defaultHandler: { new Response(200, null, headers) })

        then: "the response from the origin service should be returned to the client with the header appropriately split"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.getCountByName(headerName) == 1
        mc.receivedResponse.headers.getFirstValue(headerName).contains(headerValue)


        where:
        headerName | headerValue
        "Via"      | "HTTP/2.0 pseudonym"
        "Via"      | "HTTP/2.0 pseudonym (this, is, a, comment)"
        "Via"      | "1.0 fred, 1.1 nowhere.com (Apache/1.1)"
        "Via"      | "1.0 ricky, 1.1 ethel, 1.1 fred, 1.0 lucy"
        "Via"      | "1.0 ricky, 1.1 mertz, 1.0 lucy"
    }

    @Unroll("Responses - headers defined by RFC: #headerName with \"#headerValue\" should pass through without modification")
    def "Responses - headers defined by the RFC as not being comma-separated lists should be passed through unchanged in responses"() {

        when:
        def headers = [
            'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        def mc = deproxy.makeRequest(url: reposeEndpoint, defaultHandler: { new Response(200, null, headers) })

        then: "the response from the origin service should be returned to the client without the header being split"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.getCountByName(headerName) == 1
        mc.receivedResponse.headers.getFirstValue(headerName) == headerValue

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

    @Unroll("Responses - headers not defined by RFC: #headerName with \"#headerValue\" should pass through without modification")
    def "Responses - headers not defined by the RFC, but still known to Repose to be comma-separated lists should be passed through unchanged in responses"() {

        when:
        def headers = [
            'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        def mc = deproxy.makeRequest(url: reposeEndpoint, defaultHandler: { new Response(200, null, headers) })

        then: "the response from the origin service should be returned to the client with the header appropriately split"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.getCountByName(headerName) == 1
        mc.receivedResponse.headers.getFirstValue(headerName) == headerValue


        where:
        headerName    | headerValue
        "X-PP-Groups" | "group1"
        "X-PP-Groups" | "group1,group2"
        "X-PP-Groups" | "group1,group2,group3"

    }

    def "Responses - headers not defined by the RFC, and not known to Repose should be passed through unchanged in responses"() {
        given:
        String headerName = "X-Random-Header"
        String headerValue = "Value1,Value2"

        when:
        def headers = [
            'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        def mc = deproxy.makeRequest(url: reposeEndpoint, defaultHandler: { new Response(200, null, headers) })

        then: "the response from the origin service should be returned to the client without the header being split"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.getCountByName(headerName) == 1
        mc.receivedResponse.headers.getFirstValue(headerName) == headerValue
    }

    /*
     *  HTTP/1.1 header field syntax is specified by: https://tools.ietf.org/html/rfc7230#section-3.2
     *  HTTP/1.1 ABNF is collected at: https://tools.ietf.org/html/rfc7230#appendix-B
     *  ABNF is specified by: https://tools.ietf.org/html/rfc5234
     *
     *  The most relevant ABNF is as follows:
     *  header-field = field-name ":" OWS field-value OWS
     *  field-name = token
     *  token = 1*tchar
     *  tchar = "!" / "#" / "$" / "%" / "&" / "'" / "*" / "+" / "-" / "." /
     *          "^" / "_" / "`" / "|" / "~" / DIGIT / ALPHA
     *  field-value = *( field-content / obs-fold )
     *  field-content = field-vchar [ 1*( SP / HTAB ) field-vchar ]
     *  field-vchar = VCHAR / obs-text
     *  obs-fold = CRLF 1*( SP / HTAB )
     *  obs-text = %x80-FF
     *  ALPHA = %x41-5A / %x61-7A ; A-Z / a-z
     *  DIGIT = %x30-39 ; 0-9
     *  HTAB = %x09 ; horizontal tab
     *  SP = %x20
     *  VCHAR = %x21-7E ; visible (printing) characters
     *
     *  TODO: Unfortunately, Deproxy's Header representation stores values as a String which prevents non-printable bytes to be tested at this time
     *  TODO: Add tests for obs-fold
     */
    @Unroll("Requests - the following header value should be passed through unchanged: #headerValue")
    def "Requests - header values containing characters allowed by HTTP/1.1 specification should be passed through unchanged"() {
        given:
        String testHeaderName = 'Test-Header'

        when:
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint,
            headers: [(testHeaderName): headerValue]
        )

        then:
        mc.receivedResponse.code.toInteger() == 200
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName(testHeaderName) == 1
        mc.handlings[0].request.headers.getFirstValue(testHeaderName) == headerValue

        where:
        headerValue << [
            // Start of zero byte values
            // The empty string, to account for a field-value with cardinality of 0
            '',
            // End of zero byte values

            // Start of one byte values
            // All VCHARs
            *(0x21..0x7E).collect { (it as char) as String },
            // All obs-text
            // TODO: *(0x80..0xFF).collect { (it as char) as String },
            // End of one byte values

            // Start of multiple byte values
            // field-vchar SP field-vchar
            'a' + (0x20 as char) + 'a',
            // field-vchar HTAB field-vchar
            'a' + (0x09 as char) + 'a',
            // 2field-vchar
            'aa',
            // Alternatively, every combination of 2field-vchar could be tested, but this generates 8836 tests
            // *(Collections.nCopies(2, (0x21..0x7E)).combinations().collect { bytes -> bytes.collect { it as char }.join('') }),
            // End of multiple byte values
        ]
    }

    @Unroll("Responses - the following header value should be passed through unchanged: #headerValue")
    def "Responses - header values containing characters allowed by HTTP/1.1 specification should be passed through unchanged"() {
        given:
        String testHeaderName = 'Test-Header'

        when:
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint,
            defaultHandler: {
                new Response(
                    200,
                    null,
                    [(testHeaderName): headerValue]
                )
            }
        )

        then:
        mc.receivedResponse.code.toInteger() == 200
        mc.handlings.size() == 1
        mc.receivedResponse.headers.getCountByName(testHeaderName) == 1
        mc.receivedResponse.headers.getFirstValue(testHeaderName) == headerValue

        where:
        headerValue << [
            // Start of zero byte values
            // The empty string, to account for a field-value with cardinality of 0
            '',
            // End of zero byte values

            // Start of one byte values
            // All VCHARs
            *(0x21..0x7E).collect { (it as char) as String },
            // All obs-text
            // TODO: *(0x80..0xFF).collect { (it as char) as String },
            // End of one byte values

            // Start of multiple byte values
            // field-vchar SP field-vchar
            'a' + (0x20 as char) + 'a',
            // field-vchar HTAB field-vchar
            'a' + (0x09 as char) + 'a',
            // 2field-vchar
            'aa',
            // Alternatively, every combination of 2field-vchar could be tested, but this generates 8836 tests
            // *(Collections.nCopies(2, (0x21..0x7E)).combinations().collect { bytes -> bytes.collect { it as char }.join('') }),
            // End of multiple byte values
        ]
    }

    def "Requests - header values containing characters not allowed by HTTP/1.1 specification should be cause failure"() {
        given:
        String testHeaderName = 'Test-Header'

        when:
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint,
            headers: [(testHeaderName): (0xC0 as char) as String]
        )

        then:
        mc.receivedResponse.code.toInteger() == 500
        mc.handlings.isEmpty()
    }

    @Ignore("Jetty drops the invalid character causing Deproxy to fail when parsing the header")
    def "Responses - header values containing characters not allowed by HTTP/1.1 specification should be sanitized"() {
        given:
        String testHeaderName = 'Test-Header'

        when:
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint,
            defaultHandler: {
                new Response(
                    200,
                    null,
                    [(testHeaderName): (0xC0 as char) as String]
                )
            }
        )

        then:
        mc.receivedResponse.code.toInteger() == 200
        mc.handlings.size() == 1
        mc.receivedResponse.headers.getCountByName(testHeaderName) == 1
        mc.receivedResponse.headers.getFirstValue(testHeaderName) == ''
    }
}

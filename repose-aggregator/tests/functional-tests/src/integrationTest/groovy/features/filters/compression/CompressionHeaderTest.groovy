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
package features.filters.compression

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream

@Category(Filters)
class CompressionHeaderTest extends ReposeValveTest {
    def static String content = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi pretium non mi ac " +
            "malesuada. Integer nec est turpis duis."
    def static byte[] gzipCompressedContent = compressGzipContent(content)
    def static byte[] deflateCompressedContent = compressDeflateContent(content)
    def static byte[] falseZip = content.getBytes()

    def static compressGzipContent(String content) {
        def ByteArrayOutputStream out = new ByteArrayOutputStream(content.length())
        def GZIPOutputStream gzipOut = new GZIPOutputStream(out)
        gzipOut.write(content.getBytes())
        gzipOut.close()
        byte[] compressedContent = out.toByteArray();
        out.close()
        return compressedContent
    }

    def static compressDeflateContent(String content) {
        Deflater deflater = new Deflater();
        deflater.setInput(content.getBytes());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(content.getBytes().length);

        deflater.finish();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer); // returns the generated code... index
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        byte[] output = outputStream.toByteArray();
        return output;
    }

    def String convertStreamToString(byte[] input) {
        return new Scanner(new ByteArrayInputStream(input)).useDelimiter("\\A").next();
    }

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/compression", params)
        repose.start()
    }

    @Unroll
    def "when a compressed request is sent to Repose, Content-Encoding header is removed after decompression (#encoding)"() {
        when:
        "the compressed content is sent to the origin service through Repose with encoding " + encoding
        def MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: ["Content-Encoding": encoding],
                requestBody: zippedContent)


        then: "the compressed content should be decompressed and the content-encoding header should be absent"
        mc.sentRequest.headers.contains("Content-Encoding")
        mc.handlings.size == 1
        !mc.handlings[0].request.headers.contains("Content-Encoding")
        mc.sentRequest.body == zippedContent
        convertStreamToString((byte[]) mc.handlings[0].request.body).equals(unzippedContent)


        where:
        encoding   | unzippedContent | zippedContent
        "gzip"     | content         | gzipCompressedContent
        "x-gzip"   | content         | gzipCompressedContent
        "deflate"  | content         | deflateCompressedContent
        "identity" | content         | content

    }

    @Unroll
    def "when a compressed request is sent to Repose, Content-Encoding header is not removed if decompression fails (#encoding, #responseCode, #handlings)"() {
        when:
        "the compressed content is sent to the origin service through Repose with encoding " + encoding
        def MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: ["Content-Encoding": encoding],
                requestBody: zippedContent)


        then: "the compressed content should be decompressed and the content-encoding header should be absent"
        mc.sentRequest.headers.contains("Content-Encoding")
        mc.handlings.size == handlings
        mc.receivedResponse.code == responseCode

        where:
        encoding   | unzippedContent | zippedContent | responseCode | handlings
        "gzip"     | content         | falseZip      | '400'        | 0
        "x-gzip"   | content         | falseZip      | '400'        | 0
        "deflate"  | content         | falseZip      | '500'        | 0
        "identity" | content         | falseZip      | '200'        | 1
    }

    @Unroll
    def "when an uncompressed request is sent to Repose, Content-Encoding header is never present (#encoding, #responseCode, #handlings)"() {
        when:
        "the compressed content is sent to the origin service through Repose with encoding " + encoding
        def MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: ["Content-Encoding": encoding],
                requestBody: zippedContent)


        then: "the compressed content should be decompressed and the content-encoding header should be absent"
        mc.sentRequest.headers.contains("Content-Encoding")
        mc.handlings.size == handlings
        mc.receivedResponse.code == responseCode

        where:
        encoding   | unzippedContent | zippedContent | responseCode | handlings
        "gzip"     | content         | content       | '400'        | 0
        "x-gzip"   | content         | content       | '400'        | 0
        "deflate"  | content         | content       | '500'        | 0
        "identity" | content         | content       | '200'        | 1
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def handler = { request -> return new Response(201, "Created", respHeaders, "") }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', defaultHandler: handler)

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "$reposeEndpoint/blah?a=b,c,d"
        mc.receivedResponse.headers.findAll("via").size() == 1
    }

    @Unroll("Requests - headers: #headerName with \"#headerValue\" keep its case")
    def "Requests - headers should keep its case in requests"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then: "the request should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains(headerName)
        mc.handlings[0].request.headers.getFirstValue(headerName) == headerValue


        where:
        headerName | headerValue
        "Accept"   | "text/plain"
        "ACCEPT"   | "text/PLAIN"
        "accept"   | "TEXT/plain;q=0.2"
        "aCCept"   | "text/plain"
        //"CONTENT-Encoding" | "identity"
        //"Content-ENCODING" | "identity"
        //"content-encoding" | "idENtItY"
        //"Content-Encoding" | "IDENTITY"
    }

    @Unroll("Responses - headers: #headerName with \"#headerValue\" keep its case")
    def "Responses - header keep its case in responses"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, defaultHandler: { new Response(200, null, headers) })

        then: "the response should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.contains(headerName)
        mc.receivedResponse.headers.getFirstValue(headerName) == headerValue


        where:
        headerName     | headerValue
        "x-auth-token" | "123445"
        "X-AUTH-TOKEN" | "239853"
        "x-AUTH-token" | "slDSFslk&D"
        "x-auth-TOKEN" | "sl4hsdlg"
        "CONTENT-Type" | "application/json"
        "Content-TYPE" | "application/json"
        //"content-type" | "application/xMl"
        //"Content-Type" | "APPLICATION/xml"
    }
    /*
        Check Accept-Encoding header is removed from request through compression filter
     */

    @Unroll("When request sending #acceptheader header #encoding is removed through compression filter")
    def "Check if Accept-encoding header is removed from request"() {
        when:
        "the content is sent to the origin service through Repose with accept-encoding " + encoding
        def headers = [
                acceptheader: encoding
        ]

        def MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: headers,
                requestBody: content, defaultHandler: { new Response(200, content, headers) })


        then: "the accept-encoding header should be absent"
        mc.sentRequest.headers.contains(acceptheader)
        mc.handlings.size == 1
        !mc.handlings[0].request.headers.contains(acceptheader)

        mc.handlings[0].request.body.toString() == unzippedContent
        mc.handlings[0].response.message.toString() == content

        where:
        acceptheader      | encoding   | unzippedContent
        "accept-encoding" | "gzip"     | content
        "Accept-encoding" | "x-gzip"   | content
        "Accept-Encoding" | "deflate"  | content
        "accept-Encoding" | "identity" | content
    }
    /*
        Check Accept-Encoding header is removed from request through compression filter
     */

    @Unroll("When GET request #acceptheader header #encoding is removed through compression filter")
    def "Check if GET request with Accept-encoding header is removed from request"() {
        when:
        "the content is sent to the origin service through Repose with accept-encoding " + encoding
        def headers = [
                'Content-Length': '0',
                acceptheader    : encoding
        ]

        def MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers,
                defaultHandler: { new Response(200, content, headers) })


        then: "the accept-encoding header should be absent"
        mc.sentRequest.headers.contains(acceptheader)
        mc.handlings.size == 1
        !mc.handlings[0].request.headers.contains(acceptheader)

        mc.handlings[0].response.message.toString() == content

        where:
        acceptheader      | encoding
        "accept-encoding" | "gzip"
        "Accept-encoding" | "x-gzip"
        "Accept-Encoding" | "deflate"
        "accept-Encoding" | "identity"
    }
}

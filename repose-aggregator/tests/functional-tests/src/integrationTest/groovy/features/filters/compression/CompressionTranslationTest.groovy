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

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream

class CompressionTranslationTest extends ReposeValveTest {
    def static String content = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi pretium non mi ac " +
            "malesuada. Integer nec est turpis duis."
    def
    static String xmlResponseWithExtEntities = "<?xml version=\"1.0\" standalone=\"no\" ?> <!DOCTYPE a [  <!ENTITY license_agreement SYSTEM \"http://www.mydomain.com/license.xml\"> ]>  <a><remove-me>test</remove-me>&quot;somebody&license_agreement;</a>"
    def
    static String xmlResponseWithXmlBomb = "<?xml version=\"1.0\"?> <!DOCTYPE lolz [   <!ENTITY lol \"lol\">   <!ENTITY lol2 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">   <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">   <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">   <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">   <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">   <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">   <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">   <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\"> ]> <lolz>&lol9;</lolz>"
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
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/compression/translation", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    @Unroll("Requesting with XML Entities: #reqBody")
    def "when translating request"() {

        when: "User sends requests through repose"
        def resp = deproxy.makeRequest(
                url: (String) reposeEndpoint,
                method: "GET",
                headers: ['content-type': 'application/xml', 'accept': 'application/xml'],
                requestBody: reqBody)

        then: "Response code should contain"
        resp.receivedResponse.code == "400"

        where:
        reqBody << [
                xmlResponseWithXmlBomb,
                xmlResponseWithExtEntities
        ]

    }

    @Unroll("Removing Content-Encoding Header from Request after passing through Compression: #encoding")
    def "when a compressed request is sent from Origin to Repose, Content-Encoding header is removed after decompression"() {
        when:
        "the compressed content is sent to the origin service through Repose with encoding " + encoding
        def MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: ["Content-Encoding": encoding],
                requestBody: zippedContent)


        then: "the compressed content should be decompressed and the content-encoding header should be absent"
        mc.sentRequest.headers.contains("Content-Encoding")
        mc.handlings.size == 1
        !mc.handlings[0].request.headers.contains("Content-Encoding")
        if (!encoding.equals("identity")) {
            assert (mc.sentRequest.body != mc.handlings[0].request.body)
            assert (convertStreamToString(mc.handlings[0].request.body).toString().equals(unzippedContent))
        } else {
            assert (mc.sentRequest.body == mc.handlings[0].request.body)
            assert (mc.handlings[0].request.body.toString().trim().equals(unzippedContent.trim()))
        }

        where:
        encoding   | unzippedContent | zippedContent
        "gzip"     | content         | gzipCompressedContent
        "x-gzip"   | content         | gzipCompressedContent
        "deflate"  | content         | deflateCompressedContent
        "identity" | content         | content

    }

    @Unroll("content-encoding: #encoding")
    def "when a decompressed request is sent from Origin to Repose when compression fails"() {
        given:
        def decompressedHandler = { request -> return new Response(200, "OK", zippedContent) }

        when:
        "the decompressed content is sent to the origin service through Repose with encoding " + encoding
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint, headers: ['accept-encoding': encoding], defaultHandler: decompressedHandler])


        then: "the compressed content should be compressed and the accept-encoding header should be absent"
        mc.handlings.size == 0
        mc.receivedResponse.code == response
        if (encoding != "identity") {
            assert (mc.receivedResponse.headers.contains("Content-Encoding"))
            assert (!mc.receivedResponse.headers.contains("Accept-Encoding"))
        }

        where:
        encoding  | unzippedContent | zippedContent            | response
        "gzip"    | content         | gzipCompressedContent    | "500"
        "x-gzip"  | content         | gzipCompressedContent    | "500"
        "deflate" | content         | deflateCompressedContent | "500"

    }

    @Unroll("encoding: #encoding response code: #responseCode")
    def "when a compressed request is sent to Repose, Content-Encoding header is not removed if decompression fails"() {
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
        "deflate"  | content         | falseZip      | '502'        | 0
        "identity" | content         | falseZip      | '200'        | 1
    }

    @Unroll("No compression when entity length is less than threshold. encoding: #encoding")
    def "when an decompressed request is sent from Origin to Repose, do not compress below threshold"() {
        given:
        def decompressedHandler = { request -> return new Response(200, "OK", [], "nocompress") }

        when:
        "the decompressed content is sent to the origin service through Repose with encoding " + encoding
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint, headers: ['accept-encoding': encoding], defaultHandler: decompressedHandler])


        then: "the compressed content should be compressed and the accept-encoding header should be absent"
        mc.handlings.size == 1
        !mc.receivedResponse.headers.contains("Content-Encoding")
        !mc.receivedResponse.headers.contains("Accept-Encoding")
        assert (mc.handlings[0].response.body == mc.receivedResponse.body)
        assert (mc.receivedResponse.body.toString().trim().equals("nocompress"))

        where:
        encoding << [
                "gzip",
                "x-gzip",
                "deflate",
                "identity"
        ]

    }

    @Unroll("Do not compress with user-agent 'test'. encoding: #encoding")
    def "when an decompressed request is sent from Origin to Repose, do not compress for excluded user agent specified in client request"() {
        given:
        def decompressedHandler = { request -> return new Response(200, "OK", ['User-Agent': 'test'], content) }

        when:
        "the decompressed content is sent to the origin service through Repose with encoding " + encoding
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint, headers: ['accept-encoding': encoding, 'User-Agent': 'test'], defaultHandler: decompressedHandler])


        then: "the compressed content should be compressed and the accept-encoding header should be absent"
        mc.handlings.size == 1
        !mc.receivedResponse.headers.contains("Content-Encoding")
        !mc.receivedResponse.headers.contains("Accept-Encoding")
        assert (mc.handlings[0].response.body == mc.receivedResponse.body)
        assert (mc.receivedResponse.body.toString().trim().equals(content))

        where:
        encoding << [
                "gzip",
                "x-gzip",
                "deflate",
                "identity"
        ]

    }

    @Unroll("Do not compress for content-type application/form encoding: #encoding")
    def "when an decompressed request is sent from Origin to Repose, do not compress for excluded content type"() {
        given:
        def decompressedHandler = { request -> return new Response(200, "OK", ['content-type': 'application/form'], content) }

        when:
        "the decompressed content is sent to the origin service through Repose with encoding " + encoding
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint, headers: ['accept-encoding': encoding], defaultHandler: decompressedHandler])


        then: "the compressed content should be compressed and the accept-encoding header should be absent"
        mc.handlings.size == 1
        !mc.receivedResponse.headers.contains("Content-Encoding")
        assert (((String) mc.handlings.get(0).response.body).getBytes() == mc.receivedResponse.body)
        mc.receivedResponse.body == content.getBytes()

        where:
        encoding << [
                "gzip",
                "x-gzip",
                "deflate",
                "identity"
        ]

    }

    @Unroll("Content-encoding header should not be present. encoding: #encoding")
    def "when an uncompressed request is sent to Repose, Content-Encoding header is never present"() {
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
        "deflate"  | content         | content       | '502'        | 0
        "identity" | content         | content       | '200'        | 1
    }
}

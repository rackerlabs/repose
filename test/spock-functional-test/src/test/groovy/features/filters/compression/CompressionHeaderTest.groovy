package features.filters.compression

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response
import spock.lang.Unroll

import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream

@Category(Slow.class)
class CompressionHeaderTest extends ReposeValveTest {
    def static String content = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi pretium non mi ac " +
            "malesuada. Integer nec est turpis duis."
    def static byte[] gzipCompressedContent = compressGzipContent(content)
    def static byte[] deflateCompressedContent = compressDeflateContent(content)
    def static byte[] falseZip = content.getBytes()

    def static compressGzipContent(String content)   {
        def ByteArrayOutputStream out = new ByteArrayOutputStream(content.length())
        def GZIPOutputStream gzipOut = new GZIPOutputStream(out)
        gzipOut.write(content.getBytes())
        gzipOut.close()
        byte[] compressedContent = out.toByteArray();
        out.close()
        return compressedContent
    }

    def static compressDeflateContent(String content)   {
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

    def String convertStreamToString(byte[] input){
        return new Scanner(new ByteArrayInputStream(input)).useDelimiter("\\A").next();
    }

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/compression")
        repose.start()
    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    @Unroll
    def "when a compressed request is sent to Repose, Content-Encoding header is removed after decompression (#encoding)"() {
        when: "the compressed content is sent to the origin service through Repose with encoding " + encoding
        def MessageChain mc = deproxy.makeRequest(reposeEndpoint, "POST", ["Content-Encoding" : encoding],
                zippedContent)


        then: "the compressed content should be decompressed and the content-encoding header should be absent"
        mc.sentRequest.headers.contains("Content-Encoding")
        mc.handlings.size == 1
        !mc.handlings[0].request.headers.contains("Content-Encoding")
        if(!encoding.equals("identity")) {
            mc.sentRequest.body != mc.handlings[0].request.body
            mc.handlings[0].request.body.toString().equals(unzippedContent)
        } else {
            mc.sentRequest.body == mc.handlings[0].request.body
            mc.handlings[0].request.body.toString().trim().equals(unzippedContent.trim())
        }

        where:
        encoding    | unzippedContent | zippedContent
        "gzip"      | content         | gzipCompressedContent
        "x-gzip"    | content         | gzipCompressedContent
        "deflate"   | content         | deflateCompressedContent
        "identity"  | content         | content

    }

    @Unroll
    def "when a compressed request is sent to Repose, Content-Encoding header is not removed if decompression fails (#encoding, #responseCode, #handlings)"() {
        when: "the compressed content is sent to the origin service through Repose with encoding " + encoding
        def MessageChain mc = deproxy.makeRequest(reposeEndpoint, "POST", ["Content-Encoding" : encoding],
                zippedContent)


        then: "the compressed content should be decompressed and the content-encoding header should be absent"
        mc.sentRequest.headers.contains("Content-Encoding")
        mc.handlings.size == handlings
        mc.receivedResponse.code == responseCode

        where:
        encoding    | unzippedContent | zippedContent | responseCode | handlings
        "gzip"      | content         | falseZip       | '400'        | 0
        "x-gzip"    | content         | falseZip       | '400'        | 0
        "deflate"   | content         | falseZip       | '500'        | 0
        "identity"  | content         | falseZip       | '200'        | 1
    }

    @Unroll
    def "when an uncompressed request is sent to Repose, Content-Encoding header is never present (#encoding, #responseCode, #handlings)"() {
        when: "the compressed content is sent to the origin service through Repose with encoding " + encoding
        def MessageChain mc = deproxy.makeRequest(reposeEndpoint, "POST", ["Content-Encoding" : encoding],
                zippedContent)


        then: "the compressed content should be decompressed and the content-encoding header should be absent"
        mc.sentRequest.headers.contains("Content-Encoding")
        mc.handlings.size == handlings
        mc.receivedResponse.code == responseCode

        where:
        encoding    | unzippedContent | zippedContent | responseCode | handlings
        "gzip"      | content         | content       | '400'        | 0
        "x-gzip"    | content         | content       | '400'        | 0
        "deflate"   | content         | content       | '500'        | 0
        "identity"  | content         | content       | '200'        | 1
    }

    def "Should not split request headers according to rfc"() {
        given:
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
            [
                    "user-agent": userAgentValue,
                    "x-pp-user": "usertest1, usertest2, usertest3",
                    "accept": "application/xml;q=1 , application/json;q=0.5"
            ]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: reqHeaders)

        then: "Repose should split"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 3
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 2
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def handler = { request -> return new Response(201, "Created", respHeaders, "") }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', defaultHandler: handler)
        def handling = mc.getHandlings()[0]

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "http://somehost.com/blah?a=b,c,d"
        mc.receivedResponse.headers.findAll("via").size() == 1
    }
}

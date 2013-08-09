package features.filters.compression

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Request
import org.rackspace.gdeproxy.Response
import spock.lang.Unroll

import java.util.zip.GZIPOutputStream
import java.util.zip.DeflaterOutputStream

class CompressionHeaderTest extends ReposeValveTest {
    def static String content = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi pretium non mi ac " +
            "malesuada. Integer nec est turpis duis."
    def static byte[] gzipCompressedContent = compressGzipContent(content)
    def static String deflateCompressedContent = compressDeflateContent(content)
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
        def ByteArrayOutputStream out = new ByteArrayOutputStream(content.length())
        def DeflaterOutputStream zipOut = new DeflaterOutputStream(out)
        zipOut.write(content.getBytes())
        zipOut.close()
        byte[] compressedContent = out.toByteArray();
        out.close()
        return compressedContent
    }



    def setup() {
        repose.applyConfigs("features/filters/compression")
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        sleep(4000)
    }

    def cleanup() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "when a compressed request is sent to Repose, Content-Encoding header is removed after decompression"() {
        when: "the compressed content is sent to the origin service through Repose with encoding " + encoding
        def MessageChain mc = deproxy.makeRequest(reposeEndpoint, "POST", ["Content-Encoding" : encoding],
                zippedContent)


        then: "the compressed content should be decompressed and the content-encoding header should be absent"
        mc.sentRequest.headers.contains("Content-Encoding")
        mc.handlings.size == 1
        mc.sentRequest.body != mc.handlings[0].request.body
        !mc.handlings[0].request.headers.contains("Content-Encoding")
        mc.handlings[0].request.body.equals(content)

        where:
        encoding    | unzippedContent | zippedContent
        "gzip"      | content         | gzipCompressedContent
        "x-gzip"    | content         | gzipCompressedContent
        "deflate"   | content         | deflateCompressedContent
        "x-deflate" | content         | deflateCompressedContent
        "identity"  | content         | content

    }

    def "when a compressed request is sent to Repose, Content-Encoding header is not removed if decompression fails"() {
        when: "the compressed content is sent to the origin service through Repose with encoding " + encoding
        def MessageChain mc = deproxy.makeRequest(reposeEndpoint, "POST", ["Content-Encoding" : encoding],
                zippedContent)


        then: "the compressed content should be decompressed and the content-encoding header should be absent"
        mc.sentRequest.headers.contains("Content-Encoding")
        mc.handlings.size == 1
        mc.sentRequest.body != mc.handlings[0].request.body
        mc.handlings[0].request.headers.contains("Content-Encoding")
        mc.handlings[0].request.body.equals(content)
        mc.receivedResponse.code == '400'

        where:
        encoding    | unzippedContent | zippedContent
        "gzip"      | content         | falseZip
        "x-gzip"    | content         | falseZip
        "deflate"   | content         | falseZip
        "x-deflate" | content         | falseZip
        "identity"  | content         | falseZip
    }

    def "when an uncompressed request is sent to Repose, Content-Encoding header is never present"() {
        when: "the compressed content is sent to the origin service through Repose with encoding " + encoding
        def MessageChain mc = deproxy.makeRequest(reposeEndpoint, "POST", ["Content-Encoding" : encoding],
                zippedContent)


        then: "the compressed content should be decompressed and the content-encoding header should be absent"
        mc.sentRequest.headers.contains("Content-Encoding")
        mc.handlings.size == 1
        mc.sentRequest.body != mc.handlings[0].request.body
        mc.handlings[0].request.headers.contains("Content-Encoding")
        mc.handlings[0].request.body.equals(content)
        mc.receivedResponse.code == '400'

        where:
        encoding    | unzippedContent | zippedContent
        "gzip"      | content         | content
        "x-gzip"    | content         | content
        "deflate"   | content         | content
        "x-deflate" | content         | content
        "identity"  | content         | content
    }
}

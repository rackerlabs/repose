package features.filters.compression

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Request
import org.rackspace.gdeproxy.Response

import java.util.zip.GZIPOutputStream

class CompressionHeaderTest extends ReposeValveTest {
    def setup() {
        repose.applyConfigs("features/filters/compression")
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
        deproxy.addEndpoint(10000, "origin service", "localhost", {Request request -> return new Response(200)})

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
        given: "generated gzip compressed content"
        def String content = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi pretium non mi ac " +
                "malesuada. Integer nec est turpis duis."
        def String compressedContent = ""
        def OutputStream out = new ByteArrayOutputStream()
        def OutputStream gzipOut = new GZIPOutputStream(out)
        gzipOut.write(content.getBytes())
        gzipOut.close();
        compressedContent = out.toString()

        when: "the compressed content is sent to the origin service through Repose"
        def MessageChain mc = deproxy.makeRequest( reposeEndpoint, "POST", ["Content-Encoding" : "gzip", "test" : "ce"],
                compressedContent )

        then: "the compressed content should be decompressed and the content-encoding header should be absent"
        mc.sentRequest.headers.contains("Content-Encoding")
        mc.handlings.size == 1
        !mc.handlings[0].request.headers.contains("Content-Encoding")
        mc.handlings[0].request.body.equals(content)
    }

    def "when a compressed request is sent to Repose, Content-Encoding header is not removed if decompression fails"() {

    }

    def "when an uncompressed request is sent to Repose, Content-Encoding header is never present"() {

    }

    // TODO Write tests for responses returned to Repose from the origin service
}

package features.filters.compression

import framework.ReposeValveTest
import framework.PortFinder
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Response
import spock.lang.Shared

import java.util.zip.GZIPOutputStream

class CompressionTest extends ReposeValveTest {
    @Shared def port

    def setupSpec() {
        def portFinder = new PortFinder()

        port = portFinder.getNextOpenPort()
        deproxy = new Deproxy()
        deproxy.addEndpoint( port, "compliantOrigin", "localhost", { request -> new Response(200) } )

        repose.applyConfigs( "features/filters/compression", "common/" )
        repose.start()
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "when a compressed request is sent to Repose, Content-Encoding header is removed after decompression"() {
        given:
        // Generate zipped content
        def content = "This is a test string"
        def compressedContent = ""
        def out = new ByteArrayOutputStream()
        def gzipOut = new GZIPOutputStream(out)
        gzipOut.write(content.getBytes())
        gzipOut.close();
        compressedContent = out.toString()

        when:
        // Send zipped content through Repose
        def mc = deproxy.makeRequest("http://localhost:${port}", "POST", ["Content-Encoding": "gzip"], compressedContent)

        then:
        // Content is received by the origin service unzipped
        // The request received by the origin service does not have a Content-Encoding header
        mc.sentRequest.headers.contains("Content-Encoding")
        mc.handlings.size == 1
        !mc.handlings[0].request.headers.contains("Content-Encoding")
        mc.handlings[0].request.body.equals(content)
    }

    def "when a compressed request is sent to Repose, Content-Encoding header is not removed if decompression fails"() {
        /*given:

        when:

        then:*/
    }

    def "when an uncompressed request is sent to Repose, Content-Encoding header is never present"() {
        /*given:

        when:

        then:*/
    }

    // TODO Write tests for responses returned to Repose from the origin service
}

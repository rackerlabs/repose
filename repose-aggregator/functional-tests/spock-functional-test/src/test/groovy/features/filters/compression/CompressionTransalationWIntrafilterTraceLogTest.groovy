package features.filters.compression
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream
/**
 * Created by jennyvo on 7/24/15.
 * Verify test fail with intrafilter log with trace - REP-2470
 * Fix have to make test pass
 */
class CompressionTransalationWIntrafilterTraceLogTest extends ReposeValveTest {
    def static String content = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi pretium non mi ac " +
            "malesuada. Integer nec est turpis duis."
    def static byte[] gzipCompressedContent = compressGzipContent(content)
    def static byte[] deflateCompressedContent = compressDeflateContent(content)

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
        repose.configurationProvider.applyConfigs("features/filters/compression/translation/intrafiltertrace", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    @Unroll("content-encoding: #encoding")
    def "Test with intrafilter trace when a decompressed request is sent from Origin to Repose when compression fails"() {
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
}


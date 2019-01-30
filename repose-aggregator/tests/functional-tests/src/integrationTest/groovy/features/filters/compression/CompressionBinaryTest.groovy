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

import org.apache.http.HttpStatus
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import scaffold.category.Filters
import scaffold.category.Slow
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

import static features.filters.compression.CompressionHeaderTest.*

@Category(Filters)
class CompressionBinaryTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/compression", params)
        repose.configurationProvider.applyConfigs("features/filters/compression/binary", params)
        repose.start()
    }

    @Unroll("A GET request with Accept-Encoding header set to #encoding is honored on the output")
    def "Check if GET request with Accept-Encoding header set to #encoding is honored on the output"() {
        when:
        "the content is sent to the origin service through Repose with Accept-Encoding " + encoding
        def MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: 'GET',
                headers: [
                        'Content-Length' : '0',
                        'Accept-Encoding': encoding
                ],
                defaultHandler: { new Response(HttpStatus.SC_OK, "OK", null, content) }
        )

        then: "the uncompressed response from the origin service should be compressed before reaching the client"
        mc.handlings[0].response.body.toString() == content
        if (mc.receivedResponse.body instanceof byte[]) {
            Arrays.equals((byte[]) mc.receivedResponse.body, (byte[]) zippedContent)
        } else {
            assert (zippedContent instanceof String)
            assert (mc.receivedResponse.body == zippedContent)
        }

        where:
        encoding   | zippedContent
        "gzip"     | gzipCompressedContent
        "x-gzip"   | gzipCompressedContent
        "deflate"  | deflateCompressedContent
        "identity" | content
    }
}

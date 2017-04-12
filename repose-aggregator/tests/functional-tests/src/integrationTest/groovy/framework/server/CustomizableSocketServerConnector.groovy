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

package framework.server

import groovy.util.logging.Log4j
import org.rackspace.deproxy.BodyWriter
import org.rackspace.deproxy.Endpoint
import org.rackspace.deproxy.HandlerContext
import org.rackspace.deproxy.HeaderWriter
import org.rackspace.deproxy.Response
import org.rackspace.deproxy.SocketServerConnector

/**
 * This class allows us to customize the response sent from Deproxy in ways a Handler cannot. Specifically, it lets us
 * return an HTTP/1.0 response (instead of an HTTP/1.1 response) when we need to.
 */
@Log4j
class CustomizableSocketServerConnector extends SocketServerConnector {
    static final String HTTP_1_0 = "HTTP/1.0"
    static final String HTTP_1_1 = "HTTP/1.1"

    String httpProtocol = HTTP_1_1

    CustomizableSocketServerConnector(Endpoint endpoint, int port) {
        super(endpoint, port)
    }

    void sendResponse(OutputStream outStream, Response response, HandlerContext context = null) {
        def writer = new PrintWriter(outStream, true)

        if (response.message == null) {
            response.message = ""
        }

        log.debug("Sending HTTP protocol $httpProtocol")

        writer.write("$httpProtocol ${response.code} ${response.message}")
        writer.write("\r\n")

        writer.flush()

        HeaderWriter.writeHeaders(outStream, response.headers)

        BodyWriter.writeBody(response.body, outStream, context?.usedChunkedTransferEncoding ?: false)

        log.trace("Finished sending response")
    }
}

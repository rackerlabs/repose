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
package org.openrepose.framework.test.mocks

import io.jaegertracing.thriftjava.Batch
import groovy.util.logging.Log4j
import org.apache.thrift.TDeserializer
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response

/**
 * Simulates responses from an Tracer Collector Service
 */
@Log4j
class MockTracerCollector {

    int port

    List<Batch> batches = new ArrayList<>()
    Closure<Response> handler = { Request request -> return handleRequest(request) }

    MockTracerCollector(int port) {
        resetHandlers()

        this.port = port
    }

    void resetHandlers() {
        handler = this.&handleRequest
    }

    // we can still use the `handler' closure even if handleRequest is overridden in a derived class
    Response handleRequest(Request request) {
        // fixme: when converting a String to a byte[] or a byte[] to a String, account for the character encoding
        byte[] requestBody = request.body instanceof String ?
            (request.body as String).getBytes() :
            request.body as byte[]
        String requestBodyString = new String(requestBody)

        log.debug("Tracer collector: $requestBodyString")
        println("Tracer collector: $requestBodyString")
        def headers = [:]

        // Deserialize the request body into a Batch object so that we can read the trace data
        // NOTE: The TDeserializer defaults to using a TBinaryProtocol.Factory, which is the protocol used by the Jaeger HttpSender
        // NOTE: If adapting this code for the tracer agent (i.e., Jaeger's UdpSender), construct TDeserializer with a new TCompactProtocol.Factory()
        TDeserializer deserializer = new TDeserializer()
        Batch batch = new Batch()
        deserializer.deserialize(batch, requestBody)
        batches.add(batch)

        return new Response(200, "OK", headers, request.body)
    }
}

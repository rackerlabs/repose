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

import groovy.util.logging.Log4j
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
/**
 * Simulates responses from an Tracer Collector Service
 */
@Log4j
class MockTracerCollector {

    MockTracerCollector(int port) {

        resetHandlers()

        this.port = port
    }

    int port

    void resetHandlers() {

        handler = this.&handleRequest
    }

    def handler = { Request request -> return handleRequest(request) }

    // we can still use the `handler' closure even if handleRequest is overridden in a derived class
    Response handleRequest(Request request) {
        String requestBody = request.getBody() instanceof String ?
            request.getBody() :
            new String(request.getBody() as byte[])


        log.debug("Tracer collector: $requestBody")
        println("Tracer collector: $requestBody")
        def headers = [:]

        return new Response(200, "OK", headers, request.body)
    }
}

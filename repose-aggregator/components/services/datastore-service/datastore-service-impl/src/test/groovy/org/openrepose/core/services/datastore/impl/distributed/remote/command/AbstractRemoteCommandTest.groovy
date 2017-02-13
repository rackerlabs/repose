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
package org.openrepose.core.services.datastore.impl.distributed.remote.command

import org.junit.Before
import org.junit.Test
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.http.ServiceClientResponse
import org.openrepose.commons.utils.logging.TracingHeaderHelper
import org.openrepose.commons.utils.logging.TracingKey
import org.openrepose.core.services.RequestProxyService
import org.openrepose.core.services.datastore.distributed.RemoteBehavior
import org.openrepose.core.services.datastore.impl.distributed.DatastoreHeader
import org.slf4j.MDC

class AbstractRemoteCommandTest {

    private AbstractRemoteCommand arc;

    @Before
    public void setUp() {
        arc = new AbstractRemoteCommand("", new InetSocketAddress(0), null, false) {
            @Override
            ServiceClientResponse execute(RequestProxyService proxyService, RemoteBehavior remoteBehavior) {
                return null
            }

            @Override
            Object handleResponse(ServiceClientResponse response) throws IOException {
                return null
            }
        }
        arc.setHostKey("hostKey")
    }

    @Test
    public void shouldContainAddedHeaders() {
        MDC.put(TracingKey.TRACING_KEY, "tracingKey")
        Map<String, String> headers = arc.getHeaders(RemoteBehavior.ALLOW_FORWARDING)

        assert (headers.get(DatastoreHeader.HOST_KEY).equals("hostKey"));
        assert (TracingHeaderHelper.getTraceGuid(headers.get(CommonHttpHeader.TRACE_GUID)).equals("tracingKey"));
        assert (headers.get(DatastoreHeader.REMOTE_BEHAVIOR).equals("ALLOW_FORWARDING"));
    }
}

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
package org.openrepose.core.services.datastore.impl.distributed.remote.command;

import org.junit.Test;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.core.services.datastore.impl.distributed.CacheRequest;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeleteTest {

    @Test
    public void shouldTargetCorrectDeletionUrl() throws UnknownHostException {
        final Delete deleteCommand = new Delete("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), null, false);

        assertEquals("Delete command must target expected URL", "http://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH + "object-key", deleteCommand.getUrl());
    }

    @Test
    public void shouldReturnTrueOnSuccess() throws Exception {
        final Delete deleteCommand = new Delete("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), null, false);

        // RemoteBehavior.ALLOW_FORWARDING
        final ServiceClientResponse response = mock(ServiceClientResponse.class);
        when(response.getStatus()).thenReturn(202);

        assertEquals("Delete command must communicate success on 202", Boolean.TRUE, deleteCommand.handleResponse(response));
    }

    @Test
    public void shouldReturnFalseOnFailure() throws Exception {
        final Delete deleteCommand = new Delete("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), null, false);

        final ServiceClientResponse response = mock(ServiceClientResponse.class);
        when(response.getStatus()).thenReturn(404);

        assertEquals("Delete command must communicate failure on response != 202", Boolean.FALSE, deleteCommand.handleResponse(response));
    }
}

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
package org.openrepose.core.services.datastore.impl.distributed.remote;

import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.commons.utils.proxy.ProxyRequestException;
import org.openrepose.core.services.RequestProxyService;
import org.openrepose.core.services.datastore.DatastoreOperationException;
import org.openrepose.core.services.datastore.distributed.RemoteBehavior;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class RemoteCommandExecutorTest {

    RequestProxyService mockRequestProxyService;
    RemoteCommandExecutor executor;

    @Before
    public void setup() throws Exception {
        mockRequestProxyService = mock(RequestProxyService.class);
        executor = new RemoteCommandExecutor(mockRequestProxyService);
    }

    @Test
    public void shouldPerformRemoteCommand() {
        final Object result = executor.execute(new TestableRemoteCommand(), RemoteBehavior.ALLOW_FORWARDING);
        assertEquals("Executor must return the remote command's response", Boolean.TRUE, result);
    }

    @Test(expected = DatastoreOperationException.class)
    public void shouldThrowDatastoreOperationExceptionsForIOExceptions() {
        executor.execute(new TestableRemoteCommand(true), RemoteBehavior.ALLOW_FORWARDING);
    }

    @Test(expected = RemoteConnectionException.class)
    public void shouldCatchProxyRequestExceptions() {
        executor.execute(new TestableRemoteCommand(new ProxyRequestException("foo", new IllegalArgumentException())), RemoteBehavior.ALLOW_FORWARDING);
    }

    private class TestableRemoteCommand implements RemoteCommand {

        RuntimeException exception;
        boolean throwsIOException = false;

        private TestableRemoteCommand() {
        }

        private TestableRemoteCommand(RuntimeException exception) {
            this.exception = exception;
        }

        private TestableRemoteCommand(boolean throwsIOException) {
            this.throwsIOException = throwsIOException;
        }

        @Override
        public ServiceClientResponse execute(RequestProxyService proxyService, RemoteBehavior remoteBehavior) {
            return mock(ServiceClientResponse.class);
        }

        @Override
        public Object handleResponse(ServiceClientResponse httpResponse) throws IOException {
            if (exception != null) {
                throw exception;
            }
            if (throwsIOException) {
                throw new IOException("foo", new IllegalArgumentException());
            }
            return Boolean.TRUE;
        }
    }
}

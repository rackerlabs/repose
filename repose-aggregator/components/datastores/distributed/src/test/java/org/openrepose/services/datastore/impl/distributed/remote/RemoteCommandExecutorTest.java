package org.openrepose.services.datastore.impl.distributed.remote;

import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.commons.utils.proxy.ProxyRequestException;
import org.openrepose.commons.utils.proxy.RequestProxyService;
import org.openrepose.services.datastore.DatastoreOperationException;
import org.openrepose.services.datastore.distributed.RemoteBehavior;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

public class RemoteCommandExecutorTest {

    String HOST_KEY = "host_key";
    RequestProxyService mockRequestProxyService;
    RemoteCommandExecutor executor;

    @Before
    public void setup() throws Exception {
        mockRequestProxyService = mock(RequestProxyService.class);
        executor = new RemoteCommandExecutor(mockRequestProxyService, HOST_KEY);
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

        private TestableRemoteCommand() {}

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

        @Override
        public void setHostKey(String hostKey) {
            assertEquals("Host key must be set in the remote command", HOST_KEY, hostKey);
        }
    }

}




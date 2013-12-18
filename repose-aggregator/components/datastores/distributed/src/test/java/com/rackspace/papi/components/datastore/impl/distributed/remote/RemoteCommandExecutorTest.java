package com.rackspace.papi.components.datastore.impl.distributed.remote;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.proxy.ProxyRequestException;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import com.rackspace.papi.components.datastore.DatastoreOperationException;
import com.rackspace.papi.components.datastore.distributed.RemoteBehavior;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RemoteCommandExecutorTest {

    String HOST_KEY = "host_key";
    RequestProxyService mockRequestProxyService;
    RemoteCommandExecutor executor;

    @Before
    public void standUp() throws Exception {
        final HttpResponse mockedResponse = mock(HttpResponse.class);
        final HttpClient mockedClient = mock(HttpClient.class);
        mockRequestProxyService = mock(RequestProxyService.class);

        when(mockedClient.execute(any(HttpRequestBase.class))).thenReturn(mockedResponse);

        executor = new RemoteCommandExecutor(mockRequestProxyService, HOST_KEY);
    }

    @Test
    public void shouldPerformRemoteCommand() {
        final Object result = executor.execute(new TestableRemoteCommand(), RemoteBehavior.ALLOW_FORWARDING);
        assertEquals("Executor must return the remote command's response", Boolean.TRUE, result);
    }

    @Test(expected = DatastoreOperationException.class)
    public void shouldThrowDatastoreOperationExceptionsForIOExceptions() {
        final Object result = executor.execute(new TestableRemoteCommand(true), RemoteBehavior.ALLOW_FORWARDING);
        assertEquals("Executor must return the remote command's response", Boolean.TRUE, result);
    }

    @Test(expected = RemoteConnectionException.class)
    public void shouldCatchProxyRequestExceptions() {
        final Object result = executor.execute(new TestableRemoteCommand(new ProxyRequestException("foo", new IllegalArgumentException())), RemoteBehavior.ALLOW_FORWARDING);
        assertEquals("Executor must return the remote command's response", Boolean.TRUE, result);
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




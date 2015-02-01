package org.openrepose.services.datastore.impl.distributed.remote.command;

import org.junit.Assert;
import org.junit.Test;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.commons.utils.io.ObjectSerializer;
import org.openrepose.services.datastore.DatastoreOperationException;
import org.openrepose.services.datastore.distributed.SerializablePatch;
import org.openrepose.services.datastore.impl.distributed.CacheRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PatchTest {

    @Test
    public void getUrl_shouldTargetCorrectPatchUrl() throws UnknownHostException {
        //final Patch patchCommand = new Patch("object-key",
        // new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));
        final int ttl = 30;
        final String key = "someKey";
        final Patch patchCommand = new Patch(TimeUnit.MINUTES,
             new TestPatch(), ttl, key,
             new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

        Assert.assertEquals("Get command must target expected URL", "http://127.0.0.1:1000" +
             CacheRequest.CACHE_URI_PATH + key, patchCommand.getUrl());
    }

    @Test
    public void processResponse_shouldReturnPatchedValueOnSuccess() throws Exception {
        final int ttl = 30;
        final Patch patchCommand = new Patch(TimeUnit.MINUTES, new TestPatch(),
             ttl, "somekey", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

        // RemoteBehavior.ALLOW_FORWARDING
        final ServiceClientResponse response = mock(ServiceClientResponse.class);
        final byte[] responseData = ObjectSerializer.instance().writeObject("Response Data");

        ByteArrayInputStream bt = new ByteArrayInputStream(responseData);

        when(response.getData()).thenReturn(bt);
        when(response.getStatus()).thenReturn(200);

        Assert.assertEquals("Patch command must return value on 200", "Response Data",
             patchCommand.handleResponse(response));
    }

    @Test(expected = DatastoreOperationException.class)
    public void processResponse_shouldThrowExeptionOnUnauthorized() throws Exception {
        final int ttl = 30;
        final Patch patchCommand = new Patch(TimeUnit.MINUTES, new TestPatch(),
             ttl, "somekey", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));
        final ServiceClientResponse response = mock(ServiceClientResponse.class);
        when(response.getStatus()).thenReturn(HttpServletResponse.SC_UNAUTHORIZED);

        patchCommand.handleResponse(response);
    }

    public static class TestPatch implements SerializablePatch<String> {
        @Override
        public String newFromPatch() {
            throw new UnsupportedOperationException("org.openrepose.services.datastore.impl.distributed.remote.command.PatchTest.TestPatch.newFromPatch hasn't been written yet");
        }
    }
}

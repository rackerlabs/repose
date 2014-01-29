package com.rackspace.papi.components.datastore.impl.distributed.remote.command;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.components.datastore.DatastoreOperationException;
import com.rackspace.papi.components.datastore.distributed.SerializablePatch;
import com.rackspace.papi.components.datastore.impl.distributed.CacheRequest;
import org.junit.Assert;
import org.junit.Test;

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
        when(response.getStatusCode()).thenReturn(200);

        Assert.assertEquals("Patch command must return value on 200", "Response Data",
             patchCommand.handleResponse(response));
    }

    @Test(expected = DatastoreOperationException.class)
    public void processResponse_shouldThrowExeptionOnUnauthorized() throws Exception {
        final int ttl = 30;
        final Patch patchCommand = new Patch(TimeUnit.MINUTES, new TestPatch(),
             ttl, "somekey", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));
        final ServiceClientResponse response = mock(ServiceClientResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatusCode.UNAUTHORIZED.intValue());

        patchCommand.handleResponse(response);
    }

    public static class TestPatch implements SerializablePatch<String> {
        @Override
        public String newFromPatch() {
            throw new UnsupportedOperationException("com.rackspace.papi.components.datastore.impl.distributed.remote.command.PatchTest.TestPatch.newFromPatch hasn't been written yet");
        }
    }
}

package org.openrepose.services.datastore.impl.distributed.remote;

import org.openrepose.commons.utils.encoding.UUIDEncodingProvider;
import org.openrepose.services.datastore.api.Datastore;
import org.openrepose.services.datastore.api.DatastoreOperationException;
import org.openrepose.services.datastore.api.StringValue;
import org.openrepose.services.datastore.api.distributed.ClusterView;
import org.openrepose.services.datastore.api.distributed.RemoteBehavior;
import org.openrepose.services.datastore.api.hash.MD5MessageDigestFactory;
import org.openrepose.services.datastore.impl.distributed.DatastoreAction;
import org.openrepose.services.datastore.impl.distributed.HashRingDatastore;
import org.openrepose.services.datastore.impl.distributed.remote.RemoteCommandExecutor;
import org.openrepose.services.datastore.impl.distributed.remote.RemoteConnectionException;
import org.openrepose.services.datastore.impl.distributed.remote.command.Get;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static org.openrepose.services.datastore.api.distributed.RemoteBehavior.ALLOW_FORWARDING;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * A unit test suite for the Hash Ring Datastore
 */
public class HashRingDatastoreTest {

    protected Datastore localDatastore;
    protected ClusterView clusterView;
    protected DatastoreAction datastoreAction;
    protected HashRingDatastore hashRingDatastore;
    protected RemoteCommandExecutor remoteCommandExecutor;

    private InetSocketAddress inetSocketAddress;

    @Before
    public void standUp() throws Exception {
        inetSocketAddress = new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 1, 1, 11}), 2200);

        localDatastore = mock(Datastore.class);
        clusterView = mock(ClusterView.class);
        datastoreAction = mock(DatastoreAction.class);
        remoteCommandExecutor = mock(RemoteCommandExecutor.class);

        when(clusterView.hasDamagedMembers()).thenReturn(false);

        hashRingDatastore = spy(new HashRingDatastore(remoteCommandExecutor, clusterView, "", localDatastore,
                MD5MessageDigestFactory.getInstance(), UUIDEncodingProvider.getInstance()));
    }

    @Test
    public void shouldCatchDOExceptionFromIsRemoteTarget() throws Exception {
        when(clusterView.members()).thenReturn(
                new InetSocketAddress[]{inetSocketAddress}, new InetSocketAddress[]{});
        when(localDatastore.get(any(String.class))).thenThrow(new DatastoreOperationException("")).thenReturn(Boolean.FALSE);

        hashRingDatastore.get("", new byte[]{0}, ALLOW_FORWARDING);

        verify(remoteCommandExecutor, never()).execute(any(Get.class), any(RemoteBehavior.class));
        verify(clusterView).memberDamaged(eq(inetSocketAddress), any(String.class));
    }

    @Test
    public void shouldCatchDOExceptionFromPerformRemote() throws Exception {
        when(clusterView.members()).thenReturn(
                new InetSocketAddress[]{inetSocketAddress}, new InetSocketAddress[]{});
        when(localDatastore.get(any(String.class))).thenReturn(Boolean.FALSE);
        when(remoteCommandExecutor.execute(any(Get.class), any(RemoteBehavior.class))).
                thenThrow(new DatastoreOperationException(""));

        hashRingDatastore.get("", new byte[]{0}, ALLOW_FORWARDING);

        verify(clusterView).memberDamaged(eq(inetSocketAddress), any(String.class));
    }

    @Test
    public void shouldCatchRCExceptionFromPerformRemote() throws Exception {
        when(clusterView.members()).thenReturn(
                new InetSocketAddress[]{inetSocketAddress}, new InetSocketAddress[]{});
        when(localDatastore.get(any(String.class))).thenReturn(Boolean.FALSE);
        when(remoteCommandExecutor.execute(any(Get.class), any(RemoteBehavior.class))).
                thenThrow(new RemoteConnectionException("", new RuntimeException()));

        hashRingDatastore.get("", new byte[]{0}, ALLOW_FORWARDING);

        verify(clusterView).memberDamaged(eq(inetSocketAddress), any(String.class));
    }

    @Test
    public void getName_returnsExpectedName() throws Exception {
        assertThat(hashRingDatastore.getName(), equalTo(HashRingDatastore.DATASTORE_NAME));
    }

    @Test
    public void shouldPatchNewElement() throws Exception {
        String key = "key-one";
        byte[] id = new byte[] { 1, 2, 3};
        String value = "1, 2, 3";

        when(clusterView.members()).thenReturn(
                new InetSocketAddress[]{inetSocketAddress}, new InetSocketAddress[]{});
        when(clusterView.isLocal(any(InetSocketAddress.class))).thenReturn(true);
        StringValue.Patch patch = new StringValue.Patch(value);
        when(localDatastore.patch(eq(key), same(patch), eq(5), eq(DAYS))).thenReturn(new StringValue(value));
        StringValue patchedValue = (StringValue)hashRingDatastore.patch(key, id, patch, 5, DAYS, ALLOW_FORWARDING);
        verifyZeroInteractions(remoteCommandExecutor);
        verify(localDatastore).patch(any(String.class), any(StringValue.Patch.class), anyInt(), any(TimeUnit.class));
        assertThat(patchedValue.getValue(), equalTo(value));
    }

    @Test
    public void shouldPatchExistingElement() throws Exception {
        String key = "key-one";
        byte[] id = new byte[] { 1, 2, 3};
        String value = "1, 2, 3";
        String newValue = ", 4, 5";
        StringValue.Patch secondPatch = new StringValue.Patch(newValue);

        when(clusterView.members()).thenReturn(
                new InetSocketAddress[]{inetSocketAddress}, new InetSocketAddress[]{});
        when(clusterView.isLocal(any(InetSocketAddress.class))).thenReturn(true);
        when(localDatastore.patch(eq(key), same(secondPatch), eq(5), eq(DAYS))).thenReturn(new StringValue("1, 2, 3, 4, 5"));
        hashRingDatastore.patch(key, id, new StringValue.Patch(value), 5, DAYS, ALLOW_FORWARDING);
        StringValue patchedValue = (StringValue)hashRingDatastore.patch(key, id, secondPatch, 5, DAYS, ALLOW_FORWARDING);
        verifyZeroInteractions(remoteCommandExecutor);
        assertThat(patchedValue.getValue(), equalTo("1, 2, 3, 4, 5"));
    }
}

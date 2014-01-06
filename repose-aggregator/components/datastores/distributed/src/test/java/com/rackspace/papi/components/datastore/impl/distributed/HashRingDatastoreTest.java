package com.rackspace.papi.components.datastore.impl.distributed;

import com.rackspace.papi.commons.util.encoding.UUIDEncodingProvider;
import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.papi.components.datastore.DatastoreOperationException;
import com.rackspace.papi.components.datastore.distributed.ClusterView;
import com.rackspace.papi.components.datastore.distributed.RemoteBehavior;
import com.rackspace.papi.components.datastore.hash.MD5MessageDigestFactory;
import com.rackspace.papi.components.datastore.impl.distributed.remote.RemoteCommandExecutor;
import com.rackspace.papi.components.datastore.impl.distributed.remote.RemoteConnectionException;
import com.rackspace.papi.components.datastore.impl.distributed.remote.command.Get;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

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

        hashRingDatastore.get("", new byte[]{0}, RemoteBehavior.ALLOW_FORWARDING);

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

        hashRingDatastore.get("", new byte[]{0}, RemoteBehavior.ALLOW_FORWARDING);

        verify(clusterView).memberDamaged(eq(inetSocketAddress), any(String.class));
    }

    @Test
    public void shouldCatchRCExceptionFromPerformRemote() throws Exception {
        when(clusterView.members()).thenReturn(
                new InetSocketAddress[]{inetSocketAddress}, new InetSocketAddress[]{});
        when(localDatastore.get(any(String.class))).thenReturn(Boolean.FALSE);
        when(remoteCommandExecutor.execute(any(Get.class), any(RemoteBehavior.class))).
                thenThrow(new RemoteConnectionException("", new RuntimeException()));

        hashRingDatastore.get("", new byte[]{0}, RemoteBehavior.ALLOW_FORWARDING);

        verify(clusterView).memberDamaged(eq(inetSocketAddress), any(String.class));
    }

    @Test
    public void getName_returnsExpectedName() throws Exception {
        assertThat(hashRingDatastore.getName(), equalTo(HashRingDatastore.DATASTORE_NAME));
    }

    @Test
    public void shouldPatchNewElement(){
        String key = "key-one";
        byte[] id = new byte[] { 1, 2, 3};
        byte[] value = new byte[] { 1, 2, 3};

        when(clusterView.members()).thenReturn(
                new InetSocketAddress[]{inetSocketAddress}, new InetSocketAddress[]{});
        when(localDatastore.get(any(String.class))).thenReturn(storedElement);
        hashRingDatastore.patch(key, id, value, 5, TimeUnit.DAYS, RemoteBehavior.ALLOW_FORWARDING);
        StoredElement element = hashRingDatastore.get(key);
        assertEquals(value, element.elementBytes());
    }

    @Test
    public void shouldPatchExistingElement(){
        String key = "key-one";
        byte[] id = new byte[] { 1, 2, 3};
        byte[] value = new byte[] { 1, 2, 3};
        byte[] newValue = new byte[] { 4, 5};

        when(clusterView.members()).thenReturn(
                new InetSocketAddress[]{inetSocketAddress}, new InetSocketAddress[]{});
        when(localDatastore.get(any(String.class))).thenReturn(storedElement);
        hashRingDatastore.patch(key, id, value, 5, TimeUnit.DAYS, RemoteBehavior.ALLOW_FORWARDING);
        hashRingDatastore.patch(key, id, newValue, 5, TimeUnit.DAYS, RemoteBehavior.ALLOW_FORWARDING);
        StoredElement element = hashRingDatastore.get(key);
        assertEquals(new byte[] {1, 2, 3, 4, 5}, element.elementBytes());
    }
}

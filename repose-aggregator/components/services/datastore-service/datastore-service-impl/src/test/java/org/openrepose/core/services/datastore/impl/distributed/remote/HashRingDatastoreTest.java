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
import org.openrepose.commons.utils.encoding.UUIDEncodingProvider;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.DatastoreOperationException;
import org.openrepose.core.services.datastore.types.StringValue;
import org.openrepose.core.services.datastore.distributed.ClusterView;
import org.openrepose.core.services.datastore.distributed.RemoteBehavior;
import org.openrepose.core.services.datastore.hash.MD5MessageDigestFactory;
import org.openrepose.core.services.datastore.impl.distributed.DatastoreAction;
import org.openrepose.core.services.datastore.impl.distributed.HashRingDatastore;
import org.openrepose.core.services.datastore.impl.distributed.remote.command.Get;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.openrepose.core.services.datastore.distributed.RemoteBehavior.ALLOW_FORWARDING;

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
                MD5MessageDigestFactory.getInstance(), UUIDEncodingProvider.getInstance(), null, false));
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
        byte[] id = new byte[]{1, 2, 3};
        String value = "1, 2, 3";

        when(clusterView.members()).thenReturn(
                new InetSocketAddress[]{inetSocketAddress}, new InetSocketAddress[]{});
        when(clusterView.isLocal(any(InetSocketAddress.class))).thenReturn(true);
        StringValue.Patch patch = new StringValue.Patch(value);
        when(localDatastore.patch(eq(key), same(patch), eq(5), eq(DAYS))).thenReturn(new StringValue(value));
        StringValue patchedValue = (StringValue) hashRingDatastore.patch(key, id, patch, 5, DAYS, ALLOW_FORWARDING);
        verifyZeroInteractions(remoteCommandExecutor);
        verify(localDatastore).patch(any(String.class), any(StringValue.Patch.class), anyInt(), any(TimeUnit.class));
        assertThat(patchedValue.getValue(), equalTo(value));
    }

    @Test
    public void shouldPatchExistingElement() throws Exception {
        String key = "key-one";
        byte[] id = new byte[]{1, 2, 3};
        String value = "1, 2, 3";
        String newValue = ", 4, 5";
        StringValue.Patch secondPatch = new StringValue.Patch(newValue);

        when(clusterView.members()).thenReturn(
                new InetSocketAddress[]{inetSocketAddress}, new InetSocketAddress[]{});
        when(clusterView.isLocal(any(InetSocketAddress.class))).thenReturn(true);
        when(localDatastore.patch(eq(key), same(secondPatch), eq(5), eq(DAYS))).thenReturn(new StringValue("1, 2, 3, 4, 5"));
        hashRingDatastore.patch(key, id, new StringValue.Patch(value), 5, DAYS, ALLOW_FORWARDING);
        StringValue patchedValue = (StringValue) hashRingDatastore.patch(key, id, secondPatch, 5, DAYS, ALLOW_FORWARDING);
        verifyZeroInteractions(remoteCommandExecutor);
        assertThat(patchedValue.getValue(), equalTo("1, 2, 3, 4, 5"));
    }
}

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
import org.openrepose.commons.utils.encoding.EncodingProvider;
import org.openrepose.commons.utils.encoding.UUIDEncodingProvider;
import org.openrepose.commons.utils.net.NetworkInterfaceProvider;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.distributed.ClusterView;
import org.openrepose.core.services.datastore.hash.MD5MessageDigestFactory;
import org.openrepose.core.services.datastore.hash.MessageDigestFactory;
import org.openrepose.core.services.datastore.impl.distributed.ClusterMember;
import org.openrepose.core.services.datastore.impl.distributed.HashRingDatastore;
import org.openrepose.core.services.datastore.impl.distributed.ThreadSafeClusterView;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author zinic
 */
public class AbstractHashRingDatastoreTest {

    private InetAddress[] addresses;
    private ClusterView clusterVew, singleNodeClusterView;
    private Datastore mockedDatastore;
    protected TestingHashRingDatastore datastore;

    private List<Integer> getHttpPortList(int port) {
        List<Integer> ports = new ArrayList<>();
        ports.add(port);
        return ports;
    }

    @Before
    public void standUp() throws Exception {
        addresses = new InetAddress[]{
                InetAddress.getByAddress(new byte[]{10, 1, 1, 11}),
                InetAddress.getByAddress(new byte[]{10, 1, 1, 12}),
                InetAddress.getByAddress(new byte[]{10, 1, 1, 13}),
                InetAddress.getByAddress(new byte[]{10, 1, 1, 14})
        };

        clusterVew = mock(ClusterView.class);

        final NetworkInterfaceProvider networkInterfaceProvider = mock(NetworkInterfaceProvider.class);
        when(networkInterfaceProvider.hasInterfaceFor(any(InetAddress.class))).thenReturn(true);

        final List<ClusterMember> members = Arrays.asList(new ClusterMember[]{new ClusterMember(new InetSocketAddress(addresses[0], 2200), 10000)});
        singleNodeClusterView = new ThreadSafeClusterView(networkInterfaceProvider, members, getHttpPortList(2200));

        when(clusterVew.members()).thenReturn(new InetSocketAddress[]{
                new InetSocketAddress(addresses[0], 2200),
                new InetSocketAddress(addresses[1], 2200),
                new InetSocketAddress(addresses[2], 2200),
                new InetSocketAddress(addresses[3], 2200),
        });

        mockedDatastore = mock(Datastore.class);
    }

    @Test
    public void shouldSelectCorrectTarget() throws Exception {
        datastore = new TestingHashRingDatastore(clusterVew, "", mockedDatastore, MD5MessageDigestFactory.getInstance(), UUIDEncodingProvider.getInstance());

        final byte[] id = new byte[]{1};

        for (byte i = 0; i < 127; i++) {
            id[0] = i;

            assertEquals("Addressing must select correct target", addresses[i % 4], datastore.getTarget(id).getAddress());
        }
    }

    @Test
    public void shouldUseLocalTarget() throws Exception {
        datastore = new TestingHashRingDatastore(singleNodeClusterView, "", mockedDatastore, MD5MessageDigestFactory.getInstance(), UUIDEncodingProvider.getInstance());

        final byte[] id = new byte[]{1};

        assertEquals("If cluster is empty, datastore must use local member", addresses[0], datastore.getTarget(id).getAddress());
    }

    public static class TestingHashRingDatastore extends HashRingDatastore {

        public TestingHashRingDatastore(ClusterView clusterView, String datastorePrefix, Datastore localDatastore, MessageDigestFactory hashProvider, EncodingProvider encodingProvider) {
            super(null, clusterView, datastorePrefix, localDatastore, hashProvider, encodingProvider, null, false);
        }
    }
}

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
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.net.NetworkInterfaceProvider;
import org.openrepose.core.services.datastore.impl.distributed.ClusterMember;
import org.openrepose.core.services.datastore.impl.distributed.ThreadSafeClusterView;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(Enclosed.class)
public class ThreadSafeClusterViewTest {

    public static class TestParent {

        ThreadSafeClusterView threadSafeClusterView;
        List<Integer> servicePorts;
        List<ClusterMember> clusterMembers;
        ClusterMember clusterMember;
        InetSocketAddress inetSocketAddress;
        int i;
        NetworkInterfaceProvider networkInterfaceProvider;

        @Before
        public void setUp() throws Exception {
            servicePorts = mock(List.class);
            i = 1;
            inetSocketAddress = new InetSocketAddress(i);
            clusterMember = new ClusterMember(inetSocketAddress, i);
            clusterMembers = new LinkedList<ClusterMember>();
            clusterMembers.add(clusterMember);
            networkInterfaceProvider = mock(NetworkInterfaceProvider.class);
            threadSafeClusterView = new ThreadSafeClusterView(networkInterfaceProvider, clusterMembers, servicePorts);
        }

        @Test
        public void shouldCreateThreadSafeClusterViewWithServicePorts() {
            threadSafeClusterView = new ThreadSafeClusterView(servicePorts);
            assertNotNull(threadSafeClusterView);
        }

        @Test
        public void shouldCreateThreadSafeClusterViewWithListOfClusterMembersAndServicePorts() {
            threadSafeClusterView = new ThreadSafeClusterView(clusterMembers, servicePorts);
            assertNotNull(threadSafeClusterView);
        }

        @Test
        public void shouldCreateThreadSafeClusterViewWithNetworkInterfaceProviderAndListOfClusterMembersAndServicePorts() {
            threadSafeClusterView = new ThreadSafeClusterView(networkInterfaceProvider, clusterMembers, servicePorts);
            assertNotNull(threadSafeClusterView);
        }

        @Test
        public void shouldReturnThreadSafeClusterViewWhenCopied() {
            assertThat(threadSafeClusterView.copy(), is(instanceOf(ThreadSafeClusterView.class)));
        }

        @Test
        public void shouldReportDamagedMembers() {
            threadSafeClusterView = new ThreadSafeClusterView(networkInterfaceProvider, clusterMembers, servicePorts);
            assertFalse(threadSafeClusterView.hasDamagedMembers());

            clusterMember.setOffline();
            clusterMembers.clear();
            clusterMembers.add(clusterMember);
            threadSafeClusterView = new ThreadSafeClusterView(networkInterfaceProvider, clusterMembers, servicePorts);
            assertTrue(threadSafeClusterView.hasDamagedMembers());
        }

        @Test
        public void shouldReturnMembers() {
            assertThat(threadSafeClusterView.members(), is(instanceOf(InetSocketAddress[].class)));
            assertEquals(threadSafeClusterView.members()[0], clusterMember.getMemberAddress());
        }

    }
}

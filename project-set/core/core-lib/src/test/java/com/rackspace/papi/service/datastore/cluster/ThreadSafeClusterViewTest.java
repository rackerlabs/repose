package com.rackspace.papi.service.datastore.cluster;

import com.rackspace.papi.commons.util.net.NetworkInterfaceProvider;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.service.datastore.cluster.member.ClusterMember;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

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
        ServicePorts servicePorts;
        List<ClusterMember> clusterMembers;
        ClusterMember clusterMember;
        InetSocketAddress inetSocketAddress;
        int i;
        NetworkInterfaceProvider networkInterfaceProvider;

        @Before
        public void setUp() throws Exception {
            servicePorts = mock(ServicePorts.class);
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

        @Test
        public void shouldReturnListenPorts() {
            assertThat(threadSafeClusterView.getListenPorts(), is(instanceOf(ServicePorts.class)));
            assertEquals(threadSafeClusterView.getListenPorts(), servicePorts);
        }
    }
}

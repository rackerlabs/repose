package com.rackspace.papi.filter

import com.google.common.base.Optional
import com.rackspace.papi.domain.Port
import com.rackspace.papi.domain.ServicePorts
import com.rackspace.papi.model.*
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

public class SystemModelInterrogatorTest {
    private SystemModelInterrogator interrogator

    @Before
    public void setup() throws Exception {
        ServicePorts servicePorts = new ServicePorts()
        servicePorts.add(new Port("http", 8080))

        interrogator = new SystemModelInterrogator(servicePorts)
    }

    @Test
    public void getLocalServiceDomain_returnsMatchingCluster() throws Exception {
        SystemModel sysModel = getValidSystemModel()

        Optional<ReposeCluster> returnedCluster = interrogator.getLocalServiceDomain(sysModel)

        assertTrue(returnedCluster.isPresent())

        ReposeCluster cluster = returnedCluster.get()

        assertTrue(cluster.getId().equals("cluster1"))
        assertTrue(cluster.getNodes().getNode().get(0).getId().equals("node1"))
        assertTrue(cluster.getNodes().getNode().get(0).getHostname().equals("localhost"))
        assertTrue(cluster.getNodes().getNode().get(0).getId().equals("node1"))
        assertTrue(cluster.getNodes().getNode().get(0).getHttpPort() == 8080)
    }

    @Test
    public void getLocalServiceDomain_returnsAbsentOptional() throws Exception {
        SystemModel sysModel = getValidSystemModel()
        sysModel.getReposeCluster().get(0).getNodes().getNode().get(0).setHostname("www.example.com")

        Optional<ReposeCluster> returnedCluster = interrogator.getLocalServiceDomain(sysModel)

        assertFalse(returnedCluster.isPresent())
    }

    @Test
    public void getLocalHost_returnsMatchingNode() throws Exception {
        SystemModel sysModel = getValidSystemModel()

        Optional<Node> returnedNode = interrogator.getLocalHost(sysModel)

        assertTrue(returnedNode.isPresent())

        Node node = returnedNode.get()

        assertTrue(node.getId().equals("node1"))
        assertTrue(node.getHostname().equals("localhost"))
        assertTrue(node.getId().equals("node1"))
        assertTrue(node.getHttpPort() == 8080)
    }

    @Test
    public void getLocalHost_returnsAbsentOptional() throws Exception {
        SystemModel sysModel = getValidSystemModel()
        sysModel.getReposeCluster().get(0).getNodes().getNode().get(0).setHostname("www.example.com")

        Optional<Node> returnedNode = interrogator.getLocalHost(sysModel)

        assertFalse(returnedNode.isPresent())
    }

    @Test
    public void getDefaultDestination_returnsMatchingDestination() throws Exception {
        SystemModel sysModel = getValidSystemModel()

        Optional<Destination> returnedDest = interrogator.getDefaultDestination(sysModel)

        assertTrue(returnedDest.isPresent())

        Destination destination = returnedDest.get()

        assertTrue(destination.getId().equals("dest1"))
        assertTrue(destination.getProtocol().equals("http"))
        assertTrue(destination instanceof DestinationEndpoint)
    }

    @Test
    public void getDefaultDestination_returnsAbsentOptional() throws Exception {
        SystemModel sysModel = getValidSystemModel()
        sysModel.getReposeCluster().get(0).getNodes().getNode().get(0).setHostname("www.example.com")

        Optional<Destination> returnedDestination = interrogator.getDefaultDestination(sysModel)

        assertFalse(returnedDestination.isPresent())
    }

    /**
     * @return a valid system model
     */
    private SystemModel getValidSystemModel() {
        Node node = new Node()
        DestinationEndpoint dest = new DestinationEndpoint()
        ReposeCluster cluster = new ReposeCluster()
        SystemModel sysModel = new SystemModel()

        node.setId("node1")
        node.setHostname("localhost")
        node.setHttpPort(8080)

        dest.setHostname("localhost")
        dest.setPort(9090)
        dest.setDefault(true)
        dest.setId("dest1")
        dest.setProtocol("http")

        cluster.setId("cluster1")
        cluster.setNodes(new NodeList())
        cluster.getNodes().getNode().add(node)
        cluster.setDestinations(new DestinationList())
        cluster.getDestinations().getEndpoint().add(dest)

        sysModel.getReposeCluster().add(cluster)

        return sysModel
    }
}

package com.rackspace.papi.filter
import com.google.common.base.Optional
import com.rackspace.papi.domain.Port
import com.rackspace.papi.domain.ServicePorts
import com.rackspace.papi.model.*
import org.junit.Before
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.instanceOf
import static org.junit.Assert.*

public class SystemModelInterrogatorTest {
    private SystemModelInterrogator interrogator

    @Before
    public void setup() throws Exception {
        ServicePorts servicePorts = new ServicePorts()
        servicePorts.add(new Port("http", 8080))

        interrogator = new SystemModelInterrogator(servicePorts)
    }

    @Test
    public void "when passed a valid system model, getLocalServiceDomain(...) should return a matching cluster"() throws Exception {
        SystemModel sysModel = getValidSystemModel()

        Optional<ReposeCluster> returnedCluster = interrogator.getLocalCluster(sysModel)

        assertTrue(returnedCluster.isPresent())

        ReposeCluster cluster = returnedCluster.get()

        assertThat(cluster.getId(), equalTo("cluster1"))
        assertThat(cluster.getNodes().getNode().get(0).getId(), equalTo("node1"))
        assertThat(cluster.getNodes().getNode().get(0).getHostname(), equalTo("localhost"))
        assertThat(cluster.getNodes().getNode().get(0).getHttpPort(), equalTo(8080))
    }

    @Test
    public void "when passed a system model missing a matching cluster, getLocalServiceDomain(...) should return an absent Optional"() throws Exception {
        SystemModel sysModel = getValidSystemModel()
        sysModel.getReposeCluster().get(0).getNodes().getNode().get(0).setHostname("www.example.com")

        Optional<ReposeCluster> returnedCluster = interrogator.getLocalCluster(sysModel)

        assertFalse(returnedCluster.isPresent())
    }

    @Test
    public void "when passed a valid system model, getLocalHost(...) should return a matching node"() throws Exception {
        SystemModel sysModel = getValidSystemModel()

        Optional<Node> returnedNode = interrogator.getLocalNode(sysModel)

        assertTrue(returnedNode.isPresent())

        Node node = returnedNode.get()

        assertThat(node.getId(), equalTo("node1"))
        assertThat(node.getHostname(), equalTo("localhost"))
        assertThat(node.getHttpPort(), equalTo(8080))
    }

    @Test
    public void "when passed a system model missing a matching node, getLocalHost(...) should return an absent Optional"() throws Exception {
        SystemModel sysModel = getValidSystemModel()
        sysModel.getReposeCluster().get(0).getNodes().getNode().get(0).setHostname("www.example.com")

        Optional<Node> returnedNode = interrogator.getLocalNode(sysModel)

        assertFalse(returnedNode.isPresent())
    }

    @Test
    public void "when passed a valid system model, getDefaultDestination(...) should return a matching default destination"() throws Exception {
        SystemModel sysModel = getValidSystemModel()

        Optional<Destination> returnedDest = interrogator.getDefaultDestination(sysModel)

        assertTrue(returnedDest.isPresent())

        Destination destination = returnedDest.get()

        assertThat(destination.getId(), equalTo("dest1"))
        assertThat(destination.getProtocol(), equalTo("http"))
        assertThat(destination.getId(), equalTo("dest1"))
        assertThat(destination, instanceOf(DestinationEndpoint))
    }

    @Test
    public void "when passed a system model missing a matching default destination, getDefaultDestination(...) should return an absent Optional"() throws Exception {
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

package org.openrepose.core.filter
import com.google.common.base.Optional
import org.openrepose.core.domain.Port
import org.openrepose.core.domain.ServicePorts
import org.openrepose.core.systemmodel.*
import org.junit.Before
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.instanceOf
import static org.junit.Assert.*

public class SystemModelInterrogatorTest {
    private SystemModelInterrogator interrogator

    @Before
    public void setup() throws Exception {
        interrogator = new SystemModelInterrogator("cluster1", "node1")
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
        sysModel.getReposeCluster().get(0).setId("nope")

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
        sysModel.getReposeCluster().get(0).getNodes().getNode().get(0).setId("nopes")

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
        sysModel.getReposeCluster().get(0).getNodes().getNode().get(0).setId("NOPES")

        Optional<Destination> returnedDestination = interrogator.getDefaultDestination(sysModel)

        assertFalse(returnedDestination.isPresent())
    }

    @Test
    public void "when no destinations are present, cluster exists but destinations are absent"(){
        SystemModel sysModel = getValidSystemModel()
        sysModel.reposeCluster[0].destinations = new DestinationList()

        Optional<ReposeCluster> returnedCluster = interrogator.getLocalCluster(sysModel)

        assertTrue(returnedCluster.isPresent())

        Optional<Destination> destination = interrogator.getDefaultDestination(sysModel)

        assertFalse(destination.isPresent())

    }

    @Test
    public void "when no clusters are present, cluster and destination are absent"(){
        SystemModel sysModel = getValidSystemModel()
        sysModel.reposeCluster = new ArrayList<ReposeCluster>()

        Optional<ReposeCluster> returnedCluster = interrogator.getLocalCluster(sysModel)

        assertFalse(returnedCluster.isPresent())

        Optional<Destination> destination = interrogator.getDefaultDestination(sysModel)

        assertFalse(destination.isPresent())
    }

    /**
     * @return a valid system model
     */
    private SystemModel getValidSystemModel() {
        ReposeCluster cluster = new ReposeCluster()
        SystemModel sysModel = new SystemModel()

        cluster.setId("cluster1")
        cluster.setNodes(new NodeList())
        cluster.getNodes().getNode() <<
                new Node(id: "node1", hostname: "localhost", httpPort: 8080, httpsPort: 8181)
        cluster.setDestinations(new DestinationList())
        cluster.getDestinations().getEndpoint() << new DestinationEndpoint(
                hostname: "localhost", port: 9090, default: true, id: "dest1", protocol: "http")

        sysModel.getReposeCluster().add(cluster)

        return sysModel
    }
}

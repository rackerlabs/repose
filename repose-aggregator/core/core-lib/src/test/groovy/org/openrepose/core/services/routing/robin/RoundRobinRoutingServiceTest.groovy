/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openrepose.core.services.routing.robin

import com.rackspace.papi.model.*
import spock.lang.Specification

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotSame
import static org.junit.Assert.assertNull
import static org.mockito.Mockito.mock

public class RoundRobinRoutingServiceTest extends Specification {

    SystemModel systemModel
    RoundRobinRoutingService robinRoutingService

    def setup() {
        robinRoutingService = new RoundRobinRoutingService()
        systemModel = new SystemModel()

        def domainRepose = new ReposeCluster()
        domainRepose.setFilters(mock(FilterList.class))
        domainRepose.setId("reposeDomain1")

        def node1 = new Node()
        node1.setHostname("reposeNode1")
        node1.setHttpPort(8181)
        node1.setHttpsPort(0)

        def node2 = new Node()
        node2.setHostname("reposeNode2")
        node2.setHttpPort(8282)
        node2.setHttpsPort(0)

        def nodeListRepose = new NodeList()
        nodeListRepose.getNode().add(node1)
        nodeListRepose.getNode().add(node2)

        domainRepose.setNodes(nodeListRepose)

        def destList = new DestinationList()
        domainRepose.setDestinations(destList)
        systemModel.getReposeCluster().add(domainRepose)

        def domainService = new Cluster()
        domainService.setId("serviceDomain1")

        def node3 = new Node()
        node3.setHostname("serviceNode3")
        node3.setHttpPort(8383)
        node3.setHttpsPort(0)

        def node4 = new Node()
        node4.setHostname("serviceNode4")
        node4.setHttpPort(8484)
        node4.setHttpsPort(0)

        def nodeListService = new NodeList()
        nodeListService.getNode().add(node3)
        nodeListService.getNode().add(node4)

        domainService.setNodes(nodeListService)

        systemModel.getServiceCluster().add(domainService)

        robinRoutingService.setSystemModel(systemModel)
    }

    def "Should Switch Between Two Routable Nodes"() {
        given:
        final Node node1 = robinRoutingService.getRoutableNode("reposeDomain1")
        final Node node2 = robinRoutingService.getRoutableNode("reposeDomain1")
        final Node node3 = robinRoutingService.getRoutableNode("reposeDomain1")

        expect:
        assertNotSame("Round Robin Routing Service will switch between the two configured destinations", node2, node1)
        assertEquals("Round Robin Routing Service will cycle back if all destinations have been returned", node3, node1)
    }

    def "Should Return Null On Unknown Repose Node"() {
        given:
        final Node node1 = robinRoutingService.getRoutableNode("reposeDomainX")

        expect:
        assertNull("No Routable Node", node1)
    }

    def "Null Domain should Throw Null Pointer Exception."() {
        given:
        systemModel = new SystemModel()
        systemModel.getReposeCluster().add(null)

        when:
        robinRoutingService.setSystemModel(systemModel)

        then:
        def caught = thrown(NullPointerException)
        caught.getLocalizedMessage() == null
    }
}

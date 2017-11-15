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
package org.openrepose.core.services.routing.robin

import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.config.parser.common.ConfigurationParser
import org.openrepose.commons.config.resource.ConfigurationResourceResolver
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.systemmodel.config.*
import spock.lang.Specification

import static org.junit.Assert.*
import static org.mockito.Mockito.mock

public class RoundRobinRoutingServiceTest extends Specification {

    SystemModel systemModel
    RoundRobinRoutingService robinRoutingService

    MockConfigService<SystemModel> mockConfig = new MockConfigService<>()

    class MockConfigService<T> implements ConfigurationService {

        UpdateListener<T> listener = null

        @Override
        ConfigurationResourceResolver getResourceResolver() {
            return null
        }

        @Override
        def <T> void subscribeTo(String configurationName, UpdateListener<T> listener, Class<T> configurationClass) {
        }

        @Override
        def <T> void subscribeTo(String filterName, String configurationName, UpdateListener<T> listener, Class<T> configurationClass) {

        }

        @Override
        def <T> void subscribeTo(String configurationName, URL xsdStreamSource, UpdateListener<T> listener, Class<T> configurationClass) {
            //This one
            this.listener = listener
        }

        @Override
        def <T> void subscribeTo(String filterName, String configurationName, URL xsdStreamSource, UpdateListener<T> listener, Class<T> configurationClass) {

        }

        @Override
        def <T> void subscribeTo(String filterName, String configurationName, UpdateListener<T> listener, ConfigurationParser<T> customParser) {

        }

        @Override
        def <T> void subscribeTo(String filterName, String configurationName, UpdateListener<T> listener, ConfigurationParser<T> customParser, boolean sendNotificationNow) {

        }

        @Override
        void unsubscribeFrom(String configurationName, UpdateListener plistener) {

        }

        @Override
        void destroy() {

        }
    }


    def setup() {
        robinRoutingService = new RoundRobinRoutingService(mockConfig)
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
        robinRoutingService.afterPropertiesSet()

        mockConfig.listener.configurationUpdated(systemModel)
        //robinRoutingService.setSystemModel(systemModel)
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
        mockConfig.listener.configurationUpdated(systemModel)
        //robinRoutingService.setSystemModel(systemModel)

        then:
        def caught = thrown(NullPointerException)
        caught.getLocalizedMessage() == null
    }
}

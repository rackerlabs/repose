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
package org.openrepose.core.filter


import org.junit.Before
import org.junit.Test
import org.openrepose.core.systemmodel.config.*

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.instanceOf
import static org.junit.Assert.*

public class SystemModelInterrogatorTest {
    private SystemModelInterrogator interrogator

    @Before
    public void setup() throws Exception {
        interrogator = new SystemModelInterrogator("node1")
    }

    @Test
    public void testGetNodeReturnsMatchingNodeForValidSystemModel() throws Exception {
        SystemModel sysModel = getValidSystemModel()

        Optional<Node> returnedNode = interrogator.getNode(sysModel)

        assertTrue(returnedNode.isPresent())

        Node node = returnedNode.get()

        assertThat(node.getId(), equalTo("node1"))
        assertThat(node.getHostname(), equalTo("localhost"))
        assertThat(node.getHttpPort(), equalTo(8080))
    }

    @Test
    public void testGetNodeReturnsAbsentOptionalWhenNodeMissing() throws Exception {
        SystemModel sysModel = getValidSystemModel()
        sysModel.getNodes().getNode().get(0).setId("nopes")

        Optional<Node> returnedNode = interrogator.getNode(sysModel)

        assertFalse(returnedNode.isPresent())
    }

    @Test
    public void testGetServiceReturnsMatchingServiceForValidSystemModel() throws Exception {
        String serviceName = "foo"
        SystemModel sysModel = getValidSystemModel()
        sysModel.services = new ServicesList()
        Service service = new Service()
        service.name = serviceName
        sysModel.services.service.add(service)

        Optional<Service> returnedService = interrogator.getService(sysModel, serviceName)

        assertTrue(returnedService.isPresent())

        Service retrievedService = returnedService.get()

        assertThat(retrievedService.getName(), equalTo(serviceName))
    }

    @Test
    public void testGetServiceReturnsAbsentOptionalWhenServiceMissing() throws Exception {
        String serviceName = "foo"
        SystemModel sysModel = getValidSystemModel()
        sysModel.services = new ServicesList()

        Optional<Service> returnedService = interrogator.getService(sysModel, serviceName)

        assertFalse(returnedService.isPresent())
    }

    @Test
    public void testGetDefaultDestinationReturnsMatchingDestinationForValidSystemModel() throws Exception {
        SystemModel sysModel = getValidSystemModel()

        Optional<Destination> returnedDest = interrogator.getDefaultDestination(sysModel)

        assertTrue(returnedDest.isPresent())

        Destination destination = returnedDest.get()

        assertThat(destination.getId(), equalTo("dest1"))
        assertThat(destination.getProtocol(), equalTo("http"))
        assertThat(destination.getId(), equalTo("dest1"))
        assertThat(destination, instanceOf(Destination))
    }

    @Test
    public void testGetDefaultDestinationReturnsAbsentOptionalWhenDestinationMissing() throws Exception {
        SystemModel sysModel = getValidSystemModel()
        sysModel.getDestinations().getEndpoint().head().setDefault(false)

        Optional<Destination> returnedDestination = interrogator.getDefaultDestination(sysModel)

        assertFalse(returnedDestination.isPresent())
    }

    /**
     * @return a valid system model
     */
    private static SystemModel getValidSystemModel() {
        SystemModel sysModel = new SystemModel()

        sysModel.setNodes(new NodeList())
        Node node = new Node()
        node.id = "node1"
        node.hostname = "localhost"
        node.httpPort = 8080
        node.httpsPort = 8181
        sysModel.getNodes().getNode().add(node)
        
        sysModel.setDestinations(new DestinationList())
        Destination dest = new Destination()
        dest.hostname = "localhost"
        dest.port = 9090
        dest.isDefault = true
        dest.id = "dest1"
        dest.protocol = "http"
        sysModel.getDestinations().getEndpoint().add(dest)

        return sysModel
    }
}

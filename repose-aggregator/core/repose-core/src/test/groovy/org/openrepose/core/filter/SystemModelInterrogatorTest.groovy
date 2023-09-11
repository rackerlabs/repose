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
    public void "when passed a valid system model, getLocalHost should return a matching node"() throws Exception {
        SystemModel sysModel = getValidSystemModel()

        Optional<Node> returnedNode = interrogator.getNode(sysModel)

        assertTrue(returnedNode.isPresent())

        Node node = returnedNode.get()

        assertThat(node.getId(), equalTo("node1"))
        assertThat(node.getHostname(), equalTo("localhost"))
        assertThat(node.getHttpPort(), equalTo(8080))
    }

    @Test
    public void "when passed a system model missing a matching node, getLocalHost should return an absent Optional"() throws Exception {
        SystemModel sysModel = getValidSystemModel()
        sysModel.getNodes().getNode().get(0).setId("nopes")

        Optional<Node> returnedNode = interrogator.getNode(sysModel)

        assertFalse(returnedNode.isPresent())
    }

    @Test
    public void "when passed a valid system model including a service, getService should return a matching service"() throws Exception {
        String serviceName = "foo"
        SystemModel sysModel = getValidSystemModel()
        sysModel.services = new ServicesList()
        sysModel.services.service <<
            new Service(name: serviceName)

        Optional<Service> returnedService = interrogator.getService(sysModel, serviceName)

        assertTrue(returnedService.isPresent())

        Service service = returnedService.get()

        assertThat(service.getName(), equalTo(serviceName))
    }

    @Test
    public void "when passed a system model missing a service, getService should return an absent Optional"() throws Exception {
        String serviceName = "foo"
        SystemModel sysModel = getValidSystemModel()
        sysModel.services = new ServicesList()

        Optional<Service> returnedService = interrogator.getService(sysModel, serviceName)

        assertFalse(returnedService.isPresent())
    }

    @Test
    public void "when passed a valid system model, getDefaultDestination should return a matching default destination"() throws Exception {
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
    public void "when passed a system model missing a matching default destination, getDefaultDestination should return an absent Optional"() throws Exception {
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
        sysModel.getNodes().getNode() <<
            new Node(id: "node1", hostname: "localhost", httpPort: 8080, httpsPort: 8181)
        sysModel.setDestinations(new DestinationList())
        sysModel.getDestinations().getEndpoint() << new Destination(
            hostname: "localhost", port: 9090, isDefault: true, id: "dest1", protocol: "http")

        return sysModel
    }
}

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

import org.intellij.lang.annotations.Language
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.openrepose.core.Marshaller
import org.openrepose.core.systemmodel.config.*

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.instanceOf
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

    // @TODO: There will be only one cluster after REP-7314
    @Ignore
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
    public void "when no destinations are present, cluster exists but destinations are absent"() {
        SystemModel sysModel = getValidSystemModel()
        sysModel.reposeCluster[0].destinations = new DestinationList()

        Optional<ReposeCluster> returnedCluster = interrogator.getLocalCluster(sysModel)

        assertTrue(returnedCluster.isPresent())

        Optional<Destination> destination = interrogator.getDefaultDestination(sysModel)

        assertFalse(destination.isPresent())

    }

    @Test
    public void "when no clusters are present, cluster and destination are absent"() {
        SystemModel sysModel = getValidSystemModel()
        sysModel.reposeCluster = new ArrayList<ReposeCluster>()

        Optional<ReposeCluster> returnedCluster = interrogator.getLocalCluster(sysModel)

        assertFalse(returnedCluster.isPresent())

        Optional<Destination> destination = interrogator.getDefaultDestination(sysModel)

        assertFalse(destination.isPresent())
    }

    @Test
    public void "When having multiple clusters, it should select the right local Cluster"() {
        @Language("XML")
        def systemModelXML = """<?xml version="1.0" encoding="UTF-8"?>
<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
  <repose-cluster id="cluster-1">
    <nodes>
      <node id="node-1-1" hostname="localhost" http-port="1011"/>
      <node id="node-1-2" hostname="example.com" http-port="1012"/>
    </nodes>
    <filters></filters>
    <destinations>
      <endpoint id="target" protocol="http" hostname="localhost" port="801" root-path="/" default="true"/>
    </destinations>
  </repose-cluster>

  <repose-cluster id="cluster-2">
    <nodes>
      <node id="node-2-1" hostname="localhost" http-port="1021"/>
      <node id="node-2-2" hostname="example.com" http-port="1022"/>
    </nodes>
    <filters></filters>
    <destinations>
      <endpoint id="target" protocol="http" hostname="localhost" port="802" root-path="/" default="true"/>
    </destinations>
  </repose-cluster>
</system-model>
"""

        SystemModel systemModel = Marshaller.systemModelString(systemModelXML)

        def interrogator = new SystemModelInterrogator("cluster-2", "node-2-1")
        def localCluster = interrogator.getLocalCluster(systemModel)
        assertTrue(localCluster.isPresent())
        def lc = localCluster.get()
        assertThat(lc.getId(), equalTo("cluster-2"))

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

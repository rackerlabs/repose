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
package org.openrepose.core.filter.routing

import javax.servlet.http.HttpServletRequest

import org.junit.runner.RunWith
import org.mockito.Matchers.anyString
import org.mockito.Mockito.when
import org.openrepose.core.services.routing.RoutingService
import org.openrepose.core.systemmodel._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

/**
 * TODO: something is missing in here!
 */
@RunWith(classOf[JUnitRunner])
class DestinationLocationBuilderTest extends FunSpec with Matchers with MockitoSugar {

  describe("Domain location building") {
    val domain = new ReposeCluster()
    domain.setId("domainId")
    val domainNode = new Node()
    domainNode.setHostname("destNode")
    domainNode.setHttpPort(8080)
    domainNode.setHttpsPort(8443)
    domainNode.setId("destNodeId")

    val nodeList = new NodeList()
    nodeList.getNode.add(domainNode)
    domain.setNodes(nodeList)

    val routingService = mock[RoutingService]
    when(routingService.getRoutableNode(anyString())).thenReturn(domainNode)
    val request = mock[HttpServletRequest]
    when(request.getQueryString).thenReturn(null)

    val dest = new DestinationCluster()
    dest.setDefault(true)
    dest.setId("destId")
    dest.setProtocol("http")
    dest.setRootPath("/root")
    dest.setCluster(domain)

    val destList = new DestinationList()
    destList.getTarget.add(dest)
    domain.setDestinations(destList)

    it("finds a node and builds a destination") {
      val uri = "/context"

      val instance = new DestinationLocationBuilder(routingService, null)

      val built = instance.build(dest, uri, request)

      val expectedPath = "/root" + uri
      val expectedUrl = dest.getProtocol + "://" + domainNode.getHostname + ":" + domainNode.getHttpPort + expectedPath

      built.getUri.getPath shouldBe expectedPath
      built.getUri.toString shouldBe expectedUrl
      built.getUrl.toExternalForm shouldBe expectedUrl
    }

    it("returns an https port") {
      val uri = "/context"

      dest.setProtocol("https")

      val instance = new DestinationLocationBuilder(routingService, null)

      val built = instance.build(dest, uri, request)
      val expectedPath = "/root" + uri
      val expectedUrl = dest.getProtocol + "://" + domainNode.getHostname + ":" + domainNode.getHttpsPort + expectedPath

      built.getUri.getPath shouldBe expectedPath
      built.getUri.toString shouldBe expectedUrl
      built.getUrl.toExternalForm shouldBe expectedUrl
    }

    it("returns null when no routable node found") {
      val uri = "/context"

      when(routingService.getRoutableNode(anyString())).thenReturn(null)

      val instance = new DestinationLocationBuilder(routingService, null)
      val built = instance.build(dest, uri, request)

      built shouldBe null
    }
  }
  describe("Endpoint location building") {
    val routingService = mock[RoutingService]
    val request = mock[HttpServletRequest]
    when(request.getScheme).thenReturn("http")
    when(request.getLocalPort).thenReturn(8080)

    val localhost = new Node()
    localhost.setHttpPort(8080)
    localhost.setHttpsPort(0)
    localhost.setHostname("myhost")
    localhost.setId("local")

    def destinationEndpoint(id: String = null,
                            hostname: String = null,
                            port: Integer = -1,
                            protocol: String = null,
                            rootPath: String = null,
                            default: Boolean = false): DestinationEndpoint = {
      val d = new DestinationEndpoint()
      d.setId(id)
      d.setHostname(hostname)
      d.setPort(port)
      d.setProtocol(protocol)
      d.setRootPath(rootPath)
      d.setDefault(default)
      d
    }
    val uri = "/somepath"

    describe("when building local endpoint locations") {

      ignore("returns a proper URI when no path is specified") {
        //This test will probably be forever ignored, or maybe something more specific should happen...
        //So Repose creates this, but it's handled outside repose, and so this test case will fail
        // It's delegated to the endpoint, and it is not our problem to handle "//"
        val instance = new DestinationLocationBuilder(routingService, localhost)
        val result = instance.build(destinationEndpoint(id = "destId", port = 8080, protocol = "http", rootPath = "/", default = true), "/", request)
        val expectedUri = "/"
        val expectedUrl = "http://localhost:8080" + expectedUri

        result.getUri.getPath shouldBe expectedUri
        result.getUri.toString shouldBe expectedUri
        result.getUrl.toExternalForm shouldBe expectedUrl
      }

      it("returns local URI when no hostname specified") {
        val instance = new DestinationLocationBuilder(routingService, localhost)
        val result = instance.build(destinationEndpoint(id = "destId", port = 8080, protocol = "http", rootPath = "/root", default = true), uri, request)
        val expectedUri = "/root" + uri
        val expectedUrl = "http://localhost:8080" + expectedUri

        result.getUri.getPath shouldBe expectedUri
        result.getUri.toString shouldBe expectedUri
        result.getUrl.toExternalForm shouldBe expectedUrl
      }
      it("returns local uri when no protocol or host port specified") {
        val instance = new DestinationLocationBuilder(routingService, localhost)
        val result = instance.build(destinationEndpoint(id = "destId", port = 0, protocol = "http", rootPath = "/no-port-root", default = true), uri, request)
        val expectedUri = "/no-port-root" + uri
        val expectedUrl = "http://localhost:8080" + expectedUri

        result.getUri.getPath shouldBe expectedUri
        result.getUri.toString shouldBe expectedUri
        result.getUrl.toExternalForm shouldBe expectedUrl
      }
      it("returns local URI when protocol but no port specified") {
        val instance = new DestinationLocationBuilder(routingService, localhost)
        val result = instance.build(destinationEndpoint(id = "destId", rootPath = "/minimal-root", default = true), uri, request)
        val expectedUri = "/minimal-root" + uri
        val expectedUrl = "http://localhost:8080" + expectedUri

        result.getUri.getPath shouldBe expectedUri
        result.getUri.toString shouldBe expectedUri
        result.getUrl.toExternalForm shouldBe expectedUrl
      }
      it("returns local URI when host port matches localhost") {
        val instance = new DestinationLocationBuilder(routingService, localhost)
        val result = instance.build(destinationEndpoint(id = "destId", hostname = "localhost", port = 8080, protocol = "http", rootPath = "/root", default = true), uri, request)
        val expectedUri = "/root" + uri
        val expectedUrl = "http://localhost:8080" + expectedUri

        result.getUri.getPath shouldBe expectedUri
        result.getUri.toString shouldBe expectedUri
        result.getUrl.toExternalForm shouldBe expectedUrl
      }
    }

    describe("when building remote endpoint locations") {
      it("returns full url in uri to string for remote dispatch") {
        val instance = new DestinationLocationBuilder(routingService, localhost)
        val result = instance.build(destinationEndpoint(id = "destId", hostname = "otherhost", port = 8080, protocol = "http", rootPath = "/root", default = true), uri, request)
        val expectedUri = "/root" + uri
        val expectedUrl = "http://otherhost:8080" + expectedUri

        result.getUri.getPath shouldBe expectedUri
        result.getUri.toString shouldBe expectedUrl
        result.getUrl.toExternalForm shouldBe expectedUrl

      }
      it("returns full url in uri to string for remote dispatch with no root path") {
        val instance = new DestinationLocationBuilder(routingService, localhost)
        val result = instance.build(destinationEndpoint(id = "destId", hostname = "otherhost", port = 8080, protocol = "http", default = true), uri, request)
        val expectedUri = uri
        val expectedUrl = "http://otherhost:8080" + expectedUri

        result.getUri.getPath shouldBe expectedUri
        result.getUri.toString shouldBe expectedUrl
        result.getUrl.toExternalForm shouldBe expectedUrl
      }

      it("returns full url in uri to string for local different port dispatch") {
        val instance = new DestinationLocationBuilder(routingService, localhost)
        val result = instance.build(destinationEndpoint(id = "destId", port = 8081, protocol = "http", rootPath = "/root", default = true), uri, request)
        val expectedUri = "/root" + uri
        val expectedUrl = "http://localhost:8081" + expectedUri

        result.getUri.getPath shouldBe expectedUri
        result.getUri.toString shouldBe expectedUrl
        result.getUrl.toExternalForm shouldBe expectedUrl
      }
    }
  }
}

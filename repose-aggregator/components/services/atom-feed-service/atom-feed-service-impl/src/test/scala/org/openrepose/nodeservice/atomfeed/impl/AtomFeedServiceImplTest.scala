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
package org.openrepose.nodeservice.atomfeed.impl

import java.net.URL

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.mockito.Matchers.{eq => isEq, _}
import org.mockito.Mockito.verify
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.systemmodel._
import org.openrepose.docs.repose.atom_feed_service.v1.{AtomFeedServiceConfigType, OpenStackIdentityV2AuthenticationType}
import org.openrepose.nodeservice.atomfeed.AtomFeedListener
import org.openrepose.nodeservice.atomfeed.impl.auth.OpenStackIdentityV2AuthenticatedRequestFactory
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class AtomFeedServiceImplTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
  val serviceListAppender = ctx.getConfiguration.getAppender("serviceList").asInstanceOf[ListAppender]

  var mockConfigService: ConfigurationService = _

  before {
    mockConfigService = mock[ConfigurationService]
    serviceListAppender.clear()
  }

  describe("init") {
    it("should register configuration listeners") {
      val atomFeedService = new AtomFeedServiceImpl("1.0", "", "", mockConfigService)

      atomFeedService.init()

      verify(mockConfigService).subscribeTo(
        isEq("system-model.cfg.xml"),
        any[UpdateListener[SystemModel]](),
        isA(classOf[Class[SystemModel]])
      )
      verify(mockConfigService).subscribeTo(
        isEq("atom-feed-service.cfg.xml"),
        any[URL](),
        isA(classOf[UpdateListener[AtomFeedServiceConfigType]]),
        isA(classOf[Class[AtomFeedServiceConfigType]])
      )
    }
  }

  describe("destroy") {
    it("should unregister configuration listeners") {
      val atomFeedService = new AtomFeedServiceImpl("1.0", "", "", mockConfigService)

      atomFeedService.destroy()

      verify(mockConfigService).unsubscribeFrom(isEq("atom-feed-service.cfg.xml"), isA(classOf[UpdateListener[_]]))
      verify(mockConfigService).unsubscribeFrom(isEq("system-model.cfg.xml"), isA(classOf[UpdateListener[_]]))
    }
  }

  describe("registerListener") {
    it("should register a notifier with a notifier manager") {
      val atomFeedService = new AtomFeedServiceImpl("1.0", "clusterId", "nodeId", mockConfigService)

      val listenerIdOne = atomFeedService.registerListener("feedId", mock[AtomFeedListener])
      val listenerIdTwo = atomFeedService.registerListener("feedIdTwo", mock[AtomFeedListener])

      listenerIdOne shouldBe a[String]
      listenerIdTwo shouldBe a[String]
      listenerIdOne should not equal listenerIdTwo
    }
  }

  describe("unregisterListener") {
    ignore("TEMPORARILY IGNORED FOR PARALLELIZATION TESTING!!!"){
    it("should unregister a listener when passed a valid listener ID") {
      val atomFeedService = new AtomFeedServiceImpl("1.0", "clusterId", "nodeId", mockConfigService)

      val listenerId = atomFeedService.registerListener("feedId", mock[AtomFeedListener])
      atomFeedService.unregisterListener(listenerId)

      serviceListAppender.getEvents.exists(_.getMessage.getFormattedMessage.contains("Attempting to unregister")) shouldBe true
      serviceListAppender.getEvents.exists(_.getMessage.getFormattedMessage.contains("not registered")) shouldBe false
    }}

    it("should report if a listener ID is not registered") {
      val atomFeedService = new AtomFeedServiceImpl("1.0", "clusterId", "nodeId", mockConfigService)

      atomFeedService.unregisterListener("notRegisteredFeedId")

      serviceListAppender.getEvents.exists(_.getMessage.getFormattedMessage.contains("Attempting to unregister")) shouldBe true
      serviceListAppender.getEvents.exists(_.getMessage.getFormattedMessage.contains("not registered")) shouldBe true
    }
  }

  describe("buildAuthenticatedRequestFactory") {
    it("should return an AuthenticatedRequestFactory if a valid fqcn is provided") {
      val authConfig = new OpenStackIdentityV2AuthenticationType()

      AtomFeedServiceImpl.buildAuthenticatedRequestFactory(authConfig) shouldBe an[OpenStackIdentityV2AuthenticatedRequestFactory]
    }

    it("should throw an IllegalArgumentException if an invalid fqcn is provided") {
      val authConfig = new OpenStackIdentityV2AuthenticationType()
      authConfig.setFqcn("foo")

      an[IllegalArgumentException] should be thrownBy AtomFeedServiceImpl.buildAuthenticatedRequestFactory(authConfig)
    }

    it("should throw an IllegalArgumentException if an invalid fqcn class is provided") {
      val authConfig = new OpenStackIdentityV2AuthenticationType()
      authConfig.setFqcn("java.lang.Object")

      an[IllegalArgumentException] should be thrownBy AtomFeedServiceImpl.buildAuthenticatedRequestFactory(authConfig)
    }
  }

  def getSystemModelWithService: SystemModel = {
    val systemModel = new SystemModel()
    val cluster = new ReposeCluster()
    val nodes = new NodeList()
    val node = new Node()
    val services = new ServicesList()
    val service = new Service()
    node.setId("nodeId")
    nodes.getNode.add(node)
    service.setName(AtomFeedServiceImpl.SERVICE_NAME)
    services.getService.add(service)
    cluster.setId("clusterId")
    cluster.setServices(services)
    cluster.setNodes(nodes)
    systemModel.getReposeCluster.add(cluster)
    systemModel
  }
}

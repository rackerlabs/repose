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
import org.openrepose.docs.repose.atom_feed_service.v1.{AtomFeedConfigType, AtomFeedServiceConfigType}
import org.openrepose.nodeservice.atomfeed.AtomFeedListener
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
      val atomFeedService = new AtomFeedServiceImpl("", "", mockConfigService)

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

    it("should not start the service if configuration files have not yet been read") {
      val systemModel = getSystemModelWithService

      val atomFeedService = new AtomFeedServiceImpl("clusterId", "", mockConfigService)
      atomFeedService.init()

      val logEvents = serviceListAppender.getEvents
      logEvents.size() shouldEqual 1
      logEvents.get(0).getMessage.getFormattedMessage should include("Initializing")
      atomFeedService.isRunning shouldBe false
    }
  }

  describe("destroy") {
    it("should unregister configuration listeners") {
      val atomFeedService = new AtomFeedServiceImpl("", "", mockConfigService)

      atomFeedService.destroy()

      verify(mockConfigService).unsubscribeFrom("atom-feed-service.cfg.xml", atomFeedService.AtomFeedServiceConfigurationListener)
      verify(mockConfigService).unsubscribeFrom("system-model.cfg.xml", atomFeedService.SystemModelConfigurationListener)
    }
  }

  describe("configurationUpdated") {
    it("should stop the service if it is not listed in the system model for this node") {
      val systemModel = getSystemModelWithService
      systemModel.getReposeCluster.get(0).getServices.getService.clear()

      val atomFeedService = new AtomFeedServiceImpl("clusterId", "nodeId", mockConfigService)
      atomFeedService.init()
      atomFeedService.SystemModelConfigurationListener.configurationUpdated(systemModel)
      atomFeedService.AtomFeedServiceConfigurationListener.configurationUpdated(new AtomFeedServiceConfigType())

      atomFeedService.isRunning shouldBe false
    }

    it("should start the service if it is listed in the system model for this node, and a valid config is provided") {
      val systemModel = getSystemModelWithService

      val atomFeedService = new AtomFeedServiceImpl("clusterId", "nodeId", mockConfigService)
      atomFeedService.init()
      atomFeedService.SystemModelConfigurationListener.configurationUpdated(systemModel)
      atomFeedService.AtomFeedServiceConfigurationListener.configurationUpdated(new AtomFeedServiceConfigType())

      atomFeedService.isRunning shouldBe true
    }

    it("should restart the service if it is listed in the system model and the service config is updated") {
      val systemModel = getSystemModelWithService

      val atomFeedService = new AtomFeedServiceImpl("clusterId", "nodeId", mockConfigService)
      atomFeedService.init()
      atomFeedService.SystemModelConfigurationListener.configurationUpdated(systemModel)
      atomFeedService.AtomFeedServiceConfigurationListener.configurationUpdated(new AtomFeedServiceConfigType())
      atomFeedService.AtomFeedServiceConfigurationListener.configurationUpdated(new AtomFeedServiceConfigType())

      serviceListAppender.getMessages.exists(_.contains("Stopping"))
      serviceListAppender.getMessages.exists(_.contains("Starting"))
      atomFeedService.isRunning shouldBe true
    }
  }

  describe("registerListener") {
    it("should throw an IllegalStateException if the service is not enabled") {
      val systemModel = getSystemModelWithService
      systemModel.getReposeCluster.get(0).getServices.getService.clear()

      val atomFeedService = new AtomFeedServiceImpl("clusterId", "nodeId", mockConfigService)

      an[IllegalStateException] should be thrownBy atomFeedService.registerListener("feedId", mock[AtomFeedListener])
    }

    it("should throw an IllegalArgumentException if the feedId parameter does not match a configured feed") {
      val systemModel = getSystemModelWithService

      val atomFeedService = new AtomFeedServiceImpl("clusterId", "nodeId", mockConfigService)
      atomFeedService.init()
      atomFeedService.SystemModelConfigurationListener.configurationUpdated(systemModel)
      atomFeedService.AtomFeedServiceConfigurationListener.configurationUpdated(new AtomFeedServiceConfigType())

      an[IllegalArgumentException] should be thrownBy atomFeedService.registerListener("feedId", mock[AtomFeedListener])
    }

    it("should return a listener ID when a listener is registered") {
      val systemModel = getSystemModelWithService
      val serviceConfig = new AtomFeedServiceConfigType()
      val feedConfig = new AtomFeedConfigType()
      feedConfig.setId("feedId")
      feedConfig.setUri("http://example.com")
      serviceConfig.getFeed.add(feedConfig)

      val atomFeedService = new AtomFeedServiceImpl("clusterId", "nodeId", mockConfigService)
      atomFeedService.init()
      atomFeedService.SystemModelConfigurationListener.configurationUpdated(systemModel)
      atomFeedService.AtomFeedServiceConfigurationListener.configurationUpdated(serviceConfig)

      atomFeedService.registerListener("feedId", mock[AtomFeedListener]) shouldBe a[String]
    }
  }

  describe("unregisterListener") {
    it("should throw an IllegalStateException if the service is not enabled") {
      val systemModel = getSystemModelWithService
      systemModel.getReposeCluster.get(0).getServices.getService.clear()

      val atomFeedService = new AtomFeedServiceImpl("clusterId", "nodeId", mockConfigService)

      an[IllegalStateException] should be thrownBy atomFeedService.unregisterListener("feedId")
    }

    it("should unregister a listener when passed a valid listener ID") {
      val systemModel = getSystemModelWithService
      val serviceConfig = new AtomFeedServiceConfigType()
      val feedConfig = new AtomFeedConfigType()
      feedConfig.setId("feedId")
      feedConfig.setUri("http://example.com")
      serviceConfig.getFeed.add(feedConfig)

      val atomFeedService = new AtomFeedServiceImpl("clusterId", "nodeId", mockConfigService)
      atomFeedService.init()
      atomFeedService.SystemModelConfigurationListener.configurationUpdated(systemModel)
      atomFeedService.AtomFeedServiceConfigurationListener.configurationUpdated(serviceConfig)

      val listenerId = atomFeedService.registerListener("feedId", mock[AtomFeedListener])
      atomFeedService.unregisterListener(listenerId)

      serviceListAppender.getEvents.exists(_.getMessage.getFormattedMessage.contains("Un-registering")) shouldBe true
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

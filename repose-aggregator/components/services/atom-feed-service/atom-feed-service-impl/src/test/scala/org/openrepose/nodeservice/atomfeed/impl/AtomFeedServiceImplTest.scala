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

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import io.opentracing.Tracer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.mockito.Matchers.{eq => isEq, _}
import org.mockito.Mockito.{verify, when}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.httpclient.HttpClientService
import org.openrepose.core.systemmodel.config._
import org.openrepose.docs.repose.atom_feed_service.v1.{AtomFeedServiceConfigType, OpenStackIdentityV2AuthenticationType}
import org.openrepose.nodeservice.atomfeed.impl.actors.NotifierManager.RemoveNotifier
import org.openrepose.nodeservice.atomfeed.AtomFeedListener
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpecLike, Matchers}
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.test.util.ReflectionTestUtils

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class AtomFeedServiceImplTest
  extends TestKit(ActorSystem()) with FunSpecLike with Matchers with MockitoSugar with BeforeAndAfterEach {

  val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
  val serviceListAppender = ctx.getConfiguration.getAppender("serviceList").asInstanceOf[ListAppender]

  var mockConfigService: ConfigurationService = _
  var mockHttpClientService: HttpClientService = _
  var mockTracer: Tracer = _
  var mockAppContext: ApplicationContext = _
  var mockBeanFactory: AutowireCapableBeanFactory = _

  override def beforeEach() = {
    mockConfigService = mock[ConfigurationService]
    mockHttpClientService = mock[HttpClientService]
    mockAppContext = mock[ApplicationContext]
    mockTracer = mock[Tracer]
    mockBeanFactory = mock[AutowireCapableBeanFactory]

    when(mockAppContext.getAutowireCapableBeanFactory).thenReturn(mockBeanFactory)

    serviceListAppender.clear()
  }

  describe("init") {
    it("should register configuration listeners") {
      val atomFeedService = new AtomFeedServiceImpl("1.0", "", mockHttpClientService, mockConfigService, mockAppContext, mockTracer)

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
      val atomFeedService = new AtomFeedServiceImpl("1.0", "", mockHttpClientService, mockConfigService, mockAppContext, mockTracer)

      atomFeedService.destroy()

      verify(mockConfigService).unsubscribeFrom(isEq("atom-feed-service.cfg.xml"), isA(classOf[UpdateListener[_]]))
      verify(mockConfigService).unsubscribeFrom(isEq("system-model.cfg.xml"), isA(classOf[UpdateListener[_]]))
    }
  }

  describe("registerListener") {
    it("should register a notifier with a notifier manager") {
      val atomFeedService = new AtomFeedServiceImpl("1.0", "nodeId", mockHttpClientService, mockConfigService, mockAppContext, mockTracer)

      val listenerIdOne = atomFeedService.registerListener("feedId", mock[AtomFeedListener])
      val listenerIdTwo = atomFeedService.registerListener("feedIdTwo", mock[AtomFeedListener])

      listenerIdOne shouldBe a[String]
      listenerIdTwo shouldBe a[String]
      listenerIdOne should not equal listenerIdTwo
    }
  }

  describe("unregisterListener") {
    it(s"should unregister a listener when passed a valid listener ID") {
      val atomFeedService = new AtomFeedServiceImpl("1.0", "nodeId", mockHttpClientService, mockConfigService, mockAppContext, mockTracer)

      val listenerId = "test-listener-id"
      val notifierManagerProbe = TestProbe()
      val preListenerNotifierManagers = Map(listenerId -> notifierManagerProbe.ref)
      ReflectionTestUtils.setField(atomFeedService, "listenerNotifierManagers", preListenerNotifierManagers)

      atomFeedService.unregisterListener(listenerId)

      val postListenerNotifierManagers = ReflectionTestUtils.getField(atomFeedService, "listenerNotifierManagers").asInstanceOf[Map[_, _]]

      notifierManagerProbe.expectMsg(RemoveNotifier(listenerId))
      postListenerNotifierManagers shouldBe empty
    }

    it("should report if a listener ID is not registered") {
      val atomFeedService = new AtomFeedServiceImpl("1.0", "nodeId", mockHttpClientService, mockConfigService, mockAppContext, mockTracer)

      atomFeedService.unregisterListener("notRegisteredFeedId")

      serviceListAppender.getEvents.exists(_.getMessage.getFormattedMessage.contains("Attempting to unregister")) shouldBe true
      serviceListAppender.getEvents.exists(_.getMessage.getFormattedMessage.contains("not registered")) shouldBe true
    }
  }

  def getSystemModelWithService: SystemModel = {
    val systemModel = new SystemModel()
    val nodes = new NodeList()
    val node = new Node()
    val services = new ServicesList()
    val service = new Service()
    node.setId("nodeId")
    nodes.getNode.add(node)
    service.setName(AtomFeedServiceImpl.ServiceName)
    services.getService.add(service)
    systemModel.setServices(services)
    systemModel.setNodes(nodes)
    systemModel
  }
}

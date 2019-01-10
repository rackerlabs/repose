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
package org.openrepose.powerfilter

import java.net.URL

import javax.servlet.http.HttpServletRequest
import javax.servlet.{Filter, ServletContext}
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, isA, eq => isEq}
import org.mockito.Mockito.{never, verify, when}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.Marshaller.systemModelString
import org.openrepose.core.services.classloader.ClassLoaderManagerService
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.deploy.ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED
import org.openrepose.core.services.deploy.{ApplicationDeploymentEvent, ArtifactManager}
import org.openrepose.core.services.event.EventService
import org.openrepose.core.services.event.impl.SimpleEvent
import org.openrepose.core.services.healthcheck.{HealthCheckService, HealthCheckServiceProxy}
import org.openrepose.core.services.jmx.ConfigurationInformation
import org.openrepose.core.systemmodel.config.{SystemModel, TracingHeaderConfig, Filter => FilterConfig}
import org.openrepose.powerfilter.ReposeFilterLoader.{FilterContext, FilterContextList, FilterContextRegistrar}
import org.openrepose.powerfilter.ReposeFilterLoaderTest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers, OptionValues}
import org.springframework.context.ApplicationContext

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ReposeFilterLoaderTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterEach with OptionValues {
  var configurationService: ConfigurationService = _
  var eventService: EventService = _
  var healthCheckService: HealthCheckService = _
  var healthCheckServiceProxy: HealthCheckServiceProxy = _
  var artifactManager: ArtifactManager = _
  var applicationContext: ApplicationContext = _
  var configurationInformation: ConfigurationInformation = _
  var classLoaderManagerService: ClassLoaderManagerService = _
  var reposeFilterLoader: ReposeFilterLoader = _
  var loggerContext: LoggerContext = _
  var listAppender: ListAppender = _

  override def beforeEach(): Unit = {
    configurationService = mock[ConfigurationService]
    eventService = mock[EventService]
    healthCheckService = mock[HealthCheckService]
    healthCheckServiceProxy = mock[HealthCheckServiceProxy]
    when(healthCheckService.register).thenReturn(healthCheckServiceProxy)
    artifactManager = mock[ArtifactManager]
    applicationContext = mock[ApplicationContext]
    configurationInformation = mock[ConfigurationInformation]
    classLoaderManagerService = mock[ClassLoaderManagerService]

    reposeFilterLoader = new ReposeFilterLoader(
      NodeId,
      configurationService,
      eventService,
      healthCheckService,
      artifactManager,
      applicationContext,
      configurationInformation,
      classLoaderManagerService)

    loggerContext = LogManager.getContext(false).asInstanceOf[LoggerContext]
    listAppender = loggerContext.getConfiguration.getAppender("List0").asInstanceOf[ListAppender]
    listAppender.clear()
  }

  describe("init") {
    it("should subscribe an configuration listener") {
      reposeFilterLoader.init()

      verify(configurationService).subscribeTo(
        isEq("system-model.cfg.xml"),
        any[URL](),
        isA(classOf[UpdateListener[SystemModel]]),
        isA(classOf[Class[SystemModel]]))
      verify(healthCheckService).register()
      verify(eventService).listen(isEq(reposeFilterLoader), any[ApplicationDeploymentEvent])
    }
  }

  describe("destroy") {
    it("should unsubscribe a configuration listener") {
      reposeFilterLoader.destroy()

      verify(configurationService).unsubscribeFrom(
        isEq("system-model.cfg.xml"),
        isA(classOf[UpdateListener[SystemModel]]))
      verify(healthCheckServiceProxy).deregister()
      verify(eventService).squelch(isEq(reposeFilterLoader), any[ApplicationDeploymentEvent])
    }
  }

  describe("isInitialized") {
    it("should return false if a configuration has not yet been read") {
      reposeFilterLoader.isInitialized shouldBe false
    }

    it("should return true if a configuration has been read") {
      reposeFilterLoader.configurationUpdated(systemModelString(SystemModelXml))

      reposeFilterLoader.isInitialized shouldBe true
    }

    it("should not update internal configuration without other inputs") {
      reposeFilterLoader.configurationUpdated(systemModelString(SystemModelXml))

      val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
      messageList.filter(_.contains("Not ready to update yet.")) should have length 1
    }
  }

  describe("onEvent") {
    Seq(
      ("", true, 1),
      (" not", false, 0)
    ).foreach { case (notStr, loaded, len) =>
      it(s"should$notStr try to update internal configuration if all of the artifacts are$notStr loaded yet") {
        when(artifactManager.allArtifactsLoaded()).thenReturn(loaded)
        val artifactName = "stuff"
        reposeFilterLoader.onEvent(new SimpleEvent(
          APPLICATION_COLLECTION_MODIFIED,
          List(artifactName).asJava,
          mock[EventService]))

        val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
        messageList.filter(_.contains(s"Application that changed: [$artifactName]")) should have length 1
        messageList.filter(_.contains("Not ready to update yet.")) should have length len
      }
    }
  }

  describe("setServletContext") {
    it("should not update internal configuration without other inputs") {
      reposeFilterLoader.setServletContext(mock[ServletContext])

      val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
      messageList.filter(_.contains("Not ready to update yet.")) should have length 1
    }
  }

  describe("getFilterContextList") {
    it("should return an empty list if the internals have not been configured") {
      assertTrue("The FilterContextList should be empty.", reposeFilterLoader.getFilterContextList.isEmpty)
    }

    //TODO: can't test this without mocking the entire classloader mechanism :(
    ignore("should return a populated list if the internals have all been configured") {
    }
  }

  describe("getTracingHeaderConfig") {
    it("should return None if a configuration has not yet been read") {
      reposeFilterLoader.getTracingHeaderConfig shouldBe None
    }

    it("should return None if a configuration has been read ") {
      reposeFilterLoader.configurationUpdated(systemModelString(SystemModelXml))

      reposeFilterLoader.getTracingHeaderConfig shouldBe None
    }

    it("should return Some value if a configuration has been read") {
      val systemModel = systemModelString(SystemModelXml)
      systemModel.setTracingHeader(new TracingHeaderConfig())
      reposeFilterLoader.configurationUpdated(systemModel)

      reposeFilterLoader.getTracingHeaderConfig.value should not be null
    }
  }

  describe("Inner Classes") {
    // These are all initialized for no reason other than to make the compiler happy
    var filterConfig = new FilterConfig
    var fooFilter = mock[Filter]
    var fooFilterContext = FilterContext(fooFilter, filterConfig, (request: HttpServletRequest) => true, null)
    var filterContextRegistrar = new FilterContextRegistrar(List(fooFilterContext), None)

    def beforeEachInnerClass(): Unit = {
      filterConfig = new FilterConfig
      filterConfig.setName("foo")
      fooFilter = mock[Filter]
      fooFilterContext = FilterContext(fooFilter, filterConfig, (request: HttpServletRequest) => true, null)
      filterContextRegistrar = new FilterContextRegistrar(List(fooFilterContext), None)
    }

    describe("FilterContextList") {
      describe("close") {
        it("should release the Filter Context List from the Registrar") {
          beforeEachInnerClass()
          filterContextRegistrar = mock[FilterContextRegistrar]
          val filterContextList = new FilterContextList(filterContextRegistrar, List(fooFilterContext), None)
          filterContextList.close()
          verify(filterContextRegistrar).release(isEq(filterContextList))
        }
      }
    }

    describe("FilterContextRegistrar") {
      describe("bind") {
        it("should return a Filter Context List with the filters") {
          beforeEachInnerClass()
          val filterContextList = filterContextRegistrar.bind()
          val filterContextNames = filterContextList.filterContexts.map(_.filterConfig.getName)
          filterContextNames should contain("foo")
        }
      }

      describe("release") {
        it("should not destroy the filters if the registrar is not shutting down") {
          beforeEachInnerClass()
          val filterContextList = filterContextRegistrar.bind()
          filterContextRegistrar.release(filterContextList)
          verify(fooFilter, never).destroy()
        }

        it("should destroy the filters if all Filter Context Lists are released") {
          beforeEachInnerClass()
          val filterContextList = filterContextRegistrar.bind()
          filterContextRegistrar.close()
          verify(fooFilter, never).destroy()
          filterContextRegistrar.release(filterContextList)
          verify(fooFilter).destroy()
        }
      }

      describe("close") {
        it("should destroy the filters if there there are no bound Filter Context Lists") {
          beforeEachInnerClass()
          filterContextRegistrar.close()
          verify(fooFilter).destroy()
        }

        it("should not destroy the filters if not all Filter Context Lists are released") {
          beforeEachInnerClass()
          val filterContextList = filterContextRegistrar.bind()
          filterContextRegistrar.close()
          verify(fooFilter, never).destroy()
        }

        it("should destroy the filters if all Filter Context Lists are released") {
          beforeEachInnerClass()
          val filterContextList = filterContextRegistrar.bind()
          filterContextRegistrar.release(filterContextList)
          verify(fooFilter, never).destroy()
          filterContextRegistrar.close()
          verify(fooFilter).destroy()
        }
      }
    }
  }
}

object ReposeFilterLoaderTest {
  final val NodeId = "NodeId"
  final val FilterName = "some-filter"
  final val SystemModelXml =
    s"""<?xml version="1.0" encoding="UTF-8"?>
       |<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
       |    <nodes>
       |        <node id="$NodeId" hostname="localhost" http-port="8080"/>
       |    </nodes>
       |    <filters>
       |        <filter name="$FilterName"/>
       |    </filters>
       |    <destinations>
       |        <endpoint id="target" protocol="http" port="8081" default="true"/>
       |    </destinations>
       |</system-model>
       |""".stripMargin
}

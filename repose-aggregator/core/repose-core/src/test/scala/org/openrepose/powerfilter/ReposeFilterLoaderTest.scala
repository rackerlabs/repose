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

import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, isA, eq => isEq}
import org.mockito.Mockito.{when, verify}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.classloader.ClassLoaderManagerService
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.deploy.ArtifactManager
import org.openrepose.core.services.event.EventService
import org.openrepose.core.services.healthcheck.{HealthCheckService, HealthCheckServiceProxy}
import org.openrepose.core.services.jmx.ConfigurationInformation
import org.openrepose.core.systemmodel.config.SystemModel
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.context.ApplicationContext

@RunWith(classOf[JUnitRunner])
class ReposeFilterLoaderTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  var nodeId: String = _
  var configurationService: ConfigurationService = _
  var eventService: EventService = _
  var healthCheckService: HealthCheckService = _
  var healthCheckServiceProxy: HealthCheckServiceProxy = _
  var artifactManager: ArtifactManager = _
  var applicationContext: ApplicationContext = _
  var configurationInformation: ConfigurationInformation = _
  var classLoaderManagerService: ClassLoaderManagerService = _
  var reposeFilterLoader: ReposeFilterLoader = _

  override def beforeEach(): Unit = {
    nodeId = "randomNode"
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
      nodeId: String,
      configurationService: ConfigurationService,
      eventService: EventService,
      healthCheckService: HealthCheckService,
      artifactManager: ArtifactManager,
      applicationContext: ApplicationContext,
      configurationInformation: ConfigurationInformation,
      classLoaderManagerService: ClassLoaderManagerService)
  }

  describe("init") {
    it("should subscribe an configuration listener") {
      reposeFilterLoader.init()

      verify(configurationService).subscribeTo(
        isEq("system-model.cfg.xml"),
        any[URL](),
        isA(classOf[UpdateListener[SystemModel]]),
        isA(classOf[Class[SystemModel]]))
    }
  }

  describe("destroy") {
    it("should unsubscribe a configuration listener") {
      reposeFilterLoader.destroy()

      verify(configurationService).unsubscribeFrom(
        isEq("system-model.cfg.xml"),
        isA(classOf[UpdateListener[SystemModel]]))
    }
  }

  describe("setServletContext") {
    // TODO: Add tests.
  }

  describe("getFilterContextList") {
    it("should return false if the service has not been configured") {
      assertTrue("The FilterContextList should be empty.", reposeFilterLoader.getFilterContextList.isEmpty)
    }

    // TODO: Add more tests.
  }

  describe("getTracingHeaderConfig") {
    // TODO: Add tests.
  }

  describe("FilterContextList") {
    // TODO: Add tests.
  }

  describe("FilterContextRegistrar") {
    describe("bind") {
      // TODO: Add tests.
    }

    describe("release") {
      // TODO: Add tests.
    }

    describe("close") {
      // TODO: Add tests.
    }
  }
}

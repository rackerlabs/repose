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
package org.openrepose.core.services.datastore.hazelcast

import java.net.URL

import com.hazelcast.config.Config
import com.hazelcast.instance.HazelcastInstanceFactory
import com.hazelcast.spi.properties.GroupProperty
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, same, eq => isEq}
import org.mockito.Mockito.{never, verify, when}
import org.openrepose.commons.config.resource.ConfigurationResourceResolver
import org.openrepose.commons.config.resource.impl.BufferedURLConfigurationResource
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.hazelcast.HazelcastDatastoreBootstrapTest._
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.healthcheck.{HealthCheckService, HealthCheckServiceProxy, Severity}
import org.openrepose.core.systemmodel.config.{Service, ServicesList, SystemModel}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class HazelcastDatastoreBootstrapTest
  extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  val hazelcastConfigUrl: URL = getClass.getResource("/hazelcast/hazelcast-default.cfg.xml")

  var configurationService: ConfigurationService = _
  var resourceResolver: ConfigurationResourceResolver = _
  var healthCheckService: HealthCheckService = _
  var healthCheckServiceProxy: HealthCheckServiceProxy = _
  var datastoreService: DatastoreService = _
  var hazelcastDatastoreBootstrap: HazelcastDatastoreBootstrap = _

  override def beforeEach(): Unit = {
    HazelcastInstanceFactory.terminateAll()

    configurationService = mock[ConfigurationService]
    resourceResolver = mock[ConfigurationResourceResolver]
    healthCheckService = mock[HealthCheckService]
    healthCheckServiceProxy = mock[HealthCheckServiceProxy]
    datastoreService = mock[DatastoreService]

    when(healthCheckService.register).thenReturn(healthCheckServiceProxy)
    when(configurationService.getResourceResolver).thenReturn(resourceResolver)
    when(resourceResolver.resolve(any[String])).thenReturn(new BufferedURLConfigurationResource(hazelcastConfigUrl))

    hazelcastDatastoreBootstrap = new HazelcastDatastoreBootstrap(
      configurationService,
      healthCheckService,
      datastoreService
    )

    hazelcastDatastoreBootstrap.init()
  }

  describe("init") {
    it("should register with the health check service") {
      verify(healthCheckService).register()
    }

    it("should subscribe to the system model with the configuration service") {
      verify(configurationService).subscribeTo(
        isEq("system-model.cfg.xml"),
        same(hazelcastDatastoreBootstrap),
        any[Class[SystemModel]]
      )
    }
  }

  describe("configurationUpdated") {
    it("should enable the Hazelcast datastore if listed in the system model and is not running") {
      hazelcastDatastoreBootstrap.configurationUpdated(systemModelWithHazelcast)

      hazelcastDatastoreBootstrap.isInitialized shouldBe true
      verify(datastoreService).createHazelcastDatastore(
        isEq(HazelcastDatastoreBootstrap.DatastoreName),
        any[Config]
      )
      verify(healthCheckServiceProxy).resolveIssue(isEq(HazelcastDatastoreBootstrap.NotConfiguredIssueName))
    }

    it("should not interact with the datastore service if listed and running") {
      when(datastoreService.getDatastore(HazelcastDatastoreBootstrap.DatastoreName))
        .thenReturn(mock[Datastore])

      hazelcastDatastoreBootstrap.configurationUpdated(systemModelWithHazelcast)

      hazelcastDatastoreBootstrap.isInitialized shouldBe true
      verify(datastoreService, never).createHazelcastDatastore(any[String], any[Config])
      verify(datastoreService, never).destroyDatastore(any[String])
      verify(healthCheckServiceProxy).resolveIssue(isEq(HazelcastDatastoreBootstrap.NotConfiguredIssueName))
    }

    it("should disable the Hazelcast datastore if not listed in the system model and is running") {
      when(datastoreService.getDatastore(HazelcastDatastoreBootstrap.DatastoreName))
        .thenReturn(mock[Datastore])

      hazelcastDatastoreBootstrap.configurationUpdated(systemModelWithoutHazelcast)

      hazelcastDatastoreBootstrap.isInitialized shouldBe true
      verify(datastoreService).destroyDatastore(HazelcastDatastoreBootstrap.DatastoreName)
      verify(healthCheckServiceProxy).resolveIssue(isEq(HazelcastDatastoreBootstrap.NotConfiguredIssueName))
    }

    it("should not interact with the datastore service if not listed and not running") {
      hazelcastDatastoreBootstrap.configurationUpdated(systemModelWithoutHazelcast)

      hazelcastDatastoreBootstrap.isInitialized shouldBe true
      verify(datastoreService, never).createHazelcastDatastore(any[String], any[Config])
      verify(datastoreService, never).destroyDatastore(any[String])
      verify(healthCheckServiceProxy).resolveIssue(isEq(HazelcastDatastoreBootstrap.NotConfiguredIssueName))
    }

    it("should report a health check issue when enabling the Hazelcast datastore fails") {
      when(datastoreService.createHazelcastDatastore(any[String], any[Config]))
        .thenThrow(new RuntimeException("Failed to register Hazelcast datastore"))

      an[Exception] should be thrownBy hazelcastDatastoreBootstrap.configurationUpdated(systemModelWithHazelcast)

      verify(healthCheckServiceProxy).reportIssue(
        isEq(HazelcastDatastoreBootstrap.NotConfiguredIssueName),
        any[String],
        same(Severity.BROKEN)
      )
      verify(healthCheckServiceProxy, never).resolveIssue(any[String])
    }

    it("should resolve the Hazelcast configuration with the configuration service") {
      hazelcastDatastoreBootstrap.configurationUpdated(systemModelWithHazelcast)

      verify(resourceResolver).resolve("hazelcast.xml")
    }

    it("should configure Hazelcast to use the slf4j logging adapter") {
      val configCaptor = ArgumentCaptor.forClass(classOf[Config])

      hazelcastDatastoreBootstrap.configurationUpdated(systemModelWithHazelcast)

      verify(datastoreService).createHazelcastDatastore(
        isEq(HazelcastDatastoreBootstrap.DatastoreName),
        configCaptor.capture()
      )

      val hazelcastConfig = configCaptor.getValue
      val loggingType = hazelcastConfig.getProperty(GroupProperty.LOGGING_TYPE.getName)

      loggingType shouldBe "slf4j"
    }
  }

  describe("isInitialized") {
    it("should return false if the system model has not yet been handled") {
      hazelcastDatastoreBootstrap.isInitialized shouldBe false
    }

    it("should return true after the system model has been handled") {
      hazelcastDatastoreBootstrap.configurationUpdated(systemModelWithHazelcast)

      hazelcastDatastoreBootstrap.isInitialized shouldBe true
    }
  }
}

object HazelcastDatastoreBootstrapTest {
  private def systemModelWithHazelcast: SystemModel = {
    val systemModel = new SystemModel()
    val services = new ServicesList()
    val hazelcastService = new Service()
    hazelcastService.setName(HazelcastDatastoreBootstrap.ServiceName)
    services.getService.add(hazelcastService)
    systemModel.setServices(services)
    systemModel
  }

  private def systemModelWithoutHazelcast: SystemModel = {
    val systemModel = systemModelWithHazelcast
    systemModel.getServices.getService.clear()
    systemModel
  }
}

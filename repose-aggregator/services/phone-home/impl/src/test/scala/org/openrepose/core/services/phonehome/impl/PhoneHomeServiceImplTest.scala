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
package org.openrepose.core.services.phonehome.impl

import javax.ws.rs.core.MediaType

import org.mockito.Matchers.{eq => mockitoEq, _}
import org.mockito.Mockito.{times, verify}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.core.systemmodel.{PhoneHomeService => PhoneHomeServiceConfig, _}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.{JsNull, Json}

class PhoneHomeServiceImplTest extends FunSpec with Matchers with MockitoSugar {

  describe("init") {
    it("should register a system model configuration listener") {
      val mockConfigurationService = mock[ConfigurationService]
      val mockAkkaServiceClient = mock[AkkaServiceClient]
      val phoneHomeService = new PhoneHomeServiceImpl(
        "1.0.0",
        "/etc/repose/",
        mockConfigurationService,
        mockAkkaServiceClient)

      phoneHomeService.init()

      verify(mockConfigurationService)
        .subscribeTo(anyString(), any[UpdateListener[SystemModel]](), mockitoEq(classOf[SystemModel]))
    }
  }

  describe("isEnabled") {
    it("should throw an IllegalStateException if the service has not been initialized") {
      val phoneHomeService = new PhoneHomeServiceImpl(null, null, null, null)

      an[IllegalStateException] should be thrownBy phoneHomeService.isEnabled
    }

    it("should return true if the service is configured") {
      val systemModel = new SystemModel()
      val phoneHomeConfig = new PhoneHomeServiceConfig()
      phoneHomeConfig.setEnabled(true)
      systemModel.setPhoneHome(phoneHomeConfig)

      val phoneHomeService = new PhoneHomeServiceImpl(null, null, null, null)
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      phoneHomeService.isEnabled shouldBe true
    }

    it("should return false if the service is not configured") {
      val systemModel = new SystemModel()
      val phoneHomeService = new PhoneHomeServiceImpl(null, null, null, null)
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      phoneHomeService.isEnabled shouldBe false
    }
  }

  describe("sendUpdate") {
    it("should throw an IllegalStateException if the service has not been initialized") {
      val phoneHomeService = new PhoneHomeServiceImpl(null, null, null, null)

      an[IllegalStateException] should be thrownBy phoneHomeService.sendUpdate()
    }

    it("should throw an IllegalStateException if the service is not active") {
      val systemModel = new SystemModel()

      val phoneHomeService = new PhoneHomeServiceImpl(null, null, null, null)
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      an[IllegalStateException] should be thrownBy phoneHomeService.sendUpdate()
    }

    it("should send a JSON message to the data collection point") {
      val collectionUri = "http://phonehome.openrepose.org"

      val systemModel = new SystemModel()
      val reposeCluster = new ReposeCluster()
      val filterList = new FilterList()
      val servicesList = new ServicesList()
      val phoneHomeConfig = new PhoneHomeServiceConfig()

      phoneHomeConfig.setEnabled(true)

      val filterA = new Filter()
      val filterB = new Filter()
      val serviceC = new Service()
      val serviceD = new Service()

      filterA.setName("a")
      filterB.setName("b")
      serviceC.setName("c")
      serviceD.setName("d")

      filterList.getFilter.add(filterA)
      filterList.getFilter.add(filterB)
      servicesList.getService.add(serviceC)
      servicesList.getService.add(serviceD)

      reposeCluster.setFilters(filterList)
      reposeCluster.setServices(servicesList)
      phoneHomeConfig.setCollectionUri(collectionUri)
      phoneHomeConfig.setOriginServiceId("foo-service")
      systemModel.getReposeCluster.add(reposeCluster)
      systemModel.setPhoneHome(phoneHomeConfig)

      val mockConfigurationService = mock[ConfigurationService]
      val mockAkkaServiceClient = mock[AkkaServiceClient]
      val phoneHomeService = new PhoneHomeServiceImpl(
        "1.0.0",
        "/etc/repose/",
        mockConfigurationService,
        mockAkkaServiceClient)

      val expectedMessage = Json.stringify(Json.obj(
        "serviceId" -> "foo-service",
        "contactEmail" -> JsNull,
        "reposeVersion" -> "1.0.0",
        "clusters" -> Json.arr(
          Json.obj(
            "filters" -> Json.arr(
              "a",
              "b"
            ),
            "services" -> Json.arr(
              "c",
              "d"
            )
          )
        )
      ))

      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      phoneHomeService.sendUpdate()

      verify(mockAkkaServiceClient, times(2)).post(
        anyString(),
        mockitoEq(collectionUri),
        anyMapOf(classOf[String], classOf[String]),
        mockitoEq(expectedMessage),
        any[MediaType]())
    }
  }
}

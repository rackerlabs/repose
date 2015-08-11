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

import org.mockito.Matchers.{eq => mockitoEq, _}
import org.mockito.Mockito.verify
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.systemmodel.{PhoneHomeService => PhoneHomeServiceConfig, SystemModel}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

class PhoneHomeServiceImplTest extends FunSpec with Matchers with MockitoSugar {

  describe("init") {
    it("should register a system model configuration listener") {
      val mockConfigurationService = mock[ConfigurationService]
      val phoneHomeService = new PhoneHomeServiceImpl("1.0.0", "/etc/repose/", mockConfigurationService)

      phoneHomeService.init()

      verify(mockConfigurationService)
        .subscribeTo(anyString(), any[UpdateListener[SystemModel]](), mockitoEq(classOf[SystemModel]))
    }
  }

  describe("isActive") {
    it("should throw an IllegalStateException if the service has not been initialized") {
      val phoneHomeService = new PhoneHomeServiceImpl(null, null, null)

      an[IllegalStateException] should be thrownBy phoneHomeService.isActive
    }

    it("should return true if the service is configured") {
      val systemModel = new SystemModel()
      val phoneHomeConfig = new PhoneHomeServiceConfig()
      systemModel.setPhoneHome(phoneHomeConfig)

      val phoneHomeService = new PhoneHomeServiceImpl(null, null, null)
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      phoneHomeService.isActive shouldBe true
    }

    it("should return false if the service is not configured") {
      val systemModel = new SystemModel()
      val phoneHomeService = new PhoneHomeServiceImpl(null, null, null)
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      phoneHomeService.isActive shouldBe false
    }
  }

  describe("sendUpdate") {
    it("should throw an IllegalStateException if the service has not been initialized") {
      val phoneHomeService = new PhoneHomeServiceImpl(null, null, null)

      an[IllegalStateException] should be thrownBy phoneHomeService.sendUpdate()
    }

    it("should throw an IllegalStateException if the service is not active") {
      val systemModel = new SystemModel()

      val phoneHomeService = new PhoneHomeServiceImpl(null, null, null)
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      an[IllegalStateException] should be thrownBy phoneHomeService.sendUpdate()
    }

    // TODO: Add more test cases
  }
}

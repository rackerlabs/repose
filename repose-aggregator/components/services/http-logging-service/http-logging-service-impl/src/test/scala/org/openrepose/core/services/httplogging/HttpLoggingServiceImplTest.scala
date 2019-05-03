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
package org.openrepose.core.services.httplogging

import java.net.URL

import org.junit.runner.RunWith
import org.mockito.Matchers.{any, eq => isEq}
import org.mockito.Mockito.verify
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.httplogging.config.HttpLoggingConfig
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class HttpLoggingServiceImplTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  var configurationService: ConfigurationService = _
  var httpLoggingService: HttpLoggingServiceImpl = _

  override def beforeEach(): Unit = {
    configurationService = mock[ConfigurationService]
    httpLoggingService = new HttpLoggingServiceImpl(configurationService)
  }

  describe("init") {
    it("should register the configuration listener") {
      httpLoggingService.init()

      verify(configurationService).subscribeTo(
        isEq(HttpLoggingServiceImpl.DefaultConfig),
        any[URL],
        any[UpdateListener[HttpLoggingConfig]],
        any[Class[HttpLoggingConfig]]
      )
    }
  }

  describe("destroy") {
    it("should unregister the configuration listener") {
      httpLoggingService.destroy()

      verify(configurationService).unsubscribeFrom(
        isEq(HttpLoggingServiceImpl.DefaultConfig),
        any[UpdateListener[HttpLoggingConfig]]
      )
    }
  }

  describe("open") {
    it("should return a new HTTP logging context") {
      val httpLoggingContext = httpLoggingService.open()

      httpLoggingContext should not be null
      httpLoggingContext shouldBe an[HttpLoggingContext]
    }
  }
}

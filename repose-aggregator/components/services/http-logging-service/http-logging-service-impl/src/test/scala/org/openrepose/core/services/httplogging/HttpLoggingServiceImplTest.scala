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

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.jtwig.JtwigTemplate
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, same, eq => isEq}
import org.mockito.Mockito.{verify, when}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.httplogging.config.HttpLoggingConfig
import org.scalatest.concurrent.Eventually
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.slf4j.Logger
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class HttpLoggingServiceImplTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers with Eventually {

  import HttpLoggingServiceImplTest._

  // Giving tests which use eventually a little more time to run than the default PatienceConfig
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(1, Seconds)), interval = scaled(Span(100, Millis)))

  var logger: Logger = _
  var configurationService: ConfigurationService = _
  var configListener: HttpLoggingConfigListener = _
  var httpLoggingService: HttpLoggingServiceImpl = _

  override def beforeEach(): Unit = {
    logger = mock[Logger]
    configurationService = mock[ConfigurationService]
    configListener = mock[HttpLoggingConfigListener]
    httpLoggingService = new HttpLoggingServiceImpl(configurationService, configListener)
  }

  describe("init") {
    it("should register the configuration listener") {
      httpLoggingService.init()

      verify(configurationService).subscribeTo(
        isEq(HttpLoggingServiceImpl.DefaultConfig),
        any[URL],
        same(configListener),
        any[Class[HttpLoggingConfig]]
      )
    }
  }

  describe("destroy") {
    it("should unregister the configuration listener") {
      httpLoggingService.destroy()

      verify(configurationService).unsubscribeFrom(
        isEq(HttpLoggingServiceImpl.DefaultConfig),
        same(configListener)
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

  describe("close") {
    it("should log messages to the configured logger") {
      val message = "Request handled!"

      when(configListener.loggableTemplates)
        .thenReturn(List(
          LoggableTemplate(
            JtwigTemplate.inlineTemplate(message),
            logger
          )
        ))

      httpLoggingService.close(minimalLoggingContext())

      eventually {
        verify(logger).info(message)
      }
    }

    it("should log configured messages using values from the context being closed") {
      val message = "Method: {{ inboundRequestMethod }}"

      when(configListener.loggableTemplates)
        .thenReturn(List(
          LoggableTemplate(
            JtwigTemplate.inlineTemplate(message),
            logger
          )
        ))

      httpLoggingService.close(minimalLoggingContext())

      eventually {
        verify(logger).info("Method: GET")
      }
    }
  }
}

object HttpLoggingServiceImplTest {
  def minimalLoggingContext(request: HttpServletRequest = new MockHttpServletRequest("GET", "/"),
                            response: HttpServletResponse = new MockHttpServletResponse()): HttpLoggingContext = {
    val httpLoggingContext = new HttpLoggingContext()
    httpLoggingContext.setInboundRequest(request)
    httpLoggingContext.setOutboundRequest(request)
    httpLoggingContext.setOutboundResponse(response)
    httpLoggingContext
  }
}

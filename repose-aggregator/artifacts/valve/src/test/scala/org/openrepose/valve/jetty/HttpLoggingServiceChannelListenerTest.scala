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
package org.openrepose.valve.jetty

import org.eclipse.jetty.server.Request
import org.junit.runner.RunWith
import org.mockito.Mockito.{verify, when}
import org.openrepose.commons.utils.http.CommonRequestAttributes
import org.openrepose.core.services.httplogging.{HttpLoggingContext, HttpLoggingService}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class HttpLoggingServiceChannelListenerTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  var httpLoggingService: HttpLoggingService = _
  var httpLoggingContext: HttpLoggingContext = _
  var httpLoggingServiceChannelListener: HttpLoggingServiceChannelListener = _

  override def beforeEach(): Unit = {
    httpLoggingService = mock[HttpLoggingService]
    httpLoggingContext = mock[HttpLoggingContext]
    httpLoggingServiceChannelListener = new HttpLoggingServiceChannelListener(httpLoggingService)

    when(httpLoggingService.open()).thenReturn(httpLoggingContext)
  }

  describe("onRequestBegin") {
    it("should open a logging context") {
      val request = mock[Request]

      httpLoggingServiceChannelListener.onRequestBegin(request)

      verify(httpLoggingService).open()
    }

    it("should add the request to the logging context") {
      val request = mock[Request]

      httpLoggingServiceChannelListener.onRequestBegin(request)

      verify(httpLoggingContext).setInboundRequest(request)
    }

    it("should add the logging context to the request as an attribute") {
      val request = mock[Request]

      httpLoggingServiceChannelListener.onRequestBegin(request)

      verify(request).setAttribute(CommonRequestAttributes.HTTP_LOGGING_CONTEXT, httpLoggingContext)
    }
  }
}

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

import org.eclipse.jetty.server.{Request, Response}
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mockito.{never, verify, when}
import org.openrepose.commons.utils.http.CommonRequestAttributes
import org.openrepose.core.services.httplogging.{HttpLoggingContext, HttpLoggingService}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class HttpLoggingServiceRequestLogTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  var httpLoggingService: HttpLoggingService = _
  var httpLoggingServiceRequestLog: HttpLoggingServiceRequestLog = _

  override def beforeEach(): Unit = {
    httpLoggingService = mock[HttpLoggingService]
    httpLoggingServiceRequestLog = new HttpLoggingServiceRequestLog(httpLoggingService)
  }

  describe("log") {
    it("should add the response to the context from the request") {
      val request = mock[Request]
      val response = mock[Response]
      val loggingContext = mock[HttpLoggingContext]

      when(request.getAttribute(CommonRequestAttributes.HTTP_LOGGING_CONTEXT))
        .thenReturn(loggingContext, Nil: _*)

      httpLoggingServiceRequestLog.log(request, response)

      verify(loggingContext).setOutboundResponse(response)
    }

    it("should close the context from the request") {
      val request = mock[Request]
      val response = mock[Response]
      val loggingContext = mock[HttpLoggingContext]

      when(request.getAttribute(CommonRequestAttributes.HTTP_LOGGING_CONTEXT))
        .thenReturn(loggingContext, Nil: _*)

      httpLoggingServiceRequestLog.log(request, response)

      verify(httpLoggingService).close(loggingContext)
    }

    it("should fail to close the context gracefully if the provided from the request is invalid") {
      val request = mock[Request]
      val response = mock[Response]

      when(request.getAttribute(CommonRequestAttributes.HTTP_LOGGING_CONTEXT))
        .thenReturn("some random string object", Nil: _*)

      noException should be thrownBy httpLoggingServiceRequestLog.log(request, response)

      verify(httpLoggingService, never).close(any[HttpLoggingContext])
    }

    it("should fail to close the context gracefully if the provided from the request is missing") {
      val request = mock[Request]
      val response = mock[Response]

      when(request.getAttribute(CommonRequestAttributes.HTTP_LOGGING_CONTEXT))
        .thenReturn(null, Nil: _*)

      noException should be thrownBy httpLoggingServiceRequestLog.log(request, response)

      verify(httpLoggingService, never).close(any[HttpLoggingContext])
    }
  }
}

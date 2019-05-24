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

import java.time.Instant

import org.eclipse.jetty.http.MetaData
import org.eclipse.jetty.server.{HttpChannel, Request, Response}
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyLong}
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

  var request: Request = _
  var response: Response = _
  var responseMetadata: MetaData.Response = _
  var httpChannel: HttpChannel = _
  var loggingContext: HttpLoggingContext = _

  override def beforeEach(): Unit = {
    httpLoggingService = mock[HttpLoggingService]
    httpLoggingServiceRequestLog = new HttpLoggingServiceRequestLog(httpLoggingService)

    request = mock[Request]
    response = mock[Response]
    responseMetadata = mock[MetaData.Response]
    httpChannel = mock[HttpChannel]
    loggingContext = mock[HttpLoggingContext]

    when(response.getCommittedMetaData)
      .thenReturn(responseMetadata)
    when(response.getHttpChannel)
      .thenReturn(httpChannel)
    when(request.getAttribute(CommonRequestAttributes.HTTP_LOGGING_CONTEXT))
      .thenReturn(loggingContext, Nil: _*)
  }

  describe("log") {
    it("should add the response to the context from the request") {
      httpLoggingServiceRequestLog.log(request, response)

      verify(loggingContext).setOutboundResponse(response)
    }

    it("should add the status code to the context from the request") {
      val statusCode = 418

      when(responseMetadata.getStatus)
        .thenReturn(statusCode)

      httpLoggingServiceRequestLog.log(request, response)

      verify(loggingContext).setOutboundResponseStatusCode(statusCode)
    }

    it("should add the reason phrase to the context from the request") {
      val reasonPhrase = "Test reason"

      when(responseMetadata.getReason)
        .thenReturn(reasonPhrase)

      httpLoggingServiceRequestLog.log(request, response)

      verify(loggingContext).setOutboundResponseReasonPhrase(reasonPhrase)
    }

    it("should add the bytes written to the context from the request") {
      val bytesWritten = 1234L

      when(httpChannel.getBytesWritten)
        .thenReturn(bytesWritten)

      httpLoggingServiceRequestLog.log(request, response)

      verify(loggingContext).setOutboundResponseBytesWritten(bytesWritten)
    }

    it("should add the content length to the context from the request") {
      val contentLength = 9876L

      when(responseMetadata.getContentLength)
        .thenReturn(contentLength)

      httpLoggingServiceRequestLog.log(request, response)

      verify(loggingContext).setOutboundResponseContentLength(contentLength)
    }

    Set(Long.MinValue, -1L).foreach { contentLength =>
      it(s"should not add the content length to the context from the request if it is $contentLength") {
        when(responseMetadata.getContentLength)
          .thenReturn(contentLength)

        httpLoggingServiceRequestLog.log(request, response)

        verify(loggingContext, never).setOutboundResponseContentLength(anyLong)
      }
    }

    it("should add the time request completed to the context from the request") {
      httpLoggingServiceRequestLog.log(request, response)

      verify(loggingContext).setTimeRequestCompleted(any[Instant])
    }

    it("should close the context from the request") {
      httpLoggingServiceRequestLog.log(request, response)

      verify(httpLoggingService).close(loggingContext)
    }

    it("should fail to close the context gracefully if the provided from the request is invalid") {
      when(request.getAttribute(CommonRequestAttributes.HTTP_LOGGING_CONTEXT))
        .thenReturn("some random string object", Nil: _*)

      noException should be thrownBy httpLoggingServiceRequestLog.log(request, response)

      verify(httpLoggingService, never).close(any[HttpLoggingContext])
    }

    it("should fail to close the context gracefully if the provided from the request is missing") {
      when(request.getAttribute(CommonRequestAttributes.HTTP_LOGGING_CONTEXT))
        .thenReturn(null, Nil: _*)

      noException should be thrownBy httpLoggingServiceRequestLog.log(request, response)

      verify(httpLoggingService, never).close(any[HttpLoggingContext])
    }
  }
}

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

import java.time.{Duration, Instant}
import java.util

import org.junit.runner.RunWith
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.logging.TracingHeaderHelper
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class HttpLoggingContextMapTest extends FunSpec with Matchers {

  final val TestHeaderName = "test-header"

  describe("from") {
    it("should provide an entry for inbound request headers") {
      val context = new HttpLoggingContext()
      val request = new MockHttpServletRequest()
      context.setInboundRequest(request)
      request.addHeader(TestHeaderName, "one,two, three")
      request.addHeader(TestHeaderName, "four")

      val contextMap = HttpLoggingContextMap.from(context)

      contextMap should contain key "inboundRequestHeaders"
      contextMap("inboundRequestHeaders") shouldBe a[util.Map[_, _]]
      contextMap("inboundRequestHeaders").asInstanceOf[util.Map[_, _]] should contain key TestHeaderName
      contextMap("inboundRequestHeaders").asInstanceOf[util.Map[_, _]].get(TestHeaderName) shouldBe a[util.List[_]]
      contextMap("inboundRequestHeaders").asInstanceOf[util.Map[_, util.List[_]]].get(TestHeaderName) should contain only("one,two, three", "four")
    }

    it("should provide an entry for outbound request headers") {
      val context = new HttpLoggingContext()
      val request = new MockHttpServletRequest()
      context.setOutboundRequest(request)
      request.addHeader(TestHeaderName, "one,two, three")
      request.addHeader(TestHeaderName, "four")

      val contextMap = HttpLoggingContextMap.from(context)

      contextMap should contain key "outboundRequestHeaders"
      contextMap("outboundRequestHeaders") shouldBe a[util.Map[_, _]]
      contextMap("outboundRequestHeaders").asInstanceOf[util.Map[_, _]] should contain key TestHeaderName
      contextMap("outboundRequestHeaders").asInstanceOf[util.Map[_, _]].get(TestHeaderName) shouldBe a[util.List[_]]
      contextMap("outboundRequestHeaders").asInstanceOf[util.Map[_, util.List[_]]].get(TestHeaderName) should contain only("one,two, three", "four")
    }

    it("should provide an entry for outbound response headers") {
      val context = new HttpLoggingContext()
      val response = new MockHttpServletResponse()
      context.setOutboundResponse(response)
      response.addHeader(TestHeaderName, "one,two, three")
      response.addHeader(TestHeaderName, "four")

      val contextMap = HttpLoggingContextMap.from(context)

      contextMap should contain key "outboundResponseHeaders"
      contextMap("outboundResponseHeaders") shouldBe a[util.Map[_, _]]
      contextMap("outboundResponseHeaders").asInstanceOf[util.Map[_, _]] should contain key TestHeaderName
      contextMap("outboundResponseHeaders").asInstanceOf[util.Map[_, _]].get(TestHeaderName) shouldBe a[util.List[_]]
      contextMap("outboundResponseHeaders").asInstanceOf[util.Map[_, util.List[_]]].get(TestHeaderName) should contain only("one,two, three", "four")
    }

    it("should provide an entry for the time taken to handle the request") {
      val context = new HttpLoggingContext()
      val timeReceived = Instant.now
      val timeCompleted = timeReceived.plusMillis(127)
      context.setTimeRequestReceived(timeReceived)
      context.setTimeRequestCompleted(timeCompleted)

      val contextMap = HttpLoggingContextMap.from(context)

      contextMap should contain key "timeToHandleRequest"
      contextMap("timeToHandleRequest") shouldBe Duration.between(timeReceived, timeCompleted)
    }

    it("should provide an entry for the trace ID") {
      val context = new HttpLoggingContext()
      val request = new MockHttpServletRequest()
      val requestId = "some-test-request-ID"
      context.setOutboundRequest(request)
      request.addHeader(CommonHttpHeader.TRACE_GUID, TracingHeaderHelper.createTracingHeader(requestId, "origin"))

      val contextMap = HttpLoggingContextMap.from(context)

      contextMap should contain key "traceId"
      contextMap("traceId") shouldBe requestId
    }

    it("should not provide an entry for a value that could not be generated") {
      val contextWithDuration = new HttpLoggingContext()
      contextWithDuration.setTimeInOriginService(Duration.ZERO)

      val mapWithDuration = HttpLoggingContextMap.from(contextWithDuration)

      mapWithDuration should contain key "timeInOriginService"

      val contextWithoutDuration = new HttpLoggingContext()

      val mapWithoutDuration = HttpLoggingContextMap.from(contextWithoutDuration)

      mapWithoutDuration should not contain key("timeInOriginService")
    }
  }
}

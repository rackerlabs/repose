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
package org.openrepose.commons.utils.opentracing

import org.junit.runner.RunWith
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletRequest

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class HttpRequestCarrierTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  describe("constructor") {
    it("null wrapper") {
      val tracerExtractor = new HttpRequestCarrier(null)

      tracerExtractor.headers shouldBe empty
    }

    it("empty wrapper") {
      val servletRequest = new MockHttpServletRequest()
      val httpServletRequestWrapper = new HttpServletRequestWrapper(servletRequest)
      val tracerExtractor = new HttpRequestCarrier(httpServletRequestWrapper)

      tracerExtractor.headers shouldBe empty
    }

    it("two unique headers in wrapper") {
      val servletRequest = new MockHttpServletRequest()
      servletRequest.addHeader("content-type", "application/json")
      servletRequest.addHeader("accept", "application/json")
      val httpServletRequestWrapper = new HttpServletRequestWrapper(servletRequest)
      val tracerExtractor = new HttpRequestCarrier(httpServletRequestWrapper)

      tracerExtractor.headers.keySet should contain only("accept", "Content-Type")
      tracerExtractor.headers("Content-Type") should have size 1
      tracerExtractor.headers("accept") should have size 1
    }

    it("two non-unique headers in wrapper") {
      val servletRequest = new MockHttpServletRequest()
      servletRequest.addHeader("accept", "application/json")
      servletRequest.addHeader("accept", "application/xml")
      val httpServletRequestWrapper = new HttpServletRequestWrapper(servletRequest)
      val tracerExtractor = new HttpRequestCarrier(httpServletRequestWrapper)

      tracerExtractor.headers.keySet should contain only "accept"
      tracerExtractor.headers("accept") should have size 2
    }
  }

  describe("iterator") {
    it("null wrapper") {
      val tracerExtractor = new HttpRequestCarrier(null)

      tracerExtractor.headers shouldBe empty

      tracerExtractor.iterator().hasNext shouldBe false
    }

    it("empty wrapper") {
      val servletRequest = new MockHttpServletRequest()
      val httpServletRequestWrapper = new HttpServletRequestWrapper(servletRequest)
      val tracerExtractor = new HttpRequestCarrier(httpServletRequestWrapper)

      tracerExtractor.headers shouldBe empty

      tracerExtractor.iterator().hasNext shouldBe false
    }

    it("two unique headers in wrapper") {
      val servletRequest = new MockHttpServletRequest()
      servletRequest.addHeader("content-type", "application/json")
      servletRequest.addHeader("accept", "application/json")
      val httpServletRequestWrapper = new HttpServletRequestWrapper(servletRequest)
      val tracerExtractor = new HttpRequestCarrier(httpServletRequestWrapper)

      tracerExtractor.headers.keySet should contain only("accept", "Content-Type")
      tracerExtractor.headers("Content-Type") should have size 1
      tracerExtractor.headers("accept") should have size 1

      tracerExtractor.iterator().hasNext shouldBe true

      var numberOfEntries = 0
      for (entry <- tracerExtractor) {
        println(s"no uniques entry: $entry")
        numberOfEntries += 1
      }

      numberOfEntries shouldBe 2
    }

    it("two non-unique headers in wrapper") {
      val servletRequest = new MockHttpServletRequest()
      servletRequest.addHeader("accept", "application/json")
      servletRequest.addHeader("accept", "application/xml")
      val httpServletRequestWrapper = new HttpServletRequestWrapper(servletRequest)
      val tracerExtractor = new HttpRequestCarrier(httpServletRequestWrapper)

      tracerExtractor.headers.keySet should contain only "accept"
      tracerExtractor.headers("accept") should have size 2

      tracerExtractor.iterator().hasNext shouldBe true

      var duplicateHeaderCounter = 0
      var numberOfEntries = 0

      for (entry <- tracerExtractor) {
        if (entry.getKey == "accept") duplicateHeaderCounter += 1
        println(s"non-uniques wrapper $entry")
        numberOfEntries += 1
      }

      duplicateHeaderCounter shouldBe 2
      numberOfEntries shouldBe 2
    }
  }
}

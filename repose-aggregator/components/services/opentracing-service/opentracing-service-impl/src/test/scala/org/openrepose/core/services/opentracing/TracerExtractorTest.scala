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
package org.openrepose.core.services.opentracing

import org.junit.runner.RunWith
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.scalatest.{BeforeAndAfter, FunSpec, FunSuite, Matchers}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.springframework.mock.web.MockHttpServletRequest

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class TracerExtractorTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  describe("constructor") {
    it("null wrapper") {
      val tracerExtractor: TracerExtractor = new TracerExtractor(null)

      tracerExtractor.headers shouldBe empty
    }

    it("empty wrapper") {
      val servletRequest = new MockHttpServletRequest()
      val httpServletRequestWrapper: HttpServletRequestWrapper = new HttpServletRequestWrapper(servletRequest)
      val tracerExtractor: TracerExtractor = new TracerExtractor(httpServletRequestWrapper)

      tracerExtractor.headers shouldBe empty
    }

    it("two unique headers in wrapper") {
      val servletRequest = new MockHttpServletRequest()
      servletRequest.addHeader("content-type", "application/json")
      servletRequest.addHeader("accept", "application/json")
      val httpServletRequestWrapper: HttpServletRequestWrapper = new HttpServletRequestWrapper(servletRequest)
      val tracerExtractor: TracerExtractor = new TracerExtractor(httpServletRequestWrapper)

      tracerExtractor.headers.size shouldBe 2
      tracerExtractor.headers.keySet should contain ("Content-Type")
      tracerExtractor.headers.keySet should contain ("accept")
      tracerExtractor.headers.get("Content-Type").get.size shouldBe 1
      tracerExtractor.headers.get("accept").get.size shouldBe 1

    }

    it("two non-unique headers in wrapper") {
      val servletRequest = new MockHttpServletRequest()
      servletRequest.addHeader("accept", "application/json")
      servletRequest.addHeader("accept", "application/xml")
      val httpServletRequestWrapper: HttpServletRequestWrapper = new HttpServletRequestWrapper(servletRequest)
      val tracerExtractor: TracerExtractor = new TracerExtractor(httpServletRequestWrapper)

      tracerExtractor.headers.size shouldBe 1
      tracerExtractor.headers.keySet should contain ("accept")
      tracerExtractor.headers.get("accept").get.size shouldBe 2

    }
  }

  describe("iterator") {
    it("null wrapper") {
      val tracerExtractor: TracerExtractor = new TracerExtractor(null)

      tracerExtractor.headers shouldBe empty

      tracerExtractor.iterator().hasNext shouldBe false
    }

    it("empty wrapper") {
      val servletRequest = new MockHttpServletRequest()
      val httpServletRequestWrapper: HttpServletRequestWrapper = new HttpServletRequestWrapper(servletRequest)
      val tracerExtractor: TracerExtractor = new TracerExtractor(httpServletRequestWrapper)

      tracerExtractor.headers shouldBe empty

      tracerExtractor.iterator().hasNext shouldBe false
    }

    it("two unique headers in wrapper") {
      val servletRequest = new MockHttpServletRequest()
      servletRequest.addHeader("content-type", "application/json")
      servletRequest.addHeader("accept", "application/json")
      val httpServletRequestWrapper: HttpServletRequestWrapper = new HttpServletRequestWrapper(servletRequest)
      val tracerExtractor: TracerExtractor = new TracerExtractor(httpServletRequestWrapper)

      tracerExtractor.headers.size shouldBe 2
      tracerExtractor.headers.keySet should contain ("Content-Type")
      tracerExtractor.headers.keySet should contain ("accept")
      tracerExtractor.headers.get("Content-Type").get.size shouldBe 1
      tracerExtractor.headers.get("accept").get.size shouldBe 1

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
      val httpServletRequestWrapper: HttpServletRequestWrapper = new HttpServletRequestWrapper(servletRequest)
      val tracerExtractor: TracerExtractor = new TracerExtractor(httpServletRequestWrapper)

      tracerExtractor.headers.size shouldBe 1
      tracerExtractor.headers.keySet should contain ("accept")
      tracerExtractor.headers.get("accept").get.size shouldBe 2

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

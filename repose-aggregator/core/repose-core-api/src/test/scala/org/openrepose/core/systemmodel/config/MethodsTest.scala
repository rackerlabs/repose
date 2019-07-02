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

package org.openrepose.core.systemmodel.config

import javax.servlet.ServletInputStream
import org.junit.runner.RunWith
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.scalatest._
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.springframework.mock.web.MockHttpServletRequest

@RunWith(classOf[JUnitRunner])
class MethodsTest extends FunSpec with Matchers with MockitoSugar {
  describe("evaluate") {
    Seq("GET", "DELETE", "POST", "PUT", "PATCH", "HEAD", "OPTIONS", "CONNECT", "TRACE", "ALL").foreach { method =>
      it(s"should return true when the HTTP method verb $method does match the only configured method") {
        val filterCriterion = new Methods()
        val criteria = filterCriterion.getValue
        criteria.add(method)
        val originalRequest = new MockHttpServletRequest
        val inputStream = mock[ServletInputStream]
        originalRequest.setMethod(method)
        val httpServletRequestWrapper = new HttpServletRequestWrapper(originalRequest, inputStream)

        filterCriterion.evaluate(httpServletRequestWrapper) shouldBe true
      }

      it(s"should return true when the HTTP method verb $method does match one of the configured methods") {
        val filterCriterion = new Methods()
        val criteria = filterCriterion.getValue
        criteria.add("CRAZY")
        criteria.add(method)
        criteria.add("STUPID")
        val originalRequest = new MockHttpServletRequest
        val inputStream = mock[ServletInputStream]
        originalRequest.setMethod(method)
        val httpServletRequestWrapper = new HttpServletRequestWrapper(originalRequest, inputStream)

        filterCriterion.evaluate(httpServletRequestWrapper) shouldBe true
      }

      it(s"should return false when the HTTP method verb $method does not match the configured method") {
        val filterCriterion = new Methods()
        val criteria = filterCriterion.getValue
        criteria.add("NOTME")
        val originalRequest = new MockHttpServletRequest
        val inputStream = mock[ServletInputStream]
        originalRequest.setMethod(method)
        val httpServletRequestWrapper = new HttpServletRequestWrapper(originalRequest, inputStream)

        filterCriterion.evaluate(httpServletRequestWrapper) shouldBe false
      }

      it(s"should return false when the HTTP method verb $method does not match any of the configured methods") {
        val filterCriterion = new Methods()
        val criteria = filterCriterion.getValue
        criteria.add("CRAZY")
        criteria.add("NOTME")
        criteria.add("STUPID")
        val originalRequest = new MockHttpServletRequest
        val inputStream = mock[ServletInputStream]
        originalRequest.setMethod(method)
        val httpServletRequestWrapper = new HttpServletRequestWrapper(originalRequest, inputStream)

        filterCriterion.evaluate(httpServletRequestWrapper) shouldBe false
      }
    }
  }
}

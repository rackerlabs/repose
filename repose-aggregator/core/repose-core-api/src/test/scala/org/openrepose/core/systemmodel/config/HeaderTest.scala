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
class HeaderTest extends FunSpec with Matchers with MockitoSugar {
  describe("evaluate") {
    val withValue = Seq(
      ("this", "one"),
      ("This", "One"),
      ("ThIs", "OnE"),
      ("tHIS", "oNE")
    )

    val nullValue = Seq(
      ("this-one", null),
      ("This-One", null),
      ("ThIs-OnE", null),
      ("tHIS-oNE", null)
    )

    (withValue ++ nullValue).foreach { case (name, value) =>
      it(s"should return true when the header $name:$value does match the only header") {
        val filterCriterion = new Header()
        filterCriterion.setName(name)
        filterCriterion.setValue(value)
        val originalRequest = new MockHttpServletRequest
        val inputStream = mock[ServletInputStream]
        originalRequest.addHeader(name, Option(value).getOrElse("Don't Care"))
        val httpServletRequestWrapper = new HttpServletRequestWrapper(originalRequest, inputStream)

        filterCriterion.evaluate(httpServletRequestWrapper) shouldBe true
      }

      it(s"should return true when the header $name:$value does match the only header regardless of case") {
        val filterCriterion = new Header()
        filterCriterion.setName(name)
        filterCriterion.setValue(value)
        val originalRequest = new MockHttpServletRequest
        val inputStream = mock[ServletInputStream]
        originalRequest.addHeader(name.toUpperCase, Option(value).getOrElse("Don't Care"))
        val httpServletRequestWrapper = new HttpServletRequestWrapper(originalRequest, inputStream)

        filterCriterion.evaluate(httpServletRequestWrapper) shouldBe true
      }

      it(s"should return true when the header $name:$value matches one of the headers") {
        val filterCriterion = new Header()
        filterCriterion.setName(name)
        filterCriterion.setValue(value)
        val originalRequest = new MockHttpServletRequest
        val inputStream = mock[ServletInputStream]
        originalRequest.addHeader("CRAZY", "")
        originalRequest.addHeader(name, Option(value).getOrElse("Don't Care"))
        originalRequest.addHeader("STUPIDLY", "BaD")
        val httpServletRequestWrapper = new HttpServletRequestWrapper(originalRequest, inputStream)

        filterCriterion.evaluate(httpServletRequestWrapper) shouldBe true
      }

      it(s"should return false when the header $name:$value does not match the header") {
        val filterCriterion = new Header()
        filterCriterion.setName(name)
        filterCriterion.setValue(value)
        val originalRequest = new MockHttpServletRequest
        val inputStream = mock[ServletInputStream]
        originalRequest.addHeader("NOT", "me")
        val httpServletRequestWrapper = new HttpServletRequestWrapper(originalRequest, inputStream)

        filterCriterion.evaluate(httpServletRequestWrapper) shouldBe false
      }

      it(s"should return false when the header $name:$value does not match any of the headers") {
        val filterCriterion = new Header()
        filterCriterion.setName(name)
        filterCriterion.setValue(value)
        val originalRequest = new MockHttpServletRequest
        val inputStream = mock[ServletInputStream]
        originalRequest.addHeader("CRAZY", "")
        originalRequest.addHeader("NOT", "me")
        originalRequest.addHeader("STUPIDLY", "BaD")
        val httpServletRequestWrapper = new HttpServletRequestWrapper(originalRequest, inputStream)

        filterCriterion.evaluate(httpServletRequestWrapper) shouldBe false
      }
    }

    withValue.foreach { case (name, value) =>
      it(s"should return false when the header $name:$value does not match the header values case") {
        val filterCriterion = new Header()
        filterCriterion.setName(name)
        filterCriterion.setValue(value)
        val originalRequest = new MockHttpServletRequest
        val inputStream = mock[ServletInputStream]
        originalRequest.addHeader(name, value.toUpperCase)
        val httpServletRequestWrapper = new HttpServletRequestWrapper(originalRequest, inputStream)

        filterCriterion.evaluate(httpServletRequestWrapper) shouldBe false
      }
    }
  }
}

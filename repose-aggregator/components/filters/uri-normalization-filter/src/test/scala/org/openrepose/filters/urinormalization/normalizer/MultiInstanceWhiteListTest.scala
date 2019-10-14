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
package org.openrepose.filters.urinormalization.normalizer

import org.junit.runner.RunWith
import org.openrepose.filters.urinormalization.config.{HttpUriParameterList, UriParameter}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class MultiInstanceWhiteListTest extends FunSpec with BeforeAndAfterEach with Matchers {

  var whiteList: MultiInstanceWhiteList = _

  override def beforeEach() = {
    val parameterList = new HttpUriParameterList()
    parameterList.getParameter add {
      val parameterA = new UriParameter()
      parameterA.setName("a")
      parameterA.setCaseSensitive(false)
      parameterA.setMultiplicity(2)
      parameterA
    }
    parameterList.getParameter add {
      val parameterB = new UriParameter()
      parameterB.setName("b")
      parameterB.setCaseSensitive(true)
      parameterB.setMultiplicity(4)
      parameterB
    }
    parameterList.getParameter add {
      val parameterC = new UriParameter()
      parameterC.setName("c")
      parameterC.setCaseSensitive(true)
      parameterC.setMultiplicity(0)
      parameterC
    }

    whiteList = new MultiInstanceWhiteList(parameterList)
  }

  describe("shouldAccept") {
    it("should filter parameters in whitelist") {
      whiteList.shouldAccept("a") shouldBe true
      whiteList.shouldAccept("A") shouldBe true

      whiteList.shouldAccept("a") shouldBe false
      whiteList.shouldAccept("test") shouldBe false
      whiteList.shouldAccept("format") shouldBe false
      whiteList.shouldAccept("B") shouldBe false
    }

    it("should honor unlimited multiplicity") {
      (1 to 1000) foreach { _ =>
        whiteList.shouldAccept("c") shouldBe true
      }
    }

    it("should not accept anything with null parameter") {
      val localWhiteList = new MultiInstanceWhiteList(null)

      localWhiteList.shouldAccept("a") shouldBe false
      localWhiteList.shouldAccept("test") shouldBe false
      localWhiteList.shouldAccept("format") shouldBe false
    }
  }
}

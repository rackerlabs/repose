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
package org.openrepose.commons.test

import org.hamcrest.Description
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class RegexMatcherTest extends FunSpec with Matchers with MockitoSugar {
  import RegexMatcher._

  describe("matchesSafely") {
    List(("f.*", "foo", true), ("f.*", "bar", false)) foreach { case(regex: String, value: String, matches: Boolean) =>
      it(s"should return $matches with regex $regex and value $value") {
        matchesPattern(regex.r.pattern).matchesSafely(value) shouldBe matches
        matchesPattern(regex).matchesSafely(value) shouldBe matches
      }
    }
  }

  describe("describeTo") {
    it("should append to the descriptor") {
      val description = mock[Description]
      Mockito.when(description.appendText(org.mockito.Matchers.anyString())).thenReturn(description)
      val matcher = matchesPattern("f.*")

      matcher.describeTo(description)

      val inOrder = Mockito.inOrder(description)
      inOrder.verify(description).appendText("should match pattern ")
      inOrder.verify(description).appendText("f.*")
    }
  }
}

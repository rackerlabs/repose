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
package org.openrepose.commons.utils.string

import java.util.regex.Matcher

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class RegexStringOperatorsTest extends FunSpec with Matchers {

  describe("RegexString") {
    describe("==~") {
      it("should create a matcher") {
        val regexString = new RegexString("/foo/\\d+")
        (regexString ==~ "/foo/123") shouldBe a[Matcher]
      }
    }

    describe("=~") {
      it("should return true if the regex fully matches the operand") {
        val regexString = new RegexString("/foo/\\d+")
        (regexString =~ "/foo/123") shouldBe true
      }

      it("should return false if the regex partially matches the operand") {
        val regexString = new RegexString("/v2/\\d+")
        (regexString =~ "/foo/123/bar") shouldBe false
      }

      it("should return false if the regex does not match the operand") {
        val regexString = new RegexString("/v2/\\d+")
        (regexString =~ "baz") shouldBe false
      }

      it("should implicitly convert a String to a RegexString") {
        import RegexString._

        val rs: RegexString = "/foo/\\d+"
        rs shouldBe a[RegexString]
      }
    }
  }

  describe("RegexStringOperators") {
    it("should implicitly convert a String to a RegexString") {
      val anonymousTrait = new RegexStringOperators {
        def convert(regex: String): RegexString = regex
      }

      anonymousTrait.convert("/foo/\\d+") shouldBe a[RegexString]
    }
  }
}

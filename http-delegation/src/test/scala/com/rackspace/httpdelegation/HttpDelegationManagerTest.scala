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
package com.rackspace.httpdelegation

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HttpDelegationManagerTest extends FunSuite with Matchers with HttpDelegationManager {

  test("buildDelegationHeaders should return a header map with the appropriate values") {
    val headerMap = buildDelegationHeaders(404, "test", "not found", .8)

    headerMap.keySet should have size 1
    headerMap.keySet should contain(HttpDelegationHeaderNames.Delegated)
    headerMap(HttpDelegationHeaderNames.Delegated) should contain("status_code=404`component=test`message=not found;q=0.8")
  }

  test("parseDelegationHeader should return an object with the data parsed from the input") {
    val testValues = Seq(
      ("404", "foo", "not found", "1"),
      ("404", "foo", "not found", "1."),
      ("404", "foo", "not found", "1.0"),
      ("404", "foo", "not found", ".0")
    ) map { case (statusCode, component, message, quality) =>
      s"status_code=$statusCode`component=$component`message=$message;q=$quality"
    }

    testValues foreach { headerValue =>
      val res = parseDelegationHeader("status_code=404`component=foo`message=not found;q=1")
      res shouldBe a[Success[_]]
      res.get.statusCode should equal(404)
      res.get.component should equal("foo")
      res.get.message should equal("not found")
      res.get.quality should equal(1)
    }
  }

  test("parseDelegationHeader should default quality value to 1") {
    val res = parseDelegationHeader("status_code=404`component=foo`message=not found")
    res shouldBe a[Success[_]]
    res.get.statusCode should equal(404)
    res.get.component should equal("foo")
    res.get.message should equal("not found")
    res.get.quality should equal(1)
  }

  test("parseDelegationHeader should return a Failure if parsing fails") {
    val testValues = Seq(
      ("foo", "bar", "baz", "1"),
      ("404", "bar", "baz;q=", "1"),
      ("404", "bar", "baz", "a.b")
    ) map { case (statusCode, component, message, quality) =>
      s"status_code=$statusCode`component=$component`message=$message;q=$quality"
    }

    testValues foreach { headerValue =>
      parseDelegationHeader(headerValue) shouldBe a[Failure[_]]
    }
  }
}

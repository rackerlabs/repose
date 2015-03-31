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

import java.text.ParseException

class JavaDelegationManagerProxyTest extends FunSuite with Matchers {

  test("buildDelegationHeaders should return a header Java map with the appropriate values") {
    val headerMap = JavaDelegationManagerProxy.buildDelegationHeaders(404, "test", "not found", .8)

    headerMap.isInstanceOf[java.util.Map[_, _]]
    headerMap.keySet should have size 1
    headerMap.keySet should contain(HttpDelegationHeaderNames.Delegated)
    headerMap.get(HttpDelegationHeaderNames.Delegated) should contain("status_code=404`component=test`message=not found;q=0.8")
  }

  test("JavaDelegationManagerProxy.parseDelegationHeader should return a bean with the data parsed from the input") {
    val res = JavaDelegationManagerProxy.parseDelegationHeader("status_code=404`component=foo`message=not found;q=1")
    res shouldBe a[HttpDelegationHeader]
    res.statusCode should equal(404)
    res.component should equal("foo")
    res.message should equal("not found")
    res.quality should equal(1)
  }

  test("JavaDelegationManagerProxy.parseDelegationHeader should default quality value to 1") {
    val res = JavaDelegationManagerProxy.parseDelegationHeader("status_code=404`component=foo`message=not found")
    res shouldBe a[HttpDelegationHeader]
    res.statusCode should equal(404)
    res.component should equal("foo")
    res.message should equal("not found")
    res.quality should equal(1)
  }

  test("JavaDelegationManagerProxy.parseDelegationHeader should throw an exception if parsing fails") {
    a[ParseException] should be thrownBy JavaDelegationManagerProxy.parseDelegationHeader("status_code=foo&component=bar&message=baz;q=a.b")
  }
}

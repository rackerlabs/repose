package com.rackspace.httpdelegation.impl

import com.rackspace.httpdelegation.{HttpDelegationHeaders, HttpDelegationManager}
import org.scalatest.{FunSuite, Matchers}

import scala.util.{Failure, Success}

class HttpDelegationManagerTest extends FunSuite with Matchers with HttpDelegationManager {

  test("buildDelegationHeaders should return a header map with the appropriate values") {
    val headerMap = buildDelegationHeaders(404, "test", "not found", .8)

    headerMap.keySet should have size 1
    headerMap.keySet should contain(HttpDelegationHeaders.Delegated)
    headerMap(HttpDelegationHeaders.Delegated) should contain("status_code=404`component=test`message=not found;q=0.8")
  }

  test("parseDelegationHeader should return a bean with the data parsed from the input") {
    val res = parseDelegationHeader("status_code=404`component=foo`message=not found;q=1")
    res shouldBe a[Success[_]]
    res.get.statusCode should equal(404)
    res.get.component should equal("foo")
    res.get.message should equal("not found")
    res.get.quality should equal(1)
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
    parseDelegationHeader("status_code=foo&component=bar&message=baz;q=a.b") shouldBe a[Failure[_]]
  }
}

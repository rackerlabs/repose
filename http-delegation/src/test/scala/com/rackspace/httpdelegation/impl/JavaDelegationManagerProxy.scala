package com.rackspace.httpdelegation.impl

import com.rackspace.httpdelegation.{JavaDelegationManagerProxy, HttpDelegationHeaders}
import org.scalatest.{Matchers, FunSuite}

class JavaDelegationManagerProxy extends FunSuite with Matchers {

  test("buildDelegationHeaders should return a header Java map with the appropriate values") {
    val headerMap = JavaDelegationManagerProxy.buildDelegationHeaders(404, "test", "not found", .8)

    headerMap.keySet should have size 1
    headerMap.keySet should contain(HttpDelegationHeaders.Delegated)
    headerMap.get(HttpDelegationHeaders.Delegated) should contain("status_code=404`component=test`message=not found;q=0.8")
  }
}

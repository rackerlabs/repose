package com.rackspace.httpdelegation.impl

import com.rackspace.httpdelegation.HttpDelegationHeaders
import org.scalatest.{FunSuite, Matchers}

class HttpDelegationManagerImplTest extends FunSuite with Matchers {

  test("buildDelegationHeaders should return a header map with the appropriate values") {
    val headerMap = HttpDelegationManagerImpl.buildDelegationHeaders(404, "test", "not found", .8)

    headerMap.keySet should have size 1
    headerMap.keySet should contain(HttpDelegationHeaders.Delegated)
    headerMap(HttpDelegationHeaders.Delegated) should contain("status_code=404`component=test`message=not found;q=0.8")
  }
}

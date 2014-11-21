package com.rackspace.httpdelegation.impl

import com.rackspace.httpdelegation.{HttpDelegationHeaders, JavaDelegationManagerProxy}
import org.scalatest.{FunSuite, Matchers}

class JavaDelegationManagerProxyTest extends FunSuite with Matchers {

  test("buildDelegationHeaders should return a header Java map with the appropriate values") {
    val headerMap = JavaDelegationManagerProxy.buildDelegationHeaders(404, "test", "not found", .8)

    headerMap.isInstanceOf[java.util.Map[_, _]]
    headerMap.keySet should have size 1
    headerMap.keySet should contain(HttpDelegationHeaders.Delegated)
    headerMap.get(HttpDelegationHeaders.Delegated) should contain("status_code=404`component=test`message=not found;q=0.8")
  }
}

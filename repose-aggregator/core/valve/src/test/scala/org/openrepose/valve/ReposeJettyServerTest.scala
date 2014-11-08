package org.openrepose.valve

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ReposeJettyServerTest extends FunSpec with Matchers {

  it("can create a jetty server listening on an HTTP port") {
    pending
  }
  it("can create a jetty server listening on an HTTPS port") {
    pending
  }
  it("can create a jetty server listening on both an HTTP port and an HTTPS port") {
    pending
  }

  it("raises an exception when an HTTPS port is specified, but no ssl config is provided") {
    intercept[ServerInitializationException] {
      new ReposeJettyServer(
        "/etc/repose",
        "cluster",
        "node",
        None,
        Some(8080),
        None,
        false
      )
    }
  }
  it("raises an exception when neither HTTP nor HTTPS port are specified") {
    intercept[ServerInitializationException] {
      new ReposeJettyServer(
        "/etc/repose",
        "cluster",
        "node",
        None,
        None,
        None,
        false
      )
    }
  }

}

package org.openrepose.valve

import org.junit.runner.RunWith
import org.openrepose.core.container.config.SslConfiguration
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ReposeJettyServerTest extends FunSpec with Matchers {

  val sslConfig = {
    val s =new SslConfiguration()
    s.setKeyPassword("password")
    s.setKeystoreFilename("some.file")
    s.setKeystorePassword("lolpassword")

    s
  }

  it("can create a jetty server listening on an HTTP port") {
    val repose = new ReposeJettyServer(
      "/etc/repose",
      "cluster",
      "node",
      Some(8080),
      None,
      None,
      false
    )

    //Cannot verify too much, really can just prove that I have one connector
    repose.server.getConnectors.size shouldBe 1
  }
  it("can create a jetty server listening on an HTTPS port") {
    val repose = new ReposeJettyServer(
      "/etc/repose",
      "cluster",
      "node",
      None,
      Some(8080),
      Some(sslConfig),
      false
    )

    //Cannot verify too much, really can just prove that I have one connector
    repose.server.getConnectors.size shouldBe 1
  }
  it("can create a jetty server listening on both an HTTP port and an HTTPS port") {
    val repose = new ReposeJettyServer(
      "/etc/repose",
      "cluster",
      "node",
      Some(8081),
      Some(8080),
      Some(sslConfig),
      false
    )

    //Cannot verify too much, really can just prove that I have one connector
    repose.server.getConnectors.size shouldBe 2

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

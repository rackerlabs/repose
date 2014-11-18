package org.openrepose.valve

import org.junit.runner.RunWith
import org.openrepose.core.container.config.SslConfiguration
import org.openrepose.core.spring.ReposeSpringProperties
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ReposeJettyServerTest extends FunSpec with Matchers {

  val sslConfig = {
    val s = new SslConfiguration()
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

  it("Can terminate a server, shutting down it's spring context") {
    val server = new ReposeJettyServer(
      "/etc/repose",
      "cluster",
      "node",
      Some(8080),
      None,
      None,
      false
    )

    server.start()
    server.appContext.isActive shouldBe true
    server.appContext.isRunning shouldBe true

    server.shutdown()
    server.appContext.isActive shouldBe false
    server.appContext.isRunning shouldBe false

  }
  it("can be restarted, terminating and restarting everything") {
    val server = new ReposeJettyServer(
      "/etc/repose",
      "cluster",
      "node",
      Some(8080),
      None,
      None,
      false
    )

    server.start()
    server.appContext.isActive shouldBe true
    server.appContext.isRunning shouldBe true
    server.server.isRunning shouldBe true

    val server2 = server.restart()

    server2.appContext.isActive shouldBe false
    //Cannot check to see if it's running, because it flips out
    server2.server.isRunning shouldBe false

    server2.start()

    server2.server.isRunning shouldBe true
    server2.appContext.isActive shouldBe true
    server2.appContext.isRunning shouldBe true

    //Clean up this server
    server2.shutdown()
  }

  it("Fails when attempting to start a shutdown server") {
    val server = new ReposeJettyServer(
      "/etc/repose",
      "cluster",
      "node",
      Some(8080),
      None,
      None,
      false
    )
    println(s"app context active: ${server.appContext.isActive}")

    server.start()
    server.shutdown()

    println(s"app context active: ${server.appContext.isActive}")

    //TODO: handle this!
    intercept[Exception] {
      server.start()
    }
  }

  it("Properly configures the spring properties we need") {
    val server = new ReposeJettyServer(
      "/etc/repose",
      "cluster",
      "le_node_id",
      Some(8080),
      None,
      None,
      false
    )
    import ReposeSpringProperties._

    val expectedProperties = Map(
      CLUSTER_ID -> "cluster",
      NODE_ID -> "le_node_id",
      CONFIG_ROOT -> "/etc/repose",
      INSECURE -> "false" //Spring puts this into a string for us
    )

    expectedProperties.foreach { case (k, v) =>
      server.appContext.getEnvironment.getProperty(k) shouldBe v
    }

  }

}

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
package org.openrepose.valve

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.server.{Request, ServerConnector}
import org.junit.runner.RunWith
import org.mockito.Mockito.{verify, verifyZeroInteractions}
import org.openrepose.core.container.config.SslConfiguration
import org.openrepose.core.spring.{CoreSpringProvider, ReposeSpringProperties}
import org.openrepose.valve.ReposeJettyServer.ServerInitializationException
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ReposeJettyServerTest extends FunSpec with Matchers with MockitoSugar {

  SpringContextResetter.resetContext()
  CoreSpringProvider.getInstance().initializeCoreContext("/config/root", false)

  val nodeContext = CoreSpringProvider.getInstance().getNodeContext("cluster", "le_node_id")

  val httpPort = Some(10234)
  val httpsPort = Some(10235)
  val idleTimeout = Some(10236L)
  val soLingerTime = Some(10237)
  val DEFAULT_IDLE_TIMEOUT = 30000
  val DEFAULT_SO_LINGER_TIME = -1

  val sslConfig = {
    val s = new SslConfiguration()
    s.setKeyPassword("password")
    s.setKeystoreFilename("some.file")
    s.setKeystorePassword("lolpassword")

    Some(s)
  }

  it("can create a jetty server listening on an HTTP port") {
    val repose = new ReposeJettyServer(
      nodeContext,
      "cluster",
      "node",
      httpPort,
      None,
      None,
      None,
      None
    )

    //Cannot verify too much, really can just prove that I have one connector
    repose.server.getConnectors.length shouldBe 1
    // and it should have default idleTimeout & soLingerTime
    repose.server.getConnectors.head.getIdleTimeout shouldBe DEFAULT_IDLE_TIMEOUT
    repose.server.getConnectors.head.asInstanceOf[ServerConnector].getSoLingerTime shouldBe DEFAULT_SO_LINGER_TIME
  }

  it("can create a jetty server listening on an HTTPS port") {
    val repose = new ReposeJettyServer(
      nodeContext,
      "cluster",
      "node",
      None,
      httpsPort,
      sslConfig,
      None,
      None
    )

    //Cannot verify too much, really can just prove that I have one connector
    repose.server.getConnectors.length shouldBe 1
    // and it should have the default idleTimeout & soLingerTime
    repose.server.getConnectors.head.getIdleTimeout shouldBe DEFAULT_IDLE_TIMEOUT
    repose.server.getConnectors.head.asInstanceOf[ServerConnector].getSoLingerTime shouldBe DEFAULT_SO_LINGER_TIME
  }

  it("can create a jetty server listening on both an HTTP port and an HTTPS port") {
    val repose = new ReposeJettyServer(
      nodeContext,
      "cluster",
      "node",
      httpPort,
      httpsPort,
      sslConfig,
      None,
      None
    )

    //Cannot verify too much, really can just prove that I have two connectors
    repose.server.getConnectors.length shouldBe 2
  }

  it("the jetty server listening on an HTTP port and having a non-default idleTimeout") {
    val repose = new ReposeJettyServer(
      nodeContext,
      "cluster",
      "node",
      httpPort,
      None,
      None,
      idleTimeout,
      None
    )

    //Cannot verify too much, really can just prove that I have one connector
    repose.server.getConnectors.length shouldBe 1
    // and it should have the new idleTimeout
    repose.server.getConnectors.head.getIdleTimeout shouldBe idleTimeout.get
    // and it should have the default soLingerTime
    repose.server.getConnectors.head.asInstanceOf[ServerConnector].getSoLingerTime shouldBe DEFAULT_SO_LINGER_TIME
  }

  it("the jetty server listening on an HTTPS port and a non-default soLingerTime") {
    val repose = new ReposeJettyServer(
      nodeContext,
      "cluster",
      "node",
      None,
      httpsPort,
      sslConfig,
      None,
      soLingerTime
    )

    //Cannot verify too much, really can just prove that I have one connector
    repose.server.getConnectors.length shouldBe 1
    // and it should have the default idleTimeout
    repose.server.getConnectors.head.getIdleTimeout shouldBe DEFAULT_IDLE_TIMEOUT
    // and it should have the new soLingerTime
    repose.server.getConnectors.head.asInstanceOf[ServerConnector].getSoLingerTime shouldBe soLingerTime.get
  }

  it("the jetty server listening on both an HTTP port and an HTTPS port has non-default idleTimeout & soLingerTime") {
    val repose = new ReposeJettyServer(
      nodeContext,
      "cluster",
      "node",
      httpPort,
      httpsPort,
      sslConfig,
      idleTimeout,
      soLingerTime
    )

    //Cannot verify too much, really can just prove that I have two connectors
    repose.server.getConnectors.length shouldBe 2
    // and both should have the new idleTimeout and the new soLingerTime
    repose.server.getConnectors foreach { conn => conn.getIdleTimeout shouldBe idleTimeout.get }
    repose.server.getConnectors foreach { conn => conn.asInstanceOf[ServerConnector].getSoLingerTime shouldBe soLingerTime.get }
  }

  it("raises an exception when an HTTPS port is specified, but no ssl config is provided") {
    intercept[ServerInitializationException] {
      new ReposeJettyServer(
        nodeContext,
        "cluster",
        "node",
        None,
        httpsPort,
        None,
        None,
        None
      )
    }
  }

  it("raises an exception when neither HTTP nor HTTPS port are specified") {
    intercept[ServerInitializationException] {
      new ReposeJettyServer(
        nodeContext,
        "cluster",
        "node",
        None,
        None,
        None,
        None,
        None
      )
    }
  }

  it("creates a Jetty server with an error handler that only sets the cache-control header") {
    val baseRequest = mock[Request]
    val request = mock[HttpServletRequest]
    val response = mock[HttpServletResponse]
    val repose = new ReposeJettyServer(
      nodeContext,
      "cluster",
      "node",
      httpPort,
      httpsPort,
      sslConfig,
      idleTimeout,
      soLingerTime
    )

    repose.server.getErrorHandler.handle("some-target", baseRequest, request, response)

    verifyZeroInteractions(request)
    verify(baseRequest).setHandled(true)
    verify(response).setHeader(HttpHeader.CACHE_CONTROL.asString(), repose.server.getErrorHandler.getCacheControl)
  }

  it("Can terminate a server, shutting down the node's entire context") {
    val server = new ReposeJettyServer(
      nodeContext,
      "cluster",
      "node",
      httpPort,
      None,
      None,
      None,
      None
    )

    server.start()
    server.nodeContext.isActive shouldBe true
    server.nodeContext.isRunning shouldBe true

    server.shutdown()
    server.nodeContext.isActive shouldBe false
    server.nodeContext.isRunning shouldBe false

    server.appContext.isActive shouldBe false
    server.appContext.isRunning shouldBe false

  }

  it("can be restarted, terminating and restarting everything") {
    val server = new ReposeJettyServer(
      nodeContext,
      "cluster",
      "node",
      httpPort,
      None,
      None,
      None,
      None
    )

    server.start()
    server.appContext.isActive shouldBe true
    server.appContext.isRunning shouldBe true
    server.server.isRunning shouldBe true

    val server2 = server.restart()

    server2.appContext.isActive shouldBe true
    server2.appContext.isRunning shouldBe true
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
      nodeContext,
      "cluster",
      "node",
      httpPort,
      None,
      None,
      None,
      None
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

  it("has the spring properties we need at this stage") {
    val server = new ReposeJettyServer(
      nodeContext,
      "cluster",
      "le_node_id",
      Some(8080),
      None,
      None,
      None,
      None
    )
    import ReposeSpringProperties.CORE._
    import ReposeSpringProperties.NODE._

    val expectedProperties = Map(
      CLUSTER_ID -> "cluster",
      NODE_ID -> "le_node_id",
      CONFIG_ROOT -> "/config/root",
      INSECURE -> "false" //Spring puts this into a string for us
    ).map { case (k, v) => ReposeSpringProperties.stripSpringValueStupidity(k) -> v }

    expectedProperties.foreach { case (k, v) =>
      server.appContext.getEnvironment.getProperty(k) shouldBe v
    }
  }
}

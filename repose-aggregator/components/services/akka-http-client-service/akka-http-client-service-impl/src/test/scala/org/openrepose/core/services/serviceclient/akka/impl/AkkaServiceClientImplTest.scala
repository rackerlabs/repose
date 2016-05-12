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
package org.openrepose.core.services.serviceclient.akka.impl

import java.io.StringWriter
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.{HttpHeaders, MediaType}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.CoreConnectionPNames
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.{Request, Server, ServerConnector}
import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers.or
import org.mockito.Matchers.{any, anyString, isNull, same}
import org.mockito.Mockito.{verify, when}
import org.openrepose.commons.utils.http.ServiceClientResponse
import org.openrepose.core.service.httpclient.config.{HttpConnectionPoolConfig, PoolType}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.httpclient.{HttpClientContainer, HttpClientService}
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClientException
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class AkkaServiceClientImplTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar with LazyLogging {
  val HEADER_SLEEP = "Origin-Sleep"
  val HEADER_LOG = "Origin-Log"
  val AUTH_TOKEN_HEADER = "X-Auth-Token"
  val BODY_STRING = "BODY"
  val LIST_APPENDER_REF = "List0"
  val httpClientService = mock[HttpClientService]
  val httpClientContainer = mock[HttpClientContainer]
  val configurationService = mock[ConfigurationService]
  val originServer = new Server(0)
  val hashKey = "hashKey"
  var request: HttpGet = _
  var app: ListAppender = _
  var uri: String = _

  override def beforeEach() = {
    val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
    val cfg = ctx.getConfiguration
    app = cfg.getAppender(LIST_APPENDER_REF).asInstanceOf[ListAppender].clear()
    originServer.setHandler(new AbstractHandler() {
      override def handle(s: String, request: Request, httpServletRequest: HttpServletRequest, httpServletResponse: HttpServletResponse): Unit = {
        logger.trace("Starting handle...")
        val timeout = httpServletRequest.getHeader(HEADER_SLEEP)
        val logIt = httpServletRequest.getHeader(HEADER_LOG)
        logger.trace(s"timeout=$timeout")
        if (timeout != null) {
          logger.trace("Sleeping...")
          Thread.sleep(timeout.toLong)
          logger.trace("Continuing...")
        }
        if (logIt != null) {
          logger.error("Origin Server Log Message.")
        }
        httpServletResponse.setContentType(request.getHeader(HttpHeaders.ACCEPT))
        httpServletResponse.setStatus(HttpServletResponse.SC_OK)
        logger.trace("httpServletResponse.getStatus=" + httpServletResponse.getStatus)
        httpServletResponse.getOutputStream.println(BODY_STRING)
        logger.trace(s"httpServletResponse.getBody=$BODY_STRING")
        request.setHandled(true)
        logger.trace("Finished handle.")
      }
    })
    originServer.start()
    val port = originServer.getConnectors.toList.get(0).asInstanceOf[ServerConnector].getLocalPort
    uri = s"http://localhost:$port"
    request = new HttpGet(uri)
    request.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
  }

  override def afterEach() = {
    originServer.stop()
  }

  describe("The Akka Client") {
    val methods = List("GET", "POST")
    when(httpClientService.getClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(httpClientContainer)
    methods.foreach { method =>
      describe(s"using the HTTP request method $method") {
        def akkaServiceClientImplDo(akkaServiceClientImpl: AkkaServiceClientImpl, headers: Map[String, String]): ServiceClientResponse = method match {
          case "GET" => akkaServiceClientImpl.get(hashKey, uri, headers)
          case "POST" => akkaServiceClientImpl.post(hashKey, uri, headers, BODY_STRING, MediaType.APPLICATION_XML_TYPE)
        }
        describe(s"with no headers") {
          val headers = Map[String, String]()

          List(null, "", "test_conn_pool").foreach { connectionPoolId =>
            it(s"should validate token with connection pool id $connectionPoolId") {
              val akkaServiceClientImpl = new AkkaServiceClientImpl(connectionPoolId, httpClientService, configurationService)
              akkaServiceClientImpl.configurationUpdated(createHttpConnectionPoolConfig(Seq(createPool(default = true))))
              val serviceClientResponse = akkaServiceClientImplDo(akkaServiceClientImpl, headers)
              serviceClientResponse should not be null
              serviceClientResponse.getStatus shouldBe HttpServletResponse.SC_OK
            }
          }

          it("should Reuse Service Response") {
            val akkaServiceClientImpl = new AkkaServiceClientImpl(null, httpClientService, configurationService)
            akkaServiceClientImpl.configurationUpdated(createHttpConnectionPoolConfig(Seq(createPool(default = true))))
            val serviceClientResponse1 = akkaServiceClientImplDo(akkaServiceClientImpl, headers)
            val serviceClientResponse2 = akkaServiceClientImplDo(akkaServiceClientImpl, headers)

            val writer1 = new StringWriter()
            IOUtils.copy(serviceClientResponse1.getData, writer1, "UTF-8")
            val returnString1 = writer1.toString

            val writer2 = new StringWriter()
            IOUtils.copy(serviceClientResponse2.getData, writer2, "UTF-8")
            val returnString2 = writer2.toString

            returnString1.trim shouldBe BODY_STRING
            returnString2.trim shouldBe BODY_STRING
          }
        }

        describe("with a log it header") {
          val headers = Map(HEADER_LOG -> true.toString)
          it("should Expire Item In Future Map") {
            val akkaServiceClientImpl = new AkkaServiceClientImpl(null, httpClientService, configurationService)
            akkaServiceClientImpl.configurationUpdated(createHttpConnectionPoolConfig(Seq(createPool(default = true))))
            akkaServiceClientImplDo(akkaServiceClientImpl, headers)

            Thread.sleep(500)

            akkaServiceClientImplDo(akkaServiceClientImpl, headers)

            val events = app.getEvents.toList.map(_.getMessage.getFormattedMessage)
            events.count(_.contains("Origin Server Log Message.")) shouldBe 2
          }
        }

        val timeouts = List(2000 /*, 30000, 45000, 55000, 90000*/)
        timeouts.foreach { timeout =>
          describe(s"with the Socket timeout set to $timeout millis") {
            val httpClientDefault = new DefaultHttpClient
            val params = httpClientDefault.getParams

            when(httpClientContainer.getHttpClient).thenReturn(httpClientDefault)

            it("should succeed when the server response time is LESS than the Socket timeout.") {
              params.setParameter(CoreConnectionPNames.SO_TIMEOUT, timeout)
              httpClientDefault.setParams(params)
              val headers = Map(HEADER_SLEEP -> (timeout - 2000).toString, HttpHeaders.ACCEPT -> MediaType.APPLICATION_XML)
              val akkaServiceClientImpl = new AkkaServiceClientImpl(null, httpClientService, configurationService)
              akkaServiceClientImpl.configurationUpdated(createHttpConnectionPoolConfig(Seq(createPool(default = true, socketTimeout = timeout))))
              val serviceClientResponse = akkaServiceClientImplDo(akkaServiceClientImpl, headers)
              serviceClientResponse should not be null
              serviceClientResponse.getStatus shouldBe HttpServletResponse.SC_OK
              val inputStream = serviceClientResponse.getData
              val content = scala.io.Source.fromInputStream(inputStream).getLines().mkString
              inputStream.close()
              content.trim shouldBe BODY_STRING
            }

            it("should fail with a logged error when the server response time is MORE than the Socket timeout.") {
              params.setParameter(CoreConnectionPNames.SO_TIMEOUT, timeout)
              httpClientDefault.setParams(params)
              val headers = Map(HEADER_SLEEP -> (timeout + 5000).toString, HttpHeaders.ACCEPT -> MediaType.APPLICATION_XML)
              val akkaServiceClientImpl = new AkkaServiceClientImpl(null, httpClientService, configurationService)
              akkaServiceClientImpl.configurationUpdated(createHttpConnectionPoolConfig(Seq(createPool(default = true, socketTimeout = timeout))))
              intercept[AkkaServiceClientException] {
                val serviceClientResponse = akkaServiceClientImplDo(akkaServiceClientImpl, headers)
              }
              val events = app.getEvents.toList.map(_.getMessage.getFormattedMessage)
              events.count(_.contains(s"Error acquiring value from akka ($method) or the cache. Reason: Futures timed out after [")) shouldBe 1
            }
          }
        }
      }
    }
  }

  describe("on construction") {
    it("should register a listener for the http connection pool config") {
      val akkaServiceClientImpl = new AkkaServiceClientImpl("test-pool-id", httpClientService, configurationService)

      verify(configurationService).subscribeTo(anyString(), same(akkaServiceClientImpl), any[Class[HttpConnectionPoolConfig]])
    }
  }

  describe("destroy") {
    it("should unregister a listener for the http connection pool config") {
      val akkaServiceClientImpl = new AkkaServiceClientImpl("test-pool-id", httpClientService, configurationService)
      akkaServiceClientImpl.destroy()

      verify(configurationService).unsubscribeFrom(anyString(), same(akkaServiceClientImpl))
    }
  }

  describe("configurationUpdated") {
    it("should use the default pool settings if a matching pool id is not present") {
      val akkaServiceClientImpl = new AkkaServiceClientImpl("test-pool-id", httpClientService, configurationService)
      akkaServiceClientImpl.configurationUpdated(createHttpConnectionPoolConfig(Seq(createPool(default = true))))

      val events = app.getEvents.toList.map(_.getMessage.getFormattedMessage)
      events.count(_.contains("not available -- using the default pool settings")) shouldBe 1
    }
  }

  def createPool(default: Boolean = false, maxConnections: Int = 100, socketTimeout: Int = 30000): PoolType = {
    val pool = new PoolType

    pool.setDefault(default)
    pool.setId(java.util.UUID.randomUUID().toString)
    pool.setHttpConnManagerMaxTotal(maxConnections)
    pool.setHttpSocketTimeout(socketTimeout)

    pool
  }

  def createHttpConnectionPoolConfig(pools: Seq[PoolType]): HttpConnectionPoolConfig = {
    val hcpc = new HttpConnectionPoolConfig()

    pools.foreach(hcpc.getPool.add)

    hcpc
  }
}

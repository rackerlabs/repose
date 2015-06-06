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
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.config.{ConnectionConfig, MessageConstraints, SocketConfig}
import org.apache.http.conn.ConnectionKeepAliveStrategy
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder}
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.{Request, Server, ServerConnector}
import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers.or
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.openrepose.commons.utils.http.ServiceClientResponse
import org.openrepose.core.services.httpclient.{ExtendedHttpClient, HttpClientResponse, HttpClientService}
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClientException
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class AkkaServiceClientImplTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar with LazyLogging {
  val HEADER_SLEEP = "Origin-Sleep"
  val HEADER_LOG = "Origin-Log"
  val AUTH_TOKEN_HEADER = "X-Auth-Token"
  val BODY_STRING = "BODY"
  val LIST_APPENDER_REF = "List0"
  val httpClientService = mock[HttpClientService]
  val httpClientResponse = mock[HttpClientResponse]
  val originServer = new Server(0)
  val hashKey = "hashKey"
  var request: HttpGet = _
  var app: ListAppender = _
  var uri: String = _

  before {
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

  after {
    originServer.stop()
  }

  describe("The Akka Client") {
    val methods = List("GET", "POST")
    when(httpClientService.getClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(httpClientResponse)
    methods.foreach { method =>
      describe(s"using the HTTP request method $method") {
        def akkaServiceClientImplDo(akkaServiceClientImpl: AkkaServiceClientImpl, headers: Map[String, String]): ServiceClientResponse = method match {
          case "GET" => akkaServiceClientImpl.get(hashKey, uri, headers)
          case "POST" => akkaServiceClientImpl.post(hashKey, uri, headers, BODY_STRING, MediaType.APPLICATION_XML_TYPE)
        }
        describe(s"with no headers") {
          val headers = Map[String, String]()

          it("should Validate Token") {
            val akkaServiceClientImpl = new AkkaServiceClientImpl(httpClientService)
            val serviceClientResponse = akkaServiceClientImplDo(akkaServiceClientImpl, headers)
            serviceClientResponse should not be null
            serviceClientResponse.getStatus shouldBe HttpServletResponse.SC_OK
          }

          it("should Reuse Service Response") {
            val akkaServiceClientImpl = new AkkaServiceClientImpl(httpClientService)
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
            val akkaServiceClientImpl = new AkkaServiceClientImpl(httpClientService)
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
            val httpClientBuilder = HttpClientBuilder.create()
            val socketConfig = SocketConfig.custom()

            when(httpClientService.getMaxConnections(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(20)
            when(httpClientResponse.getExtendedHttpClient).thenReturn(new ExtendedHttpClient {
              override def getKeepAliveStrategy: ConnectionKeepAliveStrategy = ???

              override def getConnectionManager: PoolingHttpClientConnectionManager = ???

              override def getClientInstanceId: String = ???

              override def getConnectionConfig: ConnectionConfig = ???

              override def getRequestConfig: RequestConfig = ???

              override def getMessageConstraints: MessageConstraints = ???

              override def getHttpClient: CloseableHttpClient = httpClientBuilder.build()

              override def getChunkedEncoding: Boolean = ???

              override def getSocketConfig: SocketConfig = ???
            })

            it("should succeed when the server response time is LESS than the Socket timeout.") {
              when(httpClientService.getSocketTimeout(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(timeout)
              httpClientBuilder.setDefaultSocketConfig(socketConfig.setSoTimeout(timeout).build())
              val headers = Map(HEADER_SLEEP -> (timeout - 2000).toString, HttpHeaders.ACCEPT -> MediaType.APPLICATION_XML)
              val akkaServiceClientImpl = new AkkaServiceClientImpl(httpClientService)
              val serviceClientResponse = akkaServiceClientImplDo(akkaServiceClientImpl, headers)
              serviceClientResponse should not be null
              serviceClientResponse.getStatus shouldBe HttpServletResponse.SC_OK
              val inputStream = serviceClientResponse.getData
              val content = io.Source.fromInputStream(inputStream).getLines().mkString
              inputStream.close()
              content.trim shouldBe BODY_STRING
            }

            it("should fail with a logged error when the server response time is MORE than the Socket timeout.") {
              when(httpClientService.getSocketTimeout(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(timeout)
              httpClientBuilder.setDefaultSocketConfig(socketConfig.setSoTimeout(timeout).build())
              val headers = Map(HEADER_SLEEP -> (timeout + 5000).toString, HttpHeaders.ACCEPT -> MediaType.APPLICATION_XML)
              val akkaServiceClientImpl = new AkkaServiceClientImpl(httpClientService)
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
}

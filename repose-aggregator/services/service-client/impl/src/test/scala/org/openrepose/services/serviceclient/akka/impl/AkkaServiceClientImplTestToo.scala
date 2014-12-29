package org.openrepose.services.serviceclient.akka.impl

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.{HttpHeaders, MediaType}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.CoreConnectionPNames
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.{Request, Server}
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.AdditionalMatchers.or
import org.mockito.Mockito.when
import org.openrepose.services.httpclient.{HttpClientResponse, HttpClientService}
import org.rackspace.deproxy.PortFinder
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class AkkaServiceClientImplTestToo extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar with LazyLogging {
  val HEADER_SLEEP = "Origin-Sleep"
  val RESPONSE_BODY = "RESPONSE BODY"
  val LIST_APPENDER_REF = "List0"
  val httpClientService = mock[HttpClientService[AnyRef, HttpClientResponse]]
  val httpClientResponse = mock[HttpClientResponse]
  val httpClient = new DefaultHttpClient
  val params = httpClient.getParams
  var request: HttpGet = _
  val port = PortFinder.Singleton.getNextOpenPort
  val originServer = new Server(port)
  val hashKey = "hashKey"
  val uri = s"http://localhost:$port"
  var app: ListAppender = _
  var httpResponse: HttpResponse = _

  before {
    request = new HttpGet(uri)
    request.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
    val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
    val cfg = ctx.getConfiguration
    app = cfg.getAppender(LIST_APPENDER_REF).asInstanceOf[ListAppender]
    when(httpClientService.getMaxConnections(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(20)
    when(httpClientService.getClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(httpClientResponse)
    when(httpClientResponse.getHttpClient).thenReturn(httpClient)
    originServer.setHandler(new AbstractHandler() {
      override def handle(s: String, request: Request, httpServletRequest: HttpServletRequest, httpServletResponse: HttpServletResponse): Unit = {
        logger.trace("Starting handle...")
        val timeout = httpServletRequest.getHeader(HEADER_SLEEP)
        logger.trace(s"timeout=$timeout")
        if (timeout != null) {
          logger.trace("Sleeping...")
          Thread.sleep(timeout.toLong)
          logger.trace("Continuing...")
        }
        httpServletResponse.setContentType(request.getHeader(HttpHeaders.ACCEPT))
        httpServletResponse.setStatus(HttpServletResponse.SC_OK)
        logger.trace("httpServletResponse.getStatus=" + httpServletResponse.getStatus)
        httpServletResponse.getOutputStream.println(RESPONSE_BODY)
        logger.trace(s"httpServletResponse.getBody=$RESPONSE_BODY")
        request.setHandled(true)
        logger.trace("Finished handle.")
      }
    })
    originServer.start()
  }

  after {
    originServer.stop()
  }

  val timeouts = List(2000/*, 30000, 45000, 55000, 90000*/)
  timeouts.foreach { timeout =>
    describe(s"With an Socket timeout of $timeout millis, the Akka Client using GET should") {
      it("pass.") {
        app should not be null
        app.clear
        when(httpClientService.getSocketTimeout(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(timeout)
        params.setParameter(CoreConnectionPNames.SO_TIMEOUT, timeout)
        httpClient.setParams(params)
        val headers = Map(HEADER_SLEEP -> (timeout - 2000).toString, HttpHeaders.ACCEPT -> MediaType.APPLICATION_XML)
        var content = ""
        val akkaServiceClientImpl = new AkkaServiceClientImpl(httpClientService)
        val serviceClientResponse = akkaServiceClientImpl.get(hashKey, uri, headers)
        serviceClientResponse should not be null
        serviceClientResponse.getStatusCode shouldBe HttpServletResponse.SC_OK
        val inputStream = serviceClientResponse.getData
        content = io.Source.fromInputStream(inputStream).getLines().mkString
        inputStream.close()
        content.trim shouldBe RESPONSE_BODY
        app.getEvents.size shouldBe 0
      }

      it("fail.") {
        app should not be null
        app.clear
        when(httpClientService.getSocketTimeout(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(timeout)
        params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout)
        httpClient.setParams(params)
        val headers = Map(HEADER_SLEEP -> (timeout + 5000).toString, HttpHeaders.ACCEPT -> MediaType.APPLICATION_XML)
        val akkaServiceClientImpl = new AkkaServiceClientImpl(httpClientService)
        val serviceClientResponse = akkaServiceClientImpl.get(hashKey, uri, headers)
        serviceClientResponse shouldBe null
        val events = app.getEvents.toList.map(_.getMessage.getFormattedMessage)
        events should contain("Error acquiring value from akka (GET) or the cache")
      }
    }
  }
}

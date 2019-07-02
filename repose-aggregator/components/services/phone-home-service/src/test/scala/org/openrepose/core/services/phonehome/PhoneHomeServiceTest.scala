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
package org.openrepose.core.services.phonehome

import io.opentracing.Tracer.SpanBuilder
import io.opentracing.{Scope, Span, Tracer}
import org.apache.http.HttpVersion
import org.apache.http.client.methods.{CloseableHttpResponse, HttpEntityEnclosingRequestBase, HttpPost, HttpUriRequest}
import org.apache.http.entity.ContentType
import org.apache.http.message.BasicHttpResponse
import org.apache.http.protocol.HttpContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => mockitoEq, _}
import org.mockito.Mockito.{never, verify, verifyZeroInteractions, when}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.httpclient.{HttpClientService, HttpClientServiceClient}
import org.openrepose.core.systemmodel.config._
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import play.api.libs.json.{JsNull, Json}

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class PhoneHomeServiceTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  import PhoneHomeServiceTest._

  val ctx: LoggerContext = LogManager.getContext(false).asInstanceOf[LoggerContext]
  val filterListAppender: ListAppender = ctx.getConfiguration.getAppender("filterList").asInstanceOf[ListAppender]
  val msgListAppender: ListAppender = ctx.getConfiguration.getAppender("messageList").asInstanceOf[ListAppender]

  var mockTracer: Tracer = mock[Tracer]
  var mockSpanBuilder: SpanBuilder = mock[SpanBuilder]
  var mockScope: Scope = mock[Scope]
  var mockSpan: Span = mock[Span]
  var mockConfigurationService: ConfigurationService = mock[ConfigurationService]
  var mockHttpClientService: HttpClientService = mock[HttpClientService]
  var mockHttpClient: HttpClientServiceClient = mock[HttpClientServiceClient]

  override def beforeEach(): Unit = {
    mockTracer = mock[Tracer]
    mockSpanBuilder = mock[SpanBuilder]
    mockScope = mock[Scope]
    mockSpan = mock[Span]
    mockConfigurationService = mock[ConfigurationService]
    mockHttpClientService = mock[HttpClientService]
    mockHttpClient = mock[HttpClientServiceClient]

    when(mockTracer.buildSpan(anyString())).thenReturn(mockSpanBuilder)
    when(mockSpanBuilder.withTag(anyString(), anyString())).thenReturn(mockSpanBuilder)
    when(mockSpanBuilder.ignoreActiveSpan()).thenReturn(mockSpanBuilder)
    when(mockSpanBuilder.startActive(anyBoolean())).thenReturn(mockScope)
    when(mockScope.span()).thenReturn(mockSpan)

    when(mockHttpClientService.getDefaultClient).thenReturn(mockHttpClient)
  }

  describe("init") {
    it("should register a system model configuration listener") {
      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockHttpClientService)

      phoneHomeService.init()

      verify(mockConfigurationService)
        .subscribeTo(anyString(), any[UpdateListener[SystemModel]](), mockitoEq(classOf[SystemModel]))
    }

    it("should use the factory to get an instance of the akka service client") {
      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockHttpClientService)

      phoneHomeService.init()

      verify(mockHttpClientService).getDefaultClient()
    }
  }

  describe("configurationUpdated") {
    it("should call sendUpdate if the service is enabled") {
      val systemModel = basicSystemModel()
      systemModel.getPhoneHome.setEnabled(true)
      systemModel.getPhoneHome.setCollectionUri(CollectionUri)
      systemModel.getPhoneHome.setOriginServiceId("foo-service")

      when(
        mockHttpClient.execute(
          any[HttpUriRequest],
          any[HttpContext])
      ).thenReturn(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, null) with CloseableHttpResponse {
        override def close(): Unit = {}
      })

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockHttpClientService)

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpEntityEnclosingRequestBase])
      verify(mockHttpClient).execute(
        requestCaptor.capture(),
        any[HttpContext]
      )

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpPost.METHOD_NAME
      request.getURI.toString shouldEqual CollectionUri
      request.getEntity.getContentType.getValue shouldEqual ContentType.APPLICATION_JSON.toString
    }

    it("should not call sendUpdate if the service is not enabled") {
      val systemModel = basicSystemModel()
      systemModel.getPhoneHome.setEnabled(false)
      systemModel.getPhoneHome.setCollectionUri(CollectionUri)
      systemModel.getPhoneHome.setOriginServiceId("foo-service")

      when(
        mockHttpClient.execute(
          any[HttpUriRequest],
          any[HttpContext])
      ).thenReturn(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, null) with CloseableHttpResponse {
        override def close(): Unit = {}
      })

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockHttpClientService)

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      verify(mockHttpClient, never()).execute(
        any[HttpUriRequest],
        any[HttpContext]
      )
    }

    it("should log the message if the phone-home element is not present") {
      val systemModel = basicSystemModel()
      systemModel.setPhoneHome(null)

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockHttpClientService)

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      val msgLogEvents = msgListAppender.getEvents
      val filterLogEvents = filterListAppender.getEvents
      val updateMsg = msgLogEvents.get(0).getMessage.getFormattedMessage
      val lastFilterMsg = filterLogEvents.get(filterLogEvents.size() - 1).getMessage.getFormattedMessage

      verifyZeroInteractions(mockHttpClient)
      msgLogEvents.size() should be > 0
      filterLogEvents.size() should be > 0
      updateMsg should include("1.0.0")
      lastFilterMsg should startWith("Did not attempt to send usage data on update")
    }

    it("should log the message if the phone-home element enabled attribute is false") {
      val systemModel = basicSystemModel()
      systemModel.getPhoneHome.setEnabled(false)
      systemModel.getPhoneHome.setOriginServiceId("foo-service")

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockHttpClientService)

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      val msgLogEvents = msgListAppender.getEvents
      val filterLogEvents = filterListAppender.getEvents
      val updateMsg = msgLogEvents.get(0).getMessage.getFormattedMessage
      val lastFilterMsg = filterLogEvents.get(filterLogEvents.size() - 1).getMessage.getFormattedMessage

      verifyZeroInteractions(mockHttpClient)
      msgLogEvents.size() should be > 0
      filterLogEvents.size() should be > 0
      updateMsg should include("foo-service")
      lastFilterMsg should startWith("Did not attempt to send usage data on update")
    }

    it("should log the message if the post to the collection service fails") {
      val systemModel = basicSystemModel()
      systemModel.getPhoneHome.setEnabled(true)
      systemModel.getPhoneHome.setOriginServiceId("foo-service")

      when(
        mockHttpClient.execute(
          any[HttpUriRequest],
          any[HttpContext])
      ).thenReturn(new BasicHttpResponse(HttpVersion.HTTP_1_1, 400, null) with CloseableHttpResponse {
        override def close(): Unit = {}
      })

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockHttpClientService)

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      val msgLogEvents = msgListAppender.getEvents
      val filterLogEvents = filterListAppender.getEvents
      val updateMsg = msgLogEvents.get(0).getMessage.getFormattedMessage
      val lastFilterMsg = filterLogEvents.get(filterLogEvents.size() - 1).getMessage.getFormattedMessage

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpEntityEnclosingRequestBase])
      verify(mockHttpClient).execute(
        requestCaptor.capture(),
        any[HttpContext]
      )

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpPost.METHOD_NAME
      msgLogEvents.size() should be > 0
      filterLogEvents.size() should be > 0
      updateMsg should include("foo-service")
      lastFilterMsg should include("Update to the collection service failed with status code")
    }

    it("should send a tracing header to the data collection point if configured to") {
      val systemModel = basicSystemModel()
      val tracingHeader = new TracingHeaderConfig
      systemModel.setTracingHeader(tracingHeader)
      tracingHeader.setEnabled(true)
      systemModel.getPhoneHome.setEnabled(true)
      systemModel.getPhoneHome.setCollectionUri(CollectionUri)
      systemModel.getPhoneHome.setOriginServiceId("foo-service")

      when(
        mockHttpClient.execute(
          any[HttpUriRequest],
          any[HttpContext])
      ).thenReturn(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, null) with CloseableHttpResponse {
        override def close(): Unit = {}
      })

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockHttpClientService)

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpEntityEnclosingRequestBase])
      verify(mockHttpClient).execute(
        requestCaptor.capture(),
        any[HttpContext]
      )

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpPost.METHOD_NAME
      request.getURI.toString shouldEqual CollectionUri
      request.getHeaders(CommonHttpHeader.TRACE_GUID) should not be empty
    }

    it("should not send a tracing header to the data collection point if configured not to") {
      val systemModel = basicSystemModel()
      val tracingHeader = new TracingHeaderConfig
      systemModel.setTracingHeader(tracingHeader)
      tracingHeader.setEnabled(false)
      systemModel.getPhoneHome.setEnabled(true)
      systemModel.getPhoneHome.setCollectionUri(CollectionUri)
      systemModel.getPhoneHome.setOriginServiceId("foo-service")

      when(
        mockHttpClient.execute(
          any[HttpUriRequest],
          any[HttpContext])
      ).thenReturn(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, null) with CloseableHttpResponse {
        override def close(): Unit = {}
      })

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockHttpClientService)

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpEntityEnclosingRequestBase])
      verify(mockHttpClient).execute(
        requestCaptor.capture(),
        any[HttpContext]
      )

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpPost.METHOD_NAME
      request.getURI.toString shouldEqual CollectionUri
      request.getHeaders(CommonHttpHeader.TRACE_GUID) shouldBe empty
    }

    it("should send a JSON message to the data collection point") {
      val systemModel = new SystemModel()
      val filterList = new FilterList()
      val servicesList = new ServicesList()
      val phoneHomeConfig = new PhoneHomeServiceConfig()

      phoneHomeConfig.setEnabled(true)

      val filterA = new Filter()
      val filterB = new Filter()
      val serviceC = new Service()
      val serviceD = new Service()

      filterA.setName("a")
      filterB.setName("b")
      serviceC.setName("c")
      serviceD.setName("d")

      filterList.getFilter.add(filterA)
      filterList.getFilter.add(filterB)
      servicesList.getService.add(serviceC)
      servicesList.getService.add(serviceD)

      phoneHomeConfig.setCollectionUri(CollectionUri)
      phoneHomeConfig.setOriginServiceId("foo-service")
      systemModel.setFilters(filterList)
      systemModel.setServices(servicesList)
      systemModel.setPhoneHome(phoneHomeConfig)

      when(
        mockHttpClient.execute(
          any[HttpUriRequest],
          any[HttpContext])
      ).thenReturn(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, null) with CloseableHttpResponse {
        override def close(): Unit = {}
      })

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockHttpClientService)

      val expectedMessage = Json.stringify(Json.obj(
        "serviceId" -> "foo-service",
        "contactEmail" -> JsNull,
        "reposeVersion" -> "1.0.0",
        "filters" -> Json.arr(
          "a",
          "b"
        ),
        "services" -> Json.arr(
          "c",
          "d"
        )
      ))

      // Escape all the JSON to make it RegEx Compatible.
      val expectedBuilder = new StringBuilder(expectedMessage.replaceAll("\\{", "\\\\{").replaceAll("\\}", "\\\\}").replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]"))
      val idx = expectedBuilder.indexOf("foo-service") + """foo-service",""".length
      // Insert the Date/Time/Version RegEx.
      expectedBuilder.insert(idx, """"createdAt":"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}Z","createdAtMillis":[0-9]{13},"jreVersion":".*","jvmName":".*",""")

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpEntityEnclosingRequestBase])
      verify(mockHttpClient).execute(
        requestCaptor.capture(),
        any[HttpContext]
      )

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpPost.METHOD_NAME
      request.getURI.toString shouldEqual CollectionUri
      request.getEntity.getContentType.getValue shouldEqual ContentType.APPLICATION_JSON.toString
      Source.fromInputStream(request.getEntity.getContent).getLines.mkString("\n") should fullyMatch regex expectedBuilder.toString()
    }

    it("should start a new span when configuration is updated") {
      val systemModel = basicSystemModel()
      systemModel.getPhoneHome.setEnabled(true)
      systemModel.getPhoneHome.setOriginServiceId("foo-service")

      when(
        mockHttpClient.execute(
          any[HttpUriRequest],
          any[HttpContext])
      ).thenReturn(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, null) with CloseableHttpResponse {
        override def close(): Unit = {}
      })

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockHttpClientService)

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      verify(mockSpanBuilder).ignoreActiveSpan()
      verify(mockSpanBuilder).startActive(true)
      verify(mockScope).close()
    }
  }
}

object PhoneHomeServiceTest {

  final val CollectionUri = "http://phonehome.openrepose.org"

  def basicSystemModel(): SystemModel = {
    val systemModel = new SystemModel()
    val filterList = new FilterList()
    val servicesList = new ServicesList()
    val phoneHomeConfig = new PhoneHomeServiceConfig()

    systemModel.setFilters(filterList)
    systemModel.setServices(servicesList)
    systemModel.setPhoneHome(phoneHomeConfig)

    systemModel
  }
}

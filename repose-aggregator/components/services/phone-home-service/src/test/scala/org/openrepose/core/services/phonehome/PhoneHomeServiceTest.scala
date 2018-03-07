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

import java.io.ByteArrayInputStream
import javax.ws.rs.core.MediaType

import io.opentracing.Tracer.SpanBuilder
import io.opentracing.{Scope, Span, Tracer}
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.hamcrest.{Matcher, Matchers => HMatchers}
import org.junit.runner.RunWith
import org.mockito.Matchers.{eq => mockitoEq, _}
import org.mockito.Mockito.{never, verify, verifyZeroInteractions, when}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.{CommonHttpHeader, ServiceClientResponse}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientFactory}
import org.openrepose.core.systemmodel.config._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import play.api.libs.json.{JsNull, Json}

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
  var mockAkkaServiceClient: AkkaServiceClient = mock[AkkaServiceClient]
  var mockAkkaServiceClientFactory: AkkaServiceClientFactory = mock[AkkaServiceClientFactory]

  override def beforeEach(): Unit = {
    mockTracer = mock[Tracer]
    mockSpanBuilder = mock[SpanBuilder]
    mockScope = mock[Scope]
    mockSpan = mock[Span]
    mockConfigurationService = mock[ConfigurationService]
    mockAkkaServiceClient = mock[AkkaServiceClient]
    mockAkkaServiceClientFactory = mock[AkkaServiceClientFactory]

    when(mockTracer.buildSpan(anyString())).thenReturn(mockSpanBuilder)
    when(mockSpanBuilder.withTag(anyString(), anyString())).thenReturn(mockSpanBuilder)
    when(mockSpanBuilder.ignoreActiveSpan()).thenReturn(mockSpanBuilder)
    when(mockSpanBuilder.startActive(anyBoolean())).thenReturn(mockScope)
    when(mockScope.span()).thenReturn(mockSpan)

    when(mockAkkaServiceClientFactory.newAkkaServiceClient()).thenReturn(mockAkkaServiceClient)
  }

  describe("init") {
    it("should register a system model configuration listener") {
      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockAkkaServiceClientFactory)

      phoneHomeService.init()

      verify(mockConfigurationService)
        .subscribeTo(anyString(), any[UpdateListener[SystemModel]](), mockitoEq(classOf[SystemModel]))
    }

    it("should use the factory to get an instance of the akka service client") {
      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockAkkaServiceClientFactory)

      phoneHomeService.init()

      verify(mockAkkaServiceClientFactory).newAkkaServiceClient()
    }
  }

  describe("configurationUpdated") {
    it("should call sendUpdate if the service is enabled") {
      val systemModel = basicSystemModel()
      systemModel.getPhoneHome.setEnabled(true)
      systemModel.getPhoneHome.setCollectionUri(CollectionUri)
      systemModel.getPhoneHome.setOriginServiceId("foo-service")

      when(
        mockAkkaServiceClient.post(anyString(),
          anyString(),
          anyMapOf(classOf[String], classOf[String]),
          anyString(),
          any())
      ).thenReturn(new ServiceClientResponse(200, new ByteArrayInputStream("".getBytes)))

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockAkkaServiceClientFactory)

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      verify(mockAkkaServiceClient).post(
        anyString(),
        mockitoEq(CollectionUri),
        anyMapOf(classOf[String], classOf[String]),
        anyString(),
        mockitoEq(MediaType.APPLICATION_JSON_TYPE))
    }

    it("should not call sendUpdate if the service is not enabled") {
      val systemModel = basicSystemModel()
      systemModel.getPhoneHome.setEnabled(false)
      systemModel.getPhoneHome.setCollectionUri(CollectionUri)
      systemModel.getPhoneHome.setOriginServiceId("foo-service")

      when(
        mockAkkaServiceClient.post(anyString(),
          anyString(),
          anyMapOf(classOf[String], classOf[String]),
          anyString(),
          any())
      ).thenReturn(new ServiceClientResponse(200, new ByteArrayInputStream("".getBytes)))

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockAkkaServiceClientFactory)

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      verify(mockAkkaServiceClient, never()).post(
        anyString(),
        mockitoEq(CollectionUri),
        anyMapOf(classOf[String], classOf[String]),
        anyString(),
        mockitoEq(MediaType.APPLICATION_JSON_TYPE))
    }

    it("should log the message if the phone-home element is not present") {
      val systemModel = basicSystemModel()
      systemModel.setPhoneHome(null)

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockAkkaServiceClientFactory)

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      val msgLogEvents = msgListAppender.getEvents
      val filterLogEvents = filterListAppender.getEvents
      val updateMsg = msgLogEvents.get(0).getMessage.getFormattedMessage
      val lastFilterMsg = filterLogEvents.get(filterLogEvents.size() - 1).getMessage.getFormattedMessage

      verifyZeroInteractions(mockAkkaServiceClient)
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
        mockAkkaServiceClientFactory)

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      val msgLogEvents = msgListAppender.getEvents
      val filterLogEvents = filterListAppender.getEvents
      val updateMsg = msgLogEvents.get(0).getMessage.getFormattedMessage
      val lastFilterMsg = filterLogEvents.get(filterLogEvents.size() - 1).getMessage.getFormattedMessage

      verifyZeroInteractions(mockAkkaServiceClient)
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
        mockAkkaServiceClient.post(anyString(),
          anyString(),
          anyMapOf(classOf[String], classOf[String]),
          anyString(),
          any())
      ).thenReturn(new ServiceClientResponse(400, new ByteArrayInputStream("".getBytes)))

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockAkkaServiceClientFactory)

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      val msgLogEvents = msgListAppender.getEvents
      val filterLogEvents = filterListAppender.getEvents
      val updateMsg = msgLogEvents.get(0).getMessage.getFormattedMessage
      val lastFilterMsg = filterLogEvents.get(filterLogEvents.size() - 1).getMessage.getFormattedMessage

      verify(mockAkkaServiceClient).post(
        anyString(),
        anyString(),
        anyMapOf(classOf[String], classOf[String]),
        anyString(),
        any[MediaType]())
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
        mockAkkaServiceClient.post(anyString(),
          anyString(),
          anyMapOf(classOf[String], classOf[String]),
          anyString(),
          any())
      ).thenReturn(new ServiceClientResponse(200, new ByteArrayInputStream("".getBytes)))

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockAkkaServiceClientFactory)

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      verify(mockAkkaServiceClient).post(
        anyString(),
        mockitoEq(CollectionUri),
        argThat(HMatchers.hasKey(CommonHttpHeader.TRACE_GUID).asInstanceOf[Matcher[java.util.Map[String, String]]]),
        anyString(),
        any[MediaType]())
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
        mockAkkaServiceClient.post(anyString(),
          anyString(),
          anyMapOf(classOf[String], classOf[String]),
          anyString(),
          any())
      ).thenReturn(new ServiceClientResponse(200, new ByteArrayInputStream("".getBytes)))

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockAkkaServiceClientFactory)

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      verify(mockAkkaServiceClient).post(
        anyString(),
        mockitoEq(CollectionUri),
        argThat(HMatchers.not(HMatchers.hasKey(CommonHttpHeader.TRACE_GUID)).asInstanceOf[Matcher[java.util.Map[String, String]]]),
        anyString(),
        any[MediaType]())
    }

    it("should send a JSON message to the data collection point") {
      val systemModel = new SystemModel()
      val reposeCluster = new ReposeCluster()
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

      reposeCluster.setFilters(filterList)
      reposeCluster.setServices(servicesList)
      phoneHomeConfig.setCollectionUri(CollectionUri)
      phoneHomeConfig.setOriginServiceId("foo-service")
      systemModel.getReposeCluster.add(reposeCluster)
      systemModel.setPhoneHome(phoneHomeConfig)

      when(
        mockAkkaServiceClient.post(anyString(),
          anyString(),
          anyMapOf(classOf[String], classOf[String]),
          anyString(),
          any())
      ).thenReturn(new ServiceClientResponse(200, new ByteArrayInputStream("".getBytes)))

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockAkkaServiceClientFactory)

      val expectedMessage = Json.stringify(Json.obj(
        "serviceId" -> "foo-service",
        "contactEmail" -> JsNull,
        "reposeVersion" -> "1.0.0",
        "clusters" -> Json.arr(
          Json.obj(
            "filters" -> Json.arr(
              "a",
              "b"
            ),
            "services" -> Json.arr(
              "c",
              "d"
            )
          )
        )
      ))

      // Escape all the JSON to make it RegEx Compatible.
      val expectedBuilder = new StringBuilder(expectedMessage.replaceAll("\\{", "\\\\{").replaceAll("\\}", "\\\\}").replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]"))
      val idx = expectedBuilder.indexOf("foo-service") + """foo-service",""".length
      // Insert the Date/Time/Version RegEx.
      expectedBuilder.insert(idx, """"createdAt":"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}Z","createdAtMillis":[0-9]{13},"jreVersion":".*","jvmName":".*",""")

      phoneHomeService.init()
      phoneHomeService.SystemModelConfigurationListener.configurationUpdated(systemModel)

      verify(mockAkkaServiceClient).post(
        anyString(),
        mockitoEq(CollectionUri),
        anyMapOf(classOf[String], classOf[String]),
        org.mockito.Matchers.matches(expectedBuilder.toString()),
        mockitoEq(MediaType.APPLICATION_JSON_TYPE))
    }

    it("should start a new span when configuration is updated") {
      val systemModel = basicSystemModel()
      systemModel.getPhoneHome.setEnabled(true)
      systemModel.getPhoneHome.setOriginServiceId("foo-service")

      when(
        mockAkkaServiceClient.post(anyString(),
          anyString(),
          anyMapOf(classOf[String], classOf[String]),
          anyString(),
          any())
      ).thenReturn(new ServiceClientResponse(200, new ByteArrayInputStream("".getBytes)))

      val phoneHomeService = new PhoneHomeService(
        "1.0.0",
        mockTracer,
        mockConfigurationService,
        mockAkkaServiceClientFactory)

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
    val reposeCluster = new ReposeCluster()
    val filterList = new FilterList()
    val servicesList = new ServicesList()
    val phoneHomeConfig = new PhoneHomeServiceConfig()

    reposeCluster.setFilters(filterList)
    reposeCluster.setServices(servicesList)
    systemModel.getReposeCluster.add(reposeCluster)
    systemModel.setPhoneHome(phoneHomeConfig)

    systemModel
  }
}

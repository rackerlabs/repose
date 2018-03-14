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
package org.openrepose.core.services.opentracing

import java.net.URL

import com.uber.jaeger
import io.opentracing.Tracer
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => isEq, _}
import org.mockito.Mockito.{verify, verifyZeroInteractions}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.opentracing.DelegatingTracer
import org.openrepose.core.service.opentracing.config.{JaegerConnectionUdp, JaegerSamplingProbabilistic, _}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.systemmodel.config.SystemModel
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class OpenTracingServiceImplTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  import OpenTracingServiceImplTest._

  var configurationService: ConfigurationService = _
  var tracer: DelegatingTracer = _
  var openTracingService: OpenTracingServiceImpl = _

  override def beforeEach(): Unit = {
    configurationService = mock[ConfigurationService]
    tracer = mock[DelegatingTracer]

    openTracingService = new OpenTracingServiceImpl(configurationService, tracer)
  }

  describe("init") {
    it("should subscribe an opentracing configuration listener") {
      openTracingService.init()

      verify(configurationService).subscribeTo(
        isEq("open-tracing.cfg.xml"),
        any[URL](),
        isA(classOf[UpdateListener[OpenTracingConfig]]),
        isA(classOf[Class[OpenTracingConfig]]))
    }

    it("should subscribe a system model configuration listener") {
      openTracingService.init()

      verify(configurationService).subscribeTo(
        isEq("system-model.cfg.xml"),
        isA(classOf[UpdateListener[SystemModel]]),
        isA(classOf[Class[SystemModel]]))
    }
  }

  describe("destroy") {
    it("should unsubscribe an opentracing configuration listener") {
      openTracingService.destroy()

      verify(configurationService).unsubscribeFrom(
        isEq("open-tracing.cfg.xml"),
        isA(classOf[UpdateListener[OpenTracingConfig]]))
    }

    it("should unsubscribe a system model configuration listener") {
      openTracingService.destroy()

      verify(configurationService).unsubscribeFrom(
        isEq("system-model.cfg.xml"),
        isA(classOf[UpdateListener[SystemModel]]))
    }
  }

  describe("configurationUpdated") {
    it("should not register a tracer if the open tracing service configuration is not updated") {
      openTracingService.SystemModelConfigurationListener.configurationUpdated(new SystemModel())

      verifyZeroInteractions(tracer)
    }

    it("should not register a tracer if the system model configuration is not updated") {
      openTracingService.OpenTracingConfigurationListener.configurationUpdated(
        minimalOpenTracingConfig
      )

      verifyZeroInteractions(tracer)
    }

    it("should register a tracer if both the open tracing and system model configurations are updated") {
      openTracingService.SystemModelConfigurationListener.configurationUpdated(
        new SystemModelBuilder()
          .withOpenTracingHeader("OT-Header")
          .build())
      openTracingService.OpenTracingConfigurationListener.configurationUpdated(
        minimalOpenTracingConfig
      )

      verify(tracer).register(any[Tracer])
    }

    it("should set the service name") {
      val serviceName = "myService"
      val tracerCaptor = ArgumentCaptor.forClass(classOf[jaeger.Tracer])

      openTracingService.SystemModelConfigurationListener.configurationUpdated(
        new SystemModelBuilder()
          .withOpenTracingHeader("OT-Header")
          .build())
      openTracingService.OpenTracingConfigurationListener.configurationUpdated(
        minimalOpenTracingConfig
      )

      verify(tracer).register(tracerCaptor.capture())
      tracerCaptor.getValue.getServiceName shouldEqual serviceName
    }

    // todo: figure out how to inspect all of the configuration in the tracer (despite access protection)

    samplingConfigurations foreach { case (testDescriptor, samplingConfiguration) =>
      it(s"should $testDescriptor...") {
        pending
      }
    }

    connectionConfigurations foreach { case (testDescriptor, connectionConfiguration) =>
      it(s"should $testDescriptor...") {
        pending
      }
    }
  }
}

object OpenTracingServiceImplTest {
  val samplingConfigurations: Map[String, JaegerSampling] = Map(
    "constant sampling off" -> new JaegerSamplingConstant().withToggle(Toggle.OFF),
    "constant sampling on" -> new JaegerSamplingConstant().withToggle(Toggle.ON),
    "rate limiting sampling" -> new JaegerSamplingRateLimiting().withMaxTracesPerSecond(20.0),
    "probabilistic sampling" -> new JaegerSamplingProbabilistic().withProbability(80.0)
  )

  val connectionConfigurations: Map[String, JaegerConnection] = Map(
    "UDP connection" -> new JaegerConnectionUdp().withHost("localhost").withPort(9009),
    "unauthenticated HTTP connection" -> new JaegerConnectionHttp().withEndpoint("http://localhost:14268/path"),
    "token HTTP connection" -> new JaegerConnectionHttp().withEndpoint("http://localhost:14268/path").withToken("myToken"),
    "basic auth HTTP connection" -> new JaegerConnectionHttp().withEndpoint("http://localhost:14268/path").withUsername("myUsername").withPassword("myPassword")
  )

  def minimalOpenTracingConfig(): OpenTracingConfig = {
    new OpenTracingConfig()
      .withServiceName("myService")
      .withTracerConfig(new JaegerTracerConfig()
        .withJaegerConnection(new JaegerConnectionUdp()
          .withHost("localhost")
          .withPort(9009))
        .withJaegerSampling(new JaegerSamplingConstant()))
  }

  class SystemModelBuilder {
    val systemModel = new SystemModel()

    def withOpenTracingHeader(openTracingHeader: String): SystemModelBuilder = {
      systemModel.setOpenTracingHeader(openTracingHeader)
      this
    }

    def build(): SystemModel = systemModel
  }

}

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

import io.jaegertracing.internal.JaegerTracer
import io.jaegertracing.internal.samplers.{ConstSampler, ProbabilisticSampler, RateLimitingSampler}
import io.opentracing.Tracer
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => isEq, _}
import org.mockito.Mockito.verify
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.opentracing.DelegatingTracer
import org.openrepose.core.service.opentracing.config.{JaegerConnectionUdp, JaegerSamplingProbabilistic, _}
import org.openrepose.core.services.config.ConfigurationService
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
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
  }

  describe("destroy") {
    it("should unsubscribe an opentracing configuration listener") {
      openTracingService.destroy()

      verify(configurationService).unsubscribeFrom(
        isEq("open-tracing.cfg.xml"),
        isA(classOf[UpdateListener[OpenTracingConfig]]))
    }
  }

  describe("isInitialized") {
    it("should return false if the service has not been configured") {
      openTracingService.isInitialized shouldBe false
    }

    it("should return true if the service has been configured") {
      openTracingService.configurationUpdated(
        minimalOpenTracingConfig()
      )

      openTracingService.isInitialized shouldBe true
    }
  }

  describe("getJaegerSamplerConfiguration") {
    it("should return constant sampler set to on") {
      val config = new JaegerTracerConfig()
        .withJaegerSampling(new JaegerSamplingConstant()
          .withToggle(Toggle.ON))

      val samplerConfig = openTracingService.getJaegerSamplerConfiguration(config)

      samplerConfig.getType shouldBe ConstSampler.TYPE
      samplerConfig.getParam shouldEqual 1
    }

    it("should return constant sampler set to off") {
      val config = new JaegerTracerConfig()
        .withJaegerSampling(new JaegerSamplingConstant()
          .withToggle(Toggle.OFF))

      val samplerConfig = openTracingService.getJaegerSamplerConfiguration(config)

      samplerConfig.getType shouldBe ConstSampler.TYPE
      samplerConfig.getParam shouldEqual 0
    }

    it("should return rate limiting sampler set to value") {
      val max = 2.0
      val config = new JaegerTracerConfig()
        .withJaegerSampling(new JaegerSamplingRateLimiting()
          .withMaxTracesPerSecond(max))

      val samplerConfig = openTracingService.getJaegerSamplerConfiguration(config)

      samplerConfig.getType shouldBe RateLimitingSampler.TYPE
      samplerConfig.getParam shouldEqual max
    }

    it("should return probabilistic sampler set to value") {
      val probability = .2
      val config = new JaegerTracerConfig()
        .withJaegerSampling(new JaegerSamplingProbabilistic()
          .withProbability(probability))

      val samplerConfig = openTracingService.getJaegerSamplerConfiguration(config)

      samplerConfig.getType shouldBe ProbabilisticSampler.TYPE
      samplerConfig.getParam shouldEqual probability
    }
  }

  describe("getJaegerSenderConfiguration") {
    it("should return sender configuration for UDP") {
      val host = "localhost"
      val port = 12345
      val config = new JaegerTracerConfig()
        .withJaegerConnection(new JaegerConnectionUdp()
          .withHost(host)
          .withPort(port))

      val samplerConfig = openTracingService.getJaegerSenderConfiguration(config)

      samplerConfig.getAgentHost shouldEqual host
      samplerConfig.getAgentPort shouldEqual port
    }

    it("should return sender configuration for HTTP with no authentication") {
      val endpoint = "http://localhost:4004/path"
      val config = new JaegerTracerConfig()
        .withJaegerConnection(new JaegerConnectionHttp()
          .withEndpoint(endpoint))

      val samplerConfig = openTracingService.getJaegerSenderConfiguration(config)

      samplerConfig.getEndpoint shouldEqual endpoint
    }

    it("should return sender configuration for HTTP with token authentication") {
      val endpoint = "http://localhost:4004/path"
      val token = "9823rhu3ifq3fq3"
      val config = new JaegerTracerConfig()
        .withJaegerConnection(new JaegerConnectionHttp()
          .withEndpoint(endpoint)
          .withToken(token))

      val samplerConfig = openTracingService.getJaegerSenderConfiguration(config)

      samplerConfig.getEndpoint shouldEqual endpoint
      samplerConfig.getAuthToken shouldEqual token
    }

    it("should return sender configuration for HTTP with basic authentication") {
      val endpoint = "http://localhost:4004/path"
      val username = "myUsername"
      val password = "myPassword"
      val config = new JaegerTracerConfig()
        .withJaegerConnection(new JaegerConnectionHttp()
          .withEndpoint(endpoint)
          .withUsername(username)
          .withPassword(password))

      val samplerConfig = openTracingService.getJaegerSenderConfiguration(config)

      samplerConfig.getEndpoint shouldEqual endpoint
      samplerConfig.getAuthUsername shouldEqual username
      samplerConfig.getAuthPassword shouldEqual password
    }

    it("should return sender configuration preferring token to basic authentication") {
      val endpoint = "http://localhost:4004/path"
      val token = "0923rh23qirhq2"
      val username = "myUsername"
      val password = "myPassword"
      val config = new JaegerTracerConfig()
        .withJaegerConnection(new JaegerConnectionHttp()
          .withEndpoint(endpoint)
          .withToken(token)
          .withUsername(username)
          .withPassword(password))

      val samplerConfig = openTracingService.getJaegerSenderConfiguration(config)

      samplerConfig.getEndpoint shouldEqual endpoint
      samplerConfig.getAuthToken shouldBe token
      samplerConfig.getAuthUsername shouldBe null
      samplerConfig.getAuthPassword shouldBe null
    }
  }

  describe("configurationUpdated") {
    it("should set the service name") {
      val serviceName = "myService"
      val tracerCaptor = ArgumentCaptor.forClass(classOf[JaegerTracer])

      openTracingService.configurationUpdated(
        minimalOpenTracingConfig().withServiceName(serviceName)
      )

      verify(tracer).register(tracerCaptor.capture())
      tracerCaptor.getValue.getServiceName shouldEqual serviceName
    }

    // todo: figure out how to inspect all of the configuration in the tracer (despite access protection)

    Set(true, false) foreach { logSpans =>
      it(s"should register a tracer with log spans set to $logSpans") {
        val config = minimalOpenTracingConfig()
        config.getTracerConfig.withLogSpans(logSpans)

        openTracingService.configurationUpdated(config)

        verify(tracer).register(any[Tracer])
      }
    }

    Set(1, 1000, 2000) foreach { flushInterval =>
      it(s"should register a tracer with flush interval set to $flushInterval") {
        val config = minimalOpenTracingConfig()
        config.getTracerConfig.withFlushIntervalMs(flushInterval)

        openTracingService.configurationUpdated(config)

        verify(tracer).register(any[Tracer])
      }
    }

    Set(1, 100, 200) foreach { maxBufferSize =>
      it(s"should register a tracer with max buffer size set to $maxBufferSize") {
        val config = minimalOpenTracingConfig()
        config.getTracerConfig.withFlushIntervalMs(maxBufferSize)

        openTracingService.configurationUpdated(config)

        verify(tracer).register(any[Tracer])
      }
    }

    samplingConfigurations foreach { case (testDescriptor, samplingConfiguration) =>
      it(s"should register a tracer with $testDescriptor...") {
        val config = minimalOpenTracingConfig()
        config.getTracerConfig.withJaegerSampling(samplingConfiguration)

        openTracingService.configurationUpdated(config)

        verify(tracer).register(any[Tracer])
      }
    }

    connectionConfigurations foreach { case (testDescriptor, connectionConfiguration) =>
      it(s"should register a tracer with $testDescriptor...") {
        val config = minimalOpenTracingConfig()
        config.getTracerConfig.withJaegerConnection(connectionConfiguration)

        openTracingService.configurationUpdated(config)

        verify(tracer).register(any[Tracer])
      }
    }
  }
}

object OpenTracingServiceImplTest {
  val samplingConfigurations: Map[String, JaegerSampling] = Map(
    "constant sampling off" -> new JaegerSamplingConstant().withToggle(Toggle.OFF),
    "constant sampling on" -> new JaegerSamplingConstant().withToggle(Toggle.ON),
    "rate limiting sampling" -> new JaegerSamplingRateLimiting().withMaxTracesPerSecond(20.0),
    "probabilistic sampling" -> new JaegerSamplingProbabilistic().withProbability(.8)
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
}

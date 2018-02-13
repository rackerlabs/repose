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

import io.opentracing.Tracer
import io.opentracing.noop.{NoopTracerFactory}
import java.lang.reflect
import java.lang.reflect.Field

import io.opentracing.util.GlobalTracer
import org.junit.runner.RunWith
import org.mockito.Matchers.{eq => isEq, _}
import org.mockito.Mockito.verify
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.config.parser.common.ConfigurationParser
import org.openrepose.commons.config.resource.ConfigurationResourceResolver
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.service.opentracing.config._
import org.openrepose.core.services.opentracing.interceptors.{JaegerRequestInterceptor, JaegerResponseInterceptor}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.slf4j.LoggerFactory

import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class OpenTracingServiceImplTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  describe("init") {
    it("should register a opentracing configuration listener") {
      val mockConfigurationService = mock[ConfigurationService]
      val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

      openTracingService.init()

      verify(mockConfigurationService).subscribeTo(
        isEq("opentracing.cfg.xml"),
        any[URL](),
        isA(classOf[UpdateListener[OpenTracingConfig]]),
        isA(classOf[Class[OpenTracingConfig]]))
    }
  }

  describe("destroy") {
    it("should deregister a opentracing configuration listener") {
      val mockConfigurationService = mock[ConfigurationService]
      val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

      openTracingService.destroy()

      verify(mockConfigurationService).unsubscribeFrom(
        isEq("opentracing.cfg.xml"),
        isA(classOf[UpdateListener[OpenTracingConfig]]))
    }
  }

  describe("getGlobalTracer") {
    it("should not be enabled if service uninitialized") {
      val mockConfigurationService = mock[ConfigurationService]

      val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

      val tracer: Tracer = openTracingService.getGlobalTracer

      tracer shouldBe a[GlobalTracer]

      openTracingService.isEnabled shouldBe false
      openTracingService.getServiceName shouldBe null
    }

  }

  describe("OpenTracingConfigurationListener.configurationUpdated") {

    before {
      val globalTracer = classOf[GlobalTracer]
      val field: Field = globalTracer.getDeclaredField("tracer")
      field.setAccessible(true)
      field.set(null, NoopTracerFactory.create())
    }

    after {
      val globalTracer = classOf[GlobalTracer]
      val field: Field = globalTracer.getDeclaredField("tracer")
      field.setAccessible(true)
      field.set(null, NoopTracerFactory.create())
    }

    it("should not be enabled with invalid tracer type") {
      val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
      val mockConfigurationService = new MockConfiguration(openTracingConfig)

      val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

      openTracingService.init()

      openTracingService.isEnabled shouldBe false
      openTracingService.getServiceName shouldBe null

    }

    it("should not be enabled with invalid tracer type but service name should be set") {
      val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
      openTracingConfig.setName("test")
      val mockConfigurationService = new MockConfiguration(openTracingConfig)

      val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

      openTracingService.init()

      openTracingService.isEnabled shouldBe false
      openTracingService.getServiceName shouldBe "test"

      val tracer: Tracer = openTracingService.getGlobalTracer

      tracer shouldBe a[GlobalTracer]

      // this validates that the tracer is NOT registered successfully and is a NoopTracer
      openTracingService.isEnabled shouldBe false
      openTracingService.getServiceName shouldBe "test"

    }

    it("should be enabled with JAEGER tracer type") {
      val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
      openTracingConfig.setName("test")
      openTracingConfig.setTracer(TracerType.JAEGER)
      val mockConfigurationService = new MockConfiguration(openTracingConfig)

      val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

      openTracingService.init()

      openTracingService.getRequestInterceptor shouldBe a[JaegerRequestInterceptor]
      openTracingService.getResponseInterceptor shouldBe a[JaegerResponseInterceptor]

      val tracer: Tracer = openTracingService.getGlobalTracer

      tracer shouldBe a[GlobalTracer]

      // this validates that the tracer is registered successfully and is not a NoopTracer
      openTracingService.isEnabled shouldBe true

      openTracingService.getServiceName shouldBe "test"

    }

    it("should be enabled with JAEGER tracer type and empty sampling configuration") {
      val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
      openTracingConfig.setName("test")
      openTracingConfig.setTracer(TracerType.JAEGER)
      val samplerConfiguration: JaegerSamplingConfiguration = new JaegerSamplingConfiguration()
      openTracingConfig.setJaegerSamplingConfig(samplerConfiguration)

      val mockConfigurationService = new MockConfiguration(openTracingConfig)

      val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

      openTracingService.init()

      openTracingService.getRequestInterceptor shouldBe a[JaegerRequestInterceptor]
      openTracingService.getResponseInterceptor shouldBe a[JaegerResponseInterceptor]

      val tracer: Tracer = openTracingService.getGlobalTracer

      tracer shouldBe a[GlobalTracer]

      // this validates that the tracer is registered successfully and is not a NoopTracer
      openTracingService.isEnabled shouldBe true

      openTracingService.getServiceName shouldBe "test"

    }

    it("should be enabled with JAEGER tracer type and const sampling configuration with CONST type") {
      val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
      openTracingConfig.setName("test")
      openTracingConfig.setTracer(TracerType.JAEGER)
      val constSampleConfiguration: JaegerSamplingConst = new JaegerSamplingConst()
      constSampleConfiguration.setValue(1)

      val samplerConfiguration: JaegerSamplingConfiguration = new JaegerSamplingConfiguration()
      samplerConfiguration.setJaegerSamplingConst(constSampleConfiguration)
      samplerConfiguration.setSampleType(JaegerSampleType.CONST)

      openTracingConfig.setJaegerSamplingConfig(samplerConfiguration)

      val mockConfigurationService = new MockConfiguration(openTracingConfig)
      val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

      openTracingService.init()

      openTracingService.getRequestInterceptor shouldBe a[JaegerRequestInterceptor]
      openTracingService.getResponseInterceptor shouldBe a[JaegerResponseInterceptor]

      val tracer: Tracer = openTracingService.getGlobalTracer

      tracer shouldBe a[GlobalTracer]

      // this validates that the tracer is registered successfully and is not a NoopTracer
      openTracingService.isEnabled shouldBe true
      openTracingService.getServiceName shouldBe "test"

    }

    it("should be enabled with JAEGER tracer type and no sampling configuration with CONST type") {
      val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
      openTracingConfig.setName("test")
      openTracingConfig.setTracer(TracerType.JAEGER)

      val samplerConfiguration: JaegerSamplingConfiguration = new JaegerSamplingConfiguration()
      samplerConfiguration.setSampleType(JaegerSampleType.CONST)

      openTracingConfig.setJaegerSamplingConfig(samplerConfiguration)

      val mockConfigurationService = new MockConfiguration(openTracingConfig)
      val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

      openTracingService.init()

      openTracingService.getRequestInterceptor shouldBe a[JaegerRequestInterceptor]
      openTracingService.getResponseInterceptor shouldBe a[JaegerResponseInterceptor]

      val tracer: Tracer = openTracingService.getGlobalTracer

      tracer shouldBe a[GlobalTracer]

      // this validates that the tracer is registered successfully and is not a NoopTracer
      openTracingService.isEnabled shouldBe true
      openTracingService.getServiceName shouldBe "test"

    }

    it("should be enabled with JAEGER tracer type and probabilistic sampling configuration with PROBABILISTIC type") {
      val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
      openTracingConfig.setName("test")
      openTracingConfig.setTracer(TracerType.JAEGER)
      val probabilisticSampleConfiguration: JaegerSamplingProbabilistic = new JaegerSamplingProbabilistic()
      probabilisticSampleConfiguration.setValue(1)

      val samplerConfiguration: JaegerSamplingConfiguration = new JaegerSamplingConfiguration()
      samplerConfiguration.setJaegerSamplingProbabilistic(probabilisticSampleConfiguration)
      samplerConfiguration.setSampleType(JaegerSampleType.PROBABILISTIC)

      openTracingConfig.setJaegerSamplingConfig(samplerConfiguration)

      val mockConfigurationService = new MockConfiguration(openTracingConfig)
      val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

      openTracingService.init()

      openTracingService.getRequestInterceptor shouldBe a[JaegerRequestInterceptor]
      openTracingService.getResponseInterceptor shouldBe a[JaegerResponseInterceptor]

      val tracer: Tracer = openTracingService.getGlobalTracer

      tracer shouldBe a[GlobalTracer]

      // this validates that the tracer is registered successfully and is not a NoopTracer
      openTracingService.isEnabled shouldBe true
      openTracingService.getServiceName shouldBe "test"

    }

    it("should be enabled with JAEGER tracer type and no sampling configuration with PROBABILISTIC type") {
      val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
      openTracingConfig.setName("test")
      openTracingConfig.setTracer(TracerType.JAEGER)

      val samplerConfiguration: JaegerSamplingConfiguration = new JaegerSamplingConfiguration()
      samplerConfiguration.setSampleType(JaegerSampleType.PROBABILISTIC)

      openTracingConfig.setJaegerSamplingConfig(samplerConfiguration)

      val mockConfigurationService = new MockConfiguration(openTracingConfig)
      val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

      openTracingService.init()

      openTracingService.getRequestInterceptor shouldBe a[JaegerRequestInterceptor]
      openTracingService.getResponseInterceptor shouldBe a[JaegerResponseInterceptor]

      val tracer: Tracer = openTracingService.getGlobalTracer

      tracer shouldBe a[GlobalTracer]

      // this validates that the tracer is registered successfully and is not a NoopTracer
      openTracingService.isEnabled shouldBe true
      openTracingService.getServiceName shouldBe "test"

    }
  }

  it("should be enabled with JAEGER tracer type and rate limited sampling configuration with RATE_LIMITED type") {
    val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
    openTracingConfig.setName("test")
    openTracingConfig.setTracer(TracerType.JAEGER)
    val rateLimitingSampleConfiguration: JaegerSamplingRateLimiting = new JaegerSamplingRateLimiting()
    rateLimitingSampleConfiguration.setMaxTracesPerSecond(1)

    val samplerConfiguration: JaegerSamplingConfiguration = new JaegerSamplingConfiguration()
    samplerConfiguration.setJaegerSamplingRateLimiting(rateLimitingSampleConfiguration)
    samplerConfiguration.setSampleType(JaegerSampleType.RATE_LIMITED)

    openTracingConfig.setJaegerSamplingConfig(samplerConfiguration)

    val mockConfigurationService = new MockConfiguration(openTracingConfig)
    val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

    openTracingService.init()

    openTracingService.getRequestInterceptor shouldBe a[JaegerRequestInterceptor]
    openTracingService.getResponseInterceptor shouldBe a[JaegerResponseInterceptor]

    val tracer: Tracer = openTracingService.getGlobalTracer

    tracer shouldBe a[GlobalTracer]

    // this validates that the tracer is registered successfully and is not a NoopTracer
    openTracingService.isEnabled shouldBe true
    openTracingService.getServiceName shouldBe "test"

  }

  it("should be enabled with JAEGER tracer type and no sampling configuration with RATE_LIMITED type") {
    val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
    openTracingConfig.setName("test")
    openTracingConfig.setTracer(TracerType.JAEGER)

    val samplerConfiguration: JaegerSamplingConfiguration = new JaegerSamplingConfiguration()
    samplerConfiguration.setSampleType(JaegerSampleType.RATE_LIMITED)

    openTracingConfig.setJaegerSamplingConfig(samplerConfiguration)

    val mockConfigurationService = new MockConfiguration(openTracingConfig)
    val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

    openTracingService.init()

    openTracingService.getRequestInterceptor shouldBe a[JaegerRequestInterceptor]
    openTracingService.getResponseInterceptor shouldBe a[JaegerResponseInterceptor]

    val tracer: Tracer = openTracingService.getGlobalTracer

    tracer shouldBe a[GlobalTracer]

    // this validates that the tracer is registered successfully and is not a NoopTracer
    openTracingService.isEnabled shouldBe true
    openTracingService.getServiceName shouldBe "test"

  }

  it("should be enabled with JAEGER tracer type and HTTP sender protocol") {
    val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
    openTracingConfig.setName("test")
    openTracingConfig.setTracer(TracerType.JAEGER)

    openTracingConfig.setSenderProtocol(JaegerSenderProtocol.HTTP)

    val mockConfigurationService = new MockConfiguration(openTracingConfig)
    val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

    openTracingService.init()

    openTracingService.getRequestInterceptor shouldBe a[JaegerRequestInterceptor]
    openTracingService.getResponseInterceptor shouldBe a[JaegerResponseInterceptor]

    val tracer: Tracer = openTracingService.getGlobalTracer

    tracer shouldBe a[GlobalTracer]

    // this validates that the tracer is registered successfully and is not a NoopTracer
    openTracingService.isEnabled shouldBe true
    openTracingService.getServiceName shouldBe "test"

  }

  it("should be enabled with JAEGER tracer type, HTTP sender protocol and only username") {
    val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
    openTracingConfig.setName("test")
    openTracingConfig.setTracer(TracerType.JAEGER)

    openTracingConfig.setSenderProtocol(JaegerSenderProtocol.HTTP)
    openTracingConfig.setUsername("user")

    val mockConfigurationService = new MockConfiguration(openTracingConfig)
    val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

    openTracingService.init()

    openTracingService.getRequestInterceptor shouldBe a[JaegerRequestInterceptor]
    openTracingService.getResponseInterceptor shouldBe a[JaegerResponseInterceptor]

    val tracer: Tracer = openTracingService.getGlobalTracer

    tracer shouldBe a[GlobalTracer]

    // this validates that the tracer is registered successfully and is not a NoopTracer
    openTracingService.isEnabled shouldBe true
    openTracingService.getServiceName shouldBe "test"

  }

  it("should be enabled with JAEGER tracer type, HTTP sender protocol and only password") {
    val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
    openTracingConfig.setName("test")
    openTracingConfig.setTracer(TracerType.JAEGER)

    openTracingConfig.setSenderProtocol(JaegerSenderProtocol.HTTP)
    openTracingConfig.setPassword("abc123")

    val mockConfigurationService = new MockConfiguration(openTracingConfig)
    val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

    openTracingService.init()

    openTracingService.getRequestInterceptor shouldBe a[JaegerRequestInterceptor]
    openTracingService.getResponseInterceptor shouldBe a[JaegerResponseInterceptor]

    val tracer: Tracer = openTracingService.getGlobalTracer

    tracer shouldBe a[GlobalTracer]

    // this validates that the tracer is registered successfully and is not a NoopTracer
    openTracingService.isEnabled shouldBe true
    openTracingService.getServiceName shouldBe "test"

  }

  it("should be enabled with JAEGER tracer type, HTTP sender protocol and username + password") {
    val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
    openTracingConfig.setName("test")
    openTracingConfig.setTracer(TracerType.JAEGER)

    openTracingConfig.setSenderProtocol(JaegerSenderProtocol.HTTP)
    openTracingConfig.setPassword("abc123")
    openTracingConfig.setUsername("user")

    val mockConfigurationService = new MockConfiguration(openTracingConfig)
    val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

    openTracingService.init()

    openTracingService.getRequestInterceptor shouldBe a[JaegerRequestInterceptor]
    openTracingService.getResponseInterceptor shouldBe a[JaegerResponseInterceptor]

    val tracer: Tracer = openTracingService.getGlobalTracer

    tracer shouldBe a[GlobalTracer]

    // this validates that the tracer is registered successfully and is not a NoopTracer
    openTracingService.isEnabled shouldBe true
    openTracingService.getServiceName shouldBe "test"

  }

  it("should be enabled with JAEGER tracer type, HTTP sender protocol, username + password, token") {
    val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
    openTracingConfig.setName("test")
    openTracingConfig.setTracer(TracerType.JAEGER)

    openTracingConfig.setSenderProtocol(JaegerSenderProtocol.HTTP)
    openTracingConfig.setPassword("abc123")
    openTracingConfig.setUsername("user")
    openTracingConfig.setToken("12345683")

    val mockConfigurationService = new MockConfiguration(openTracingConfig)
    val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

    openTracingService.init()

    openTracingService.getRequestInterceptor shouldBe a[JaegerRequestInterceptor]
    openTracingService.getResponseInterceptor shouldBe a[JaegerResponseInterceptor]

    val tracer: Tracer = openTracingService.getGlobalTracer

    tracer shouldBe a[GlobalTracer]

    // this validates that the tracer is registered successfully and is not a NoopTracer
    openTracingService.isEnabled shouldBe true
    openTracingService.getServiceName shouldBe "test"

  }

  it("should be enabled with JAEGER tracer type, HTTP sender protocol, and token") {
    val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
    openTracingConfig.setName("test")
    openTracingConfig.setTracer(TracerType.JAEGER)

    openTracingConfig.setSenderProtocol(JaegerSenderProtocol.HTTP)
    openTracingConfig.setToken("12345683")

    val mockConfigurationService = new MockConfiguration(openTracingConfig)
    val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

    openTracingService.init()

    openTracingService.getRequestInterceptor shouldBe a[JaegerRequestInterceptor]
    openTracingService.getResponseInterceptor shouldBe a[JaegerResponseInterceptor]

    val tracer: Tracer = openTracingService.getGlobalTracer

    tracer shouldBe a[GlobalTracer]

    // this validates that the tracer is registered successfully and is not a NoopTracer
    openTracingService.isEnabled shouldBe true
    openTracingService.getServiceName shouldBe "test"

  }

  it("should be enabled with JAEGER tracer type and UDP sender protocol") {
    val openTracingConfig: OpenTracingConfig = new OpenTracingConfig
    openTracingConfig.setName("test")
    openTracingConfig.setTracer(TracerType.JAEGER)

    openTracingConfig.setSenderProtocol(JaegerSenderProtocol.UDP)

    val mockConfigurationService = new MockConfiguration(openTracingConfig)
    val openTracingService = new OpenTracingServiceImpl(mockConfigurationService)

    openTracingService.init()

    openTracingService.getRequestInterceptor shouldBe a[JaegerRequestInterceptor]
    openTracingService.getResponseInterceptor shouldBe a[JaegerResponseInterceptor]

    val tracer: Tracer = openTracingService.getGlobalTracer

    tracer shouldBe a[GlobalTracer]

    // this validates that the tracer is registered successfully and is not a NoopTracer
    openTracingService.isEnabled shouldBe true
    openTracingService.getServiceName shouldBe "test"

  }
}

private class MockConfiguration(openTracingConfig: OpenTracingConfig) extends ConfigurationService {

  val log = LoggerFactory.getLogger(this.getClass)
  val stupidListener: mutable.Map[String, AnyRef] = mutable.Map.empty[String, AnyRef]
  private val lock = new Object()

  def getListener[T](key: String): UpdateListener[T] = {

    //Have to block on an entry
    while (lock.synchronized {
      !stupidListener.keySet.contains(key)
    }) {
      //Set up to block for when we want to get ahold of a listener by a key
      //THis guy blocks forever, it doesn't have any timeout things :(
      Thread.sleep(10)
    }

    stupidListener(key).asInstanceOf[UpdateListener[T]]
  }

  override def subscribeTo[T](configurationName: String, listener: UpdateListener[T], configurationClass: Class[T]): Unit = ???

  override def subscribeTo[T](filterName: String, configurationName: String, listener: UpdateListener[T], configurationClass: Class[T]): Unit = ???

  //This is the only one we use I think
  override def subscribeTo[T](configurationName: String, xsdStreamSource: URL, listener: UpdateListener[T], configurationClass: Class[T]): Unit = {
    log.info(s"Subscribing to ${configurationName}")
    lock.synchronized {
      listener.configurationUpdated(configurationClass.cast(openTracingConfig))
      stupidListener(configurationName) = listener
    }
  }

  override def subscribeTo[T](filterName: String, configurationName: String, xsdStreamSource: URL, listener: UpdateListener[T], configurationClass: Class[T]): Unit = ???

  override def subscribeTo[T](filterName: String, configurationName: String, listener: UpdateListener[T], customParser: ConfigurationParser[T]): Unit = ???

  override def subscribeTo[T](filterName: String, configurationName: String, listener: UpdateListener[T], customParser: ConfigurationParser[T], sendNotificationNow: Boolean): Unit = ???

  override def unsubscribeFrom(configurationName: String, plistener: UpdateListener[_]): Unit = {
    stupidListener.remove(configurationName) //Drop it from our stuff
  }

  override def getResourceResolver: ConfigurationResourceResolver = ???

  override def destroy(): Unit = ???
}

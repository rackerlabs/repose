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

import javax.annotation.{PostConstruct, PreDestroy}
import javax.inject.{Inject, Named}

import com.typesafe.scalalogging.StrictLogging
import io.jaegertracing.Configuration
import io.jaegertracing.Configuration.{SamplerConfiguration, SenderConfiguration}
import io.jaegertracing.internal.samplers.{ConstSampler, ProbabilisticSampler, RateLimitingSampler}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.opentracing.DelegatingTracer
import org.openrepose.core.service.opentracing.config._
import org.openrepose.core.services.config.ConfigurationService

/**
  * A service that enables support for OpenTracing compliant tracing in Repose.
  *
  * Currently, this service only configures the Open Tracing {@link io.opentracing.Tracer}
  * to report trace data. As such, it does not expose any methods.
  */
@Named
class OpenTracingServiceImpl @Inject()(configurationService: ConfigurationService, reposeTracer: DelegatingTracer)
  extends UpdateListener[OpenTracingConfig] with StrictLogging {

  import OpenTracingServiceImpl._

  private var initialized: Boolean = false

  @PostConstruct
  def init(): Unit = {
    logger.info("Initializing Open Tracing Service")
    val xsdURL = getClass.getResource("/META-INF/schema/config/open-tracing.xsd")

    configurationService.subscribeTo(
      DefaultConfig,
      xsdURL,
      this,
      classOf[OpenTracingConfig])
  }

  @PreDestroy
  def destroy(): Unit = {
    logger.info("Unsubscribing configuration listener and shutting down service")
    configurationService.unsubscribeFrom(DefaultConfig, this)
  }

  override def configurationUpdated(openTracingConfig: OpenTracingConfig): Unit = {
    logger.debug("Open Tracing Service configuration updated")

    openTracingConfig.getTracerConfig match {
      case jaeger: JaegerTracerConfig =>
        logger.debug("Jaeger tracer configured")

        val samplerConfiguration = getJaegerSamplerConfiguration(jaeger)
        val senderConfiguration = getJaegerSenderConfiguration(jaeger)
        val reporterConfiguration = new Configuration.ReporterConfiguration().withLogSpans(jaeger.isLogSpans).withFlushInterval(jaeger.getFlushIntervalMs).withMaxQueueSize(jaeger.getMaxBufferSize).withSender(senderConfiguration)
        val configuration = (new Configuration(openTracingConfig.getServiceName)).withSampler(samplerConfiguration)
          .withReporter(reporterConfiguration)

        logger.debug("Registering the tracer with global tracer")
        reposeTracer.register(configuration.getTracer())
      case _ =>
        logger.error("Unsupported tracer specified")
    }

    initialized = true
  }

  override def isInitialized: Boolean = initialized

  def getJaegerSamplerConfiguration(jaegerConfig: JaegerTracerConfig): SamplerConfiguration = {
    jaegerConfig.getJaegerSampling match {
      case constant: JaegerSamplingConstant =>
        logger.debug("Constant sampling configured with value set to {}", constant.getToggle)
        new Configuration.SamplerConfiguration().withType(ConstSampler.TYPE).withParam(if (Toggle.ON.equals(constant.getToggle)) 1 else 0)
      case rateLimiting: JaegerSamplingRateLimiting =>
        logger.debug("Rate limiting sampling configured with value set to {} samples per second", rateLimiting.getMaxTracesPerSecond.asInstanceOf[AnyRef])
        new Configuration.SamplerConfiguration().withType(RateLimitingSampler.TYPE).withParam(rateLimiting.getMaxTracesPerSecond)
      case probabilistic: JaegerSamplingProbabilistic =>
        logger.debug("Probabilistic sampling configured with value set to {}", probabilistic.getProbability.asInstanceOf[AnyRef])
        new Configuration.SamplerConfiguration().withType(ProbabilisticSampler.TYPE).withParam(probabilistic.getProbability)
    }
  }

  def getJaegerSenderConfiguration(jaegerConfig: JaegerTracerConfig): SenderConfiguration = {
    jaegerConfig.getJaegerConnection match {
      case udp: JaegerConnectionUdp =>
        logger.debug("UDP sender configured")
        new SenderConfiguration().withAgentHost(udp.getHost).withAgentPort(udp.getPort)
      case http: JaegerConnectionHttp =>
        if (Option(http.getToken).isDefined) {
          logger.debug("HTTP sender configured with bearer token")
          new SenderConfiguration().withAuthToken(http.getToken).withEndpoint(http.getEndpoint)
        } else if (Option(http.getUsername).isDefined) {
          logger.debug("HTTP sender configured with basic auth")
          new SenderConfiguration().withAuthUsername(http.getUsername).withAuthPassword(http.getPassword).withEndpoint(http.getEndpoint)
        } else {
          logger.debug("HTTP sender configured without authentication")
          new SenderConfiguration().withEndpoint(http.getEndpoint)
        }
    }
  }
}

object OpenTracingServiceImpl {
  private final val DefaultConfig = "open-tracing.cfg.xml"
}

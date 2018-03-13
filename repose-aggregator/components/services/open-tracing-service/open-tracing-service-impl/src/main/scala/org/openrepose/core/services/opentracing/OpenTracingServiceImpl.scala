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

import com.typesafe.scalalogging.slf4j.LazyLogging
import com.uber.jaeger.Configuration
import com.uber.jaeger.Configuration.{SamplerConfiguration, SenderConfiguration}
import io.opentracing.Tracer
import io.opentracing.util.GlobalTracer
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.service.opentracing.config.{JaegerSampleType, JaegerSenderProtocol, OpenTracingConfig, TracerType}
import org.openrepose.core.services.config.ConfigurationService

@Named
class OpenTracingServiceImpl @Inject()(configurationService: ConfigurationService)
  extends OpenTracingService with LazyLogging {

  import OpenTracingServiceImpl._


  private var isServiceEnabled: Boolean = false
  private var serviceName: String = _

  @PostConstruct
  def init(): Unit = {
    logger.info("Initializing OpenTracingService")
    val xsdURL = getClass.getResource("/META-INF/schema/config/opentracing.xsd")

    configurationService.subscribeTo(
      DefaultConfig,
      xsdURL,
      OpenTracingConfigurationListener,
      classOf[OpenTracingConfig])
  }


  @PreDestroy
  def destroy(): Unit = {
    logger.info("Unregistering configuration listeners and shutting down service")
    configurationService.unsubscribeFrom(DefaultConfig, OpenTracingConfigurationListener)
  }

  /**
    * Check that the service is enabled.  The tracing will only happen if this is enabled.
    * While configuration might set the service as enabled, there are a couple instances when this might
    * get turned back to disabled:
    *
    * * Invalid tracer specified
    * * Unable to connect to tracer
    *
    * @return a Boolean which corresponds to the clientId parameter
    */
  override def isEnabled: Boolean = this.isServiceEnabled

  /**
    * Retrieves the global tracer singleton.  This is configured at startup via opentracing.cfg.xml tracer
    * specific configuration.  If an invalid tracer is provided, the tracer will not be available.
    *
    * @return io.opentracing.Tracer object that contains Tracer implementation information
    */
  override def getGlobalTracer: Tracer = {
    logger.debug("Retrieve global tracer.")
    try {
      if (!OpenTracingConfigurationListener.isInitialized) throw new IllegalStateException("The OpenTracingService has not yet been initialized")
      if (!GlobalTracer.isRegistered) {
        logger.error("Opentracing configuration is missing.  " +
          "Check that you have opentracing.cfg.xml properly configured with one of the tracers registered")
        logger.trace("If we don't disable it, we would through an NPE wherever the tracer is called.  Don't do that.")
        this.isServiceEnabled = false
      }
    } catch {
      case ise: IllegalStateException =>
        logger.error("Opentracing was not initialized.  We will turn this off.  Check the logs for the issue. " +
          "For example, an invalid tracer host/port", ise)
        this.isServiceEnabled = false
    }

    GlobalTracer.get
  }

  /**
    * Retrieves service name.  This is specific for every repose implementation and defines the namespace
    * for your service.  It should be unique in your company/flow.
    *
    * @return String object that contains your service name
    */
  override def getServiceName: String = this.serviceName

  private object OpenTracingConfigurationListener extends UpdateListener[OpenTracingConfig] {
    private var initialized = false

    override def configurationUpdated(openTracingConfig: OpenTracingConfig): Unit = {
      synchronized {
        logger.trace("OpenTracingService configuration updated")
        initialized = true

        logger.trace("get the tracer from configuration")

        serviceName = openTracingConfig.getName
        isServiceEnabled = openTracingConfig.isEnabled

        if (openTracingConfig.isEnabled) openTracingConfig.getTracer match {
          case TracerType.JAEGER =>
            logger.debug("register Jaeger tracer")
            logger.debug("figure out which sampler we want!")
            val samplerConfiguration: SamplerConfiguration = getJaegerSamplerConfiguration(openTracingConfig)
            val senderConfiguration: SenderConfiguration = getJaegerSenderConfiguration(openTracingConfig)
            val configuration = new Configuration(openTracingConfig.getName, samplerConfiguration, new Configuration.ReporterConfiguration(openTracingConfig.isLogSpans, openTracingConfig.getFlushIntervalMs, openTracingConfig.getMaxBufferSize, senderConfiguration))
            logger.debug("register the tracer with global tracer")
            GlobalTracer.register(configuration.getTracer)
          case _ =>
            logger.error("Invalid tracer specified.  Problem with opentracing.xsd enumeration")
            isServiceEnabled = false
        }

        def getJaegerSamplerConfiguration(openTracingConfig: OpenTracingConfig): SamplerConfiguration = {
          var samplerConfiguration: SamplerConfiguration = null
          if (openTracingConfig.getJaegerSamplingConfig == null) {
            logger.trace("no sampling configuration configured.  Default to send everything!")
            samplerConfiguration = new Configuration.SamplerConfiguration("const", 1)
          }
          else openTracingConfig.getJaegerSamplingConfig.getSampleType match {
            case JaegerSampleType.CONST =>
              logger.trace("constant sampling configuration configured.")
              if (openTracingConfig.getJaegerSamplingConfig.getJaegerSamplingConst != null) {
                logger.trace(s"Sampling value set to ${openTracingConfig.getJaegerSamplingConfig.getJaegerSamplingConst.getValue}")
                samplerConfiguration = new Configuration.SamplerConfiguration(
                  "const", openTracingConfig.getJaegerSamplingConfig.getJaegerSamplingConst.getValue)
              }
              else {
                logger.error("no const sampling configuration configured.  Default to send everything!")
                samplerConfiguration = new Configuration.SamplerConfiguration("const", 1)
              }
            case JaegerSampleType.RATE_LIMITED =>
              logger.trace("rate limiting sampling configuration configured.")
              if (openTracingConfig.getJaegerSamplingConfig.getJaegerSamplingRateLimiting != null) {
                logger.trace(s"Rate limited to ${openTracingConfig.getJaegerSamplingConfig.getJaegerSamplingRateLimiting.getMaxTracesPerSecond} " +
                  s"samples per second!")
                samplerConfiguration = new Configuration.SamplerConfiguration("ratelimiting",
                  openTracingConfig.getJaegerSamplingConfig.getJaegerSamplingRateLimiting.getMaxTracesPerSecond)
              }
              else {
                logger.error("no rate-limited sampling configuration configured.  Default to send everything!")
                samplerConfiguration = new Configuration.SamplerConfiguration("const", 1)
              }
            case JaegerSampleType.PROBABILISTIC =>
              logger.trace("probabilistic sampling configuration configured.")
              if (openTracingConfig.getJaegerSamplingConfig.getJaegerSamplingProbabilistic != null) {
                logger.trace(s"Probability set to ${openTracingConfig.getJaegerSamplingConfig.getJaegerSamplingProbabilistic.getValue}")
                samplerConfiguration = new Configuration.SamplerConfiguration("probabilistic",
                  openTracingConfig.getJaegerSamplingConfig.getJaegerSamplingProbabilistic.getValue)
              }
              else {
                logger.error("no probabilistic sampling configuration configured.  Default to send everything!")
                samplerConfiguration = new Configuration.SamplerConfiguration("const", 1)
              }
            case _ =>
              // default to const and always on
              logger.error("invalid sampling configuration configured.  Default to send everything!")
              samplerConfiguration = new Configuration.SamplerConfiguration("const", 1)
          }
          samplerConfiguration
        }

        def getJaegerSenderConfiguration(openTracingConfig: OpenTracingConfig): SenderConfiguration = {
          var senderConfiguration: SenderConfiguration = null

          openTracingConfig.getSenderProtocol match {
            case JaegerSenderProtocol.UDP =>
              logger.trace("set udp sender")
              senderConfiguration = new SenderConfiguration.Builder().agentHost(openTracingConfig.getAgentHost).agentPort(openTracingConfig.getAgentPort).build
            case JaegerSenderProtocol.HTTP =>
              logger.trace("Check if username and password are provided")
              if (!Option(openTracingConfig.getUsername).getOrElse("").isEmpty &&
                !Option(openTracingConfig.getPassword).getOrElse("").isEmpty) {
                logger.trace("set http sender with BasicAuth headers")
                senderConfiguration = new SenderConfiguration.Builder().authUsername(openTracingConfig.getUsername).authPassword(openTracingConfig.getPassword).endpoint(openTracingConfig.getCollectorEndpoint).build
              }
              else if (!Option(openTracingConfig.getToken).getOrElse("").isEmpty) {
                logger.trace("set http sender with Bearer token")
                senderConfiguration = new SenderConfiguration.Builder().authToken(openTracingConfig.getToken).endpoint(openTracingConfig.getCollectorEndpoint).build
              }
              else {
                logger.trace("set http sender without authentication")
                senderConfiguration = new SenderConfiguration.Builder().endpoint(openTracingConfig.getCollectorEndpoint).build
              }
            case _ =>
              logger.error("invalid protocol in sender configuration configured.  Default to http sender!")
              senderConfiguration = new SenderConfiguration.Builder().endpoint(openTracingConfig.getCollectorEndpoint).build
          }

          senderConfiguration
        }

      }
    }

    override def isInitialized: Boolean = initialized
  }
}


object OpenTracingServiceImpl {
  final val ServiceName = "opentracing"

  private final val DefaultConfig = ServiceName + ".cfg.xml"
}

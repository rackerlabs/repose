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
import org.openrepose.core.service.opentracing.config._
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

        serviceName = openTracingConfig.getServiceName

        val tracerConfig = Option(openTracingConfig.getJaeger)
        tracerConfig match {
          case Some(jaegerConfig) =>
            logger.debug("register Jaeger tracer")
            logger.debug("figure out which sampler we want!")
            val samplerConfiguration: SamplerConfiguration = getJaegerSamplerConfiguration(jaegerConfig)
            val senderConfiguration: SenderConfiguration = getJaegerSenderConfiguration(jaegerConfig)
            val configuration = new Configuration(openTracingConfig.getServiceName, samplerConfiguration, new Configuration.ReporterConfiguration(jaegerConfig.isLogSpans, jaegerConfig.getFlushIntervalMs, jaegerConfig.getMaxBufferSize, senderConfiguration))
            logger.debug("register the tracer with global tracer")
            GlobalTracer.register(configuration.getTracer)
          case _ =>
            logger.error("Invalid tracer specified.  Problem with opentracing.xsd enumeration")
            isServiceEnabled = false
        }

        def getJaegerSamplerConfiguration(jaegerConfig: JaegerTracerConfiguration): SamplerConfiguration = {
          val samplingConfig = Option[JaegerSampling](jaegerConfig.getSamplingConstant).orElse(Option(jaegerConfig.getSamplingProbabilistic)).orElse(Option(jaegerConfig.getSamplingRateLimiting))
          samplingConfig match {
            case Some(config: JaegerSamplingConstant) =>
              logger.trace("constant sampling configuration configured.")
              logger.trace(s"Sampling value set to ${config.getToggle}")
              new Configuration.SamplerConfiguration("const", (if (Toggle.ON.equals(config.getToggle)) 1 else 0))
            case Some(config: JaegerSamplingRateLimiting) =>
              logger.trace("rate limiting sampling configuration configured.")
              logger.trace(s"Rate limited to ${config.getMaxTracesPerSecond} samples per second!")
              new Configuration.SamplerConfiguration("ratelimiting", config.getMaxTracesPerSecond)
            case Some(config: JaegerSamplingProbabilistic) =>
              logger.trace("probabilistic sampling configuration configured.")
              logger.trace(s"Probability set to ${config.getProbability}")
              new Configuration.SamplerConfiguration("probabilistic", config.getProbability)
          }
        }

        def getJaegerSenderConfiguration(jaegerConfig: JaegerTracerConfiguration): SenderConfiguration = {
          val connectionConfig = Option[JaegerConnection](jaegerConfig.getConnectionHttp).orElse(Option(jaegerConfig.getConnectionUdp))
          connectionConfig match {
            case Some(config: JaegerConnectionUdp) =>
              logger.trace("set udp sender")
              new SenderConfiguration.Builder().agentHost(config.getHost).agentPort(config.getPort).build
            case Some(config: JaegerConnectionHttp) =>
              logger.trace("Check if username and password are provided")
              (Option(config.getToken), Option(config.getUsername)) match {
                case (Some(_), _) =>
                  logger.trace("set http sender with Bearer token")
                  new SenderConfiguration.Builder().authToken(config.getToken).endpoint(s"${config.getHost}:${config.getPort}").build
                case (_, Some(_)) =>
                  logger.trace("set http sender with BasicAuth headers")
                  new SenderConfiguration.Builder().authUsername(config.getUsername).authPassword(config.getPassword).endpoint(s"${config.getHost}:${config.getPort}").build
                case (_, _) =>
                  logger.trace("set http sender without authentication")
                  new SenderConfiguration.Builder().endpoint(s"${config.getHost}:${config.getPort}").build
              }
          }
        }

      }
    }

    override def isInitialized: Boolean = initialized
  }
}


object OpenTracingServiceImpl {

  private final val DefaultConfig = "open-tracing.cfg.xml"
}

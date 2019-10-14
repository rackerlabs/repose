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
package org.openrepose.core.services.httpclient

import com.typesafe.scalalogging.StrictLogging
import javax.annotation.{PostConstruct, PreDestroy}
import javax.inject.{Inject, Named}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.healthcheck.{HealthCheckService, HealthCheckServiceProxy, Severity}
import org.openrepose.core.services.httpclient.HttpClientServiceImpl._
import org.openrepose.core.services.httpclient.config.HttpConnectionPoolsConfig

import scala.Function.tupled
import scala.collection.JavaConverters._

@Named
class HttpClientServiceImpl @Inject()(configurationService: ConfigurationService,
                                      healthCheckService: HealthCheckService,
                                      httpClientProvider: HttpClientProvider,
                                      httpClientDecommissioner: HttpClientDecommissioner)
  extends HttpClientService with InternalHttpClientService with StrictLogging {

  private var defaultHttpClientId: String = _
  private var httpClients: Map[String, InternalHttpClient] = Map.empty

  private var healthCheckServiceProxy: HealthCheckServiceProxy = _

  @PostConstruct
  def init(): Unit = {
    logger.debug("Initializing HttpClientService")

    healthCheckServiceProxy = healthCheckService.register()
    healthCheckServiceProxy.reportIssue(HealthCheckConfigurationIssue, HealthCheckConfigurationMessage, Severity.BROKEN)

    val defaultConfigSchemaUrl = getClass.getResource(DefaultConfigSchema)
    configurationService.subscribeTo(
      DefaultConfig,
      defaultConfigSchemaUrl,
      ConfigurationListener,
      classOf[HttpConnectionPoolsConfig])

    logger.debug("Initialized HttpClientService")
  }

  @PreDestroy
  def destroy(): Unit = {
    logger.debug("Destroying HttpClientService")

    configurationService.unsubscribeFrom(DefaultConfig, ConfigurationListener)
    httpClients.values.foreach(httpClientDecommissioner.decommissionClient)

    logger.debug("Destroyed HttpClientService")
  }

  override def getDefaultClient: HttpClientServiceClient = {
    getClient(null)
  }

  override def getClient(clientId: String): HttpClientServiceClient = {
    new HttpClientServiceClient(this, httpClientDecommissioner, clientId)
  }

  override def getInternalClient(clientId: String): InternalHttpClient = {
    verifyInitialized()

    Option(clientId) match {
      case Some(id) if httpClients.contains(id) =>
        httpClients(id)
      case Some(id) =>
        logger.warn("Client {} not available -- returning the default client", id)
        httpClients(defaultHttpClientId)
      case None =>
        httpClients(defaultHttpClientId)
    }
  }

  private def verifyInitialized(): Unit = {
    if (!ConfigurationListener.isInitialized) {
      throw new IllegalStateException(s"$ServiceName has not yet been initialized")
    }
  }

  private object ConfigurationListener extends UpdateListener[HttpConnectionPoolsConfig] {
    private var initialized: Boolean = false

    override def configurationUpdated(configurationObject: HttpConnectionPoolsConfig): Unit = {
      val oldHttpClients = httpClients

      httpClients = configurationObject.getPool.asScala.map { clientConfig =>
        val newClient = httpClientProvider.createClient(clientConfig)
        if (clientConfig.isDefault) {
          defaultHttpClientId = clientConfig.getId
        }
        clientConfig.getId -> newClient
      }.toMap

      // Decommissioning must occur after available clients are updated to avoid concurrency issues
      // where a client being decommissioned is returned to a user of this service.
      oldHttpClients.values.foreach(httpClientDecommissioner.decommissionClient)

      initialized = true
      healthCheckServiceProxy.resolveIssue(HealthCheckConfigurationIssue)
    }

    override def isInitialized: Boolean = {
      initialized
    }
  }

}

object HttpClientServiceImpl {
  final val ServiceName: String = "http-client-service"
  final val DefaultConfig: String = "http-connection-pool.cfg.xml"
  final val DefaultConfigSchema: String = "/META-INF/schema/config/http-connection-pool.xsd"
  final val HealthCheckConfigurationIssue: String = "HttpClientServiceReport"
  final val HealthCheckConfigurationMessage: String = "Http Client Service Configuration Error"
}

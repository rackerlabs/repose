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
package org.openrepose.core.services.phonehome.impl

import javax.annotation.PostConstruct
import javax.inject.{Inject, Named}
import javax.ws.rs.core.MediaType

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.core.systemmodel.{PhoneHomeService => PhoneHomeServiceConfig, SystemModel}
import org.springframework.beans.factory.annotation.Value
import play.api.libs.json.Json

import scala.collection.JavaConverters._

@Named
class PhoneHomeServiceImpl @Inject()(@Value(ReposeSpringProperties.CORE.REPOSE_VERSION) reposeVer: String,
                                     @Value(ReposeSpringProperties.CORE.CONFIG_ROOT) confDir: String,
                                     configurationService: ConfigurationService,
                                     akkaServiceClient: AkkaServiceClient)
  extends PhoneHomeService with LazyLogging {

  private var enabled: Boolean = false
  private var systemModel: SystemModel = _

  @PostConstruct
  def init(): Unit = {
    logger.debug("Registering system model listener")
    configurationService.subscribeTo(
      "system-model.cfg.xml",
      SystemModelConfigurationListener,
      classOf[SystemModel]
    )
  }

  override def isEnabled: Boolean = {
    logger.trace("isEnabled method called")

    ifInitialized(enabled)
  }

  override def sendUpdate(): Unit = {
    logger.trace("sendUpdate method called")

    ifInitialized {
      val staticSystemModel = systemModel // Pin the system model in case an update occurs while processing

      Option(staticSystemModel.getPhoneHome) match {
        case Some(phoneHome) if phoneHome.isEnabled =>
          logger.debug("Sending usage data update to data collection point")

          sendUpdateMessage(phoneHome.getCollectionUri, buildUpdateMessage(staticSystemModel, phoneHome))
        case _ =>
          logger.debug("Could not send an update; the phone home service is not enabled")
          //TODO: Should this throw an exception to programmatically inform a user that this call should not be made?
          throw new IllegalStateException("Could not send an update; the phone home service is not enabled")
      }
    }
  }

  private def ifInitialized[T](f: => T): T = {
    if (SystemModelConfigurationListener.isInitialized) {
      f
    } else {
      logger.error("The phone home service has not yet initialized")
      throw new IllegalStateException("The phone home service has not yet initialized")
    }
  }

  private def buildUpdateMessage(systemModel: SystemModel, phoneHome: PhoneHomeServiceConfig): String = {
    logger.trace("buildUpdateMessage method called")

    //TODO: Define implicit writes for filters and services
    Json.stringify(Json.obj(
      "serviceId" -> phoneHome.getOriginServiceId,
      "contactEmail" -> phoneHome.getContactEmail,
      "reposeVersion" -> reposeVer,
      "clusters" -> systemModel.getReposeCluster.asScala.map(cluster =>
        Json.obj(
          "filters" -> cluster.getFilters.getFilter.asScala.map(filter => filter.getName),
          "services" -> cluster.getServices.getService.asScala.map(service => service.getName)
        )
      )
    ))
  }

  private def sendUpdateMessage(collectionUri: String, message: String): Unit = {
    logger.trace("sendUpdateMessage method called")

    try {
      //TODO: Add x-trans-id header
      //TODO: Log failed status code responses
      akkaServiceClient.post(
        "phone-home-update",
        collectionUri,
        Map.empty[String, String].asJava,
        message,
        MediaType.APPLICATION_JSON_TYPE)
    } catch {
      case e: Exception => logger.error("Could not send an update to the collection service", e)
    }
  }

  object SystemModelConfigurationListener extends UpdateListener[SystemModel] {
    private var initialized = false

    override def configurationUpdated(configurationObject: SystemModel): Unit = {
      systemModel = configurationObject
      enabled = Option(configurationObject.getPhoneHome) match {
        case Some(phoneHome) => phoneHome.isEnabled
        case None => false
      }

      initialized = true

      if (isEnabled) sendUpdate()
    }

    override def isInitialized: Boolean = initialized
  }

}

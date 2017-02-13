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

import java.text.SimpleDateFormat
import java.util.{TimeZone, Date, UUID}
import javax.annotation.PostConstruct
import javax.inject.{Inject, Named}
import javax.ws.rs.core.MediaType

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClientFactory, AkkaServiceClient}
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.core.systemmodel.{FilterList, PhoneHomeServiceConfig, ServicesList, SystemModel}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{JsNull, JsValue, Json, Writes}

import scala.collection.JavaConverters._

/**
 * A service which sends Repose usage data to a data collection point. This data may be used to determine common usage
 * patterns.
 * <p/>
 * Data that may be sent includes:
 * - Filter chain
 * - Services
 * - Contact information
 * <p/>
 * Note that the lifecycle of this service is self-managed, and the configuration for this service is read directly
 * from the system model configuration file. Additionally, usage data will be sent whenever an update to the system
 * model is observed, as long as the phone home service is active.
 */
@Named
class PhoneHomeService @Inject()(@Value(ReposeSpringProperties.CORE.REPOSE_VERSION) reposeVer: String,
                                 configurationService: ConfigurationService,
                                 akkaServiceClientFactory: AkkaServiceClientFactory)
  extends LazyLogging {

  private final val msgLogger = LoggerFactory.getLogger("phone-home-message")
  private final val defaultCollectionUri = new PhoneHomeServiceConfig().getCollectionUri

  private var systemModel: SystemModel = _
  private var akkaServiceClient: AkkaServiceClient = _

  @PostConstruct
  def init(): Unit = {
    logger.debug("Registering system model listener")
    configurationService.subscribeTo(
      "system-model.cfg.xml",
      SystemModelConfigurationListener,
      classOf[SystemModel]
    )
    akkaServiceClient = akkaServiceClientFactory.newAkkaServiceClient()
  }

  private def sendUpdate(): Unit = {
    logger.trace("sendUpdate method called")

    val staticSystemModel = systemModel // Pin the system model in case an update occurs while processing

    Option(staticSystemModel.getPhoneHome) match {
      case Some(phoneHome) if phoneHome.isEnabled =>
        logger.debug("Sending usage data update to data collection service")

        sendUpdateMessage(
          phoneHome.getCollectionUri,
          buildUpdateMessage(phoneHome.getOriginServiceId, phoneHome.getContactEmail)
        )
      case Some(phoneHome) if !phoneHome.isEnabled =>
        val updateMessage = buildUpdateMessage(phoneHome.getOriginServiceId, phoneHome.getContactEmail)
        logger.warn(buildLogMessage(
          "Did not attempt to send usage data on update -- the phone home service is not enabled",
          updateMessage,
          phoneHome.getCollectionUri
        ))
      case _ =>
        val updateMessage = buildUpdateMessage()
        logger.warn(buildLogMessage(
          "Did not attempt to send usage data on update -- the phone home service is not enabled",
          updateMessage,
          defaultCollectionUri
        ))
    }

    def buildLogMessage(reason: String, updateMessage: String, collectionUri: String): String = {
      s"""$reason. Check the output location of the 'phone-home-message' logger to view the message. To manually send
        | the message to the data collection service, use the following command:
        | curl -d '$updateMessage' -H 'Content-Type: application/json' $collectionUri
        |""".stripMargin.replaceAll("[\n\r]", "")
    }

    def buildUpdateMessage(originServiceId: JsValueWrapper = JsNull, contactEmail: JsValueWrapper = JsNull): String = {
      logger.trace("buildUpdateMessage method called")

      implicit val filtersWrites = new Writes[FilterList] {
        override def writes(filterList: FilterList): JsValue = {
          Option(filterList) map { fltrList =>
            Json.toJson(fltrList.getFilter.asScala.map(filter => filter.getName))
          } getOrElse JsNull
        }
      }

      implicit val servicesWrites = new Writes[ServicesList] {
        override def writes(servicesList: ServicesList): JsValue = {
          Option(servicesList) map { svcList =>
            Json.toJson(svcList.getService.asScala.map(service => service.getName))
          } getOrElse JsNull
        }
      }

      def getCurrentFormattedDateTime(dateTime: Long): String = {
        val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
        simpleDateFormat.format(new Date(dateTime))
      }

      val currentTimeMillis = System.currentTimeMillis

      val updateMessage = Json.stringify(Json.obj(
        "serviceId" -> originServiceId,
        "createdAt" -> getCurrentFormattedDateTime(currentTimeMillis),
        "createdAtMillis" -> currentTimeMillis,
        "jreVersion" -> System.getProperty("java.version", "UNKNOWN"),
        "jvmName" -> System.getProperty("java.vm.name", "UNKNOWN"),
        "contactEmail" -> contactEmail,
        "reposeVersion" -> reposeVer,
        "clusters" -> staticSystemModel.getReposeCluster.asScala.map(cluster =>
          Json.obj(
            "filters" -> cluster.getFilters,
            "services" -> cluster.getServices
          )
        )
      ))

      msgLogger.info(updateMessage)

      updateMessage
    }

    def sendUpdateMessage(collectionUri: String, message: String): Unit = {
      logger.trace("sendUpdateMessage method called")

      try {
        val updateHeaders = if (Option(staticSystemModel.getTracingHeader).isEmpty || staticSystemModel.getTracingHeader.isEnabled) {
          Map(CommonHttpHeader.TRACE_GUID -> UUID.randomUUID().toString).asJava
        } else {
          Map.empty[String, String].asJava
        }

        val akkaResponse = akkaServiceClient.post(
          "phone-home-update",
          collectionUri,
          updateHeaders,
          message,
          MediaType.APPLICATION_JSON_TYPE
        )

        // Handle error status codes
        if (akkaResponse.getStatus < 200 || akkaResponse.getStatus > 299) {
          logger.error(buildLogMessage(
            s"""Update to the collection service failed with status code ${akkaResponse.getStatus}""",
            message,
            collectionUri
          ))
        }
      } catch {
        case e: Exception => logger.error("Could not send an update to the collection service", e)
      }
    }
  }

  object SystemModelConfigurationListener extends UpdateListener[SystemModel] {
    private var initialized = false

    override def configurationUpdated(configurationObject: SystemModel): Unit = {
      systemModel = configurationObject
      initialized = true

      sendUpdate()
    }

    override def isInitialized: Boolean = initialized
  }

}

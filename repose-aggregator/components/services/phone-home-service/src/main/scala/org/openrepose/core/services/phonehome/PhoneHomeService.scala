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
import java.util.{Date, TimeZone, UUID}

import com.typesafe.scalalogging.StrictLogging
import io.opentracing.tag.Tags
import io.opentracing.{Scope, Tracer}
import javax.annotation.PostConstruct
import javax.inject.{Inject, Named}
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.opentracing.ReposeTags
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.httpclient.{CachingHttpClientContext, HttpClientService, HttpClientServiceClient}
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.core.systemmodel.config.{FilterList, PhoneHomeServiceConfig, ServicesList, SystemModel}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{JsNull, JsValue, Json, Writes}

import scala.Function.tupled
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
                                 tracer: Tracer,
                                 configurationService: ConfigurationService,
                                 httpClientService: HttpClientService)
  extends StrictLogging {

  import PhoneHomeService._

  private var systemModel: SystemModel = _
  private var httpClient: HttpClientServiceClient = _

  @PostConstruct
  def init(): Unit = {
    logger.debug("Registering system model listener")
    httpClient = httpClientService.getDefaultClient
    configurationService.subscribeTo(
      "system-model.cfg.xml",
      SystemModelConfigurationListener,
      classOf[SystemModel]
    )
  }

  private def sendUpdate(scope: Scope): Unit = {
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
          DefaultCollectionUri
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
        "filters" -> staticSystemModel.getFilters,
        "services" -> staticSystemModel.getServices
      ))

      MsgLogger.info(updateMessage)

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

        val requestBody = EntityBuilder.create()
          .setText(message)
          .setContentType(ContentType.APPLICATION_JSON)
          .build()
        val requestBuilder = RequestBuilder.post(collectionUri)
          .setEntity(requestBody)
        updateHeaders.asScala.foreach(tupled(requestBuilder.addHeader))
        val request = requestBuilder.build()

        val cachingContext = CachingHttpClientContext.create()
          .setCacheKey("phone-home-update")

        val response = httpClient.execute(request, cachingContext)

        try {
          // Handle error status codes
          if (response.getStatusLine.getStatusCode < 200 || response.getStatusLine.getStatusCode > 299) {
            logger.error(buildLogMessage(
              s"""Update to the collection service failed with status code ${response.getStatusLine.getStatusCode}""",
              message,
              collectionUri
            ))
          }
        } finally {
          EntityUtils.consumeQuietly(response.getEntity)
        }
      } catch {
        case e: Exception =>
          logger.error("Could not send an update to the collection service", e)
          scope.span.setTag(Tags.ERROR.getKey, true)
      }
    }
  }

  object SystemModelConfigurationListener extends UpdateListener[SystemModel] {
    private var initialized = false

    override def configurationUpdated(configurationObject: SystemModel): Unit = {
      val span = tracer.buildSpan(TracingOperationName).ignoreActiveSpan().start()
      val scope = tracer.activateSpan(span)
      scope.span().setTag(ReposeTags.ReposeVersion, reposeVer)

      try {
        systemModel = configurationObject
        initialized = true

        sendUpdate(scope)
      } catch {
        case e: Exception => scope.span.setTag(Tags.ERROR.getKey, true)
      } finally {
        scope.close()
      }
    }

    override def isInitialized: Boolean = initialized
  }

}

object PhoneHomeService {
  private final val TracingOperationName = "phone_home"
  private final val MsgLogger = LoggerFactory.getLogger("phone-home-message")
  private final val DefaultCollectionUri = new PhoneHomeServiceConfig().getCollectionUri
}

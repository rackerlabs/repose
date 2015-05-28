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
package org.openrepose.filters.Keystonev2

import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import javax.ws.rs.core.MediaType

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.{ServiceClientResponse, CommonHttpHeader}
import org.openrepose.commons.utils.servlet.http.{MutableHttpServletResponse, MutableHttpServletRequest}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.keystonev2.config.{CacheTimeoutsType, CacheSettingsType, KeystoneV2Config}

import scala.annotation.tailrec
import scala.io.Source
import scala.util.{Failure, Success, Try}

@Named
class KeystoneV2Filter @Inject()(configurationService: ConfigurationService,
                                 akkaServiceClient: AkkaServiceClient,
                                 datastoreService: DatastoreService)
  extends Filter
  with UpdateListener[KeystoneV2Config]
  with HttpDelegationManager
  with LazyLogging {

  private val DEFAULT_CONFIG = "keystone-v2.cfg.xml"
  var configurationFile: String = DEFAULT_CONFIG
  var configuration: KeystoneV2Config = _
  var initialized = false

  override def init(filterConfig: FilterConfig): Unit = {
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info(s"Initializing Keystone V2 Filter using config $configurationFile")
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/keystone-v2.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      configurationFile,
      xsdURL,
      this,
      classOf[KeystoneV2Config]
    )
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(configurationFile, this)
  }

  implicit val autoHeaderToString: CommonHttpHeader => String = { chh =>
    chh.toString
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain): Unit = {
    val request = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
    val response = MutableHttpServletResponse.wrap(servletRequest.asInstanceOf[HttpServletRequest], servletResponse.asInstanceOf[HttpServletResponse])

    val authTokenValue = Option(request.getHeader(CommonHttpHeader.AUTH_TOKEN))

    authTokenValue.map { authToken =>
      validateToken(authToken) match {
        case Success(x) => {
          if (!x) {
            response.setStatus(403) //NOPE
          } else {
            chain.doFilter(request, response)
          }
        }
        case Failure(x) => {
          response.setStatus(500) //TODO: this isn't complete!
        }
      }
    } getOrElse {
      response.setStatus(403)
    }
  }

  @tailrec
  final def validateToken(token: String, doRetry: Boolean = true): Try[Boolean] = {
    requestAdminToken match {
      case Success(adminToken) => {
        val identityEndpoint = configuration.getIdentityService.getUri

        import scala.collection.JavaConverters._
        Try(akkaServiceClient.get(token,
          identityEndpoint,
          Map(CommonHttpHeader.AUTH_TOKEN.toString -> adminToken).asJava)
        ) match {
          case Success(serviceClientResponse) => {
            //DEAL WITH IT
            //Parse the response for validating a token?
            logger.debug(s"SERVICE CLIENT RESPONSE: ${serviceClientResponse.getStatus}")
            logger.debug(s"Admin Token: ${adminToken}")
            serviceClientResponse.getStatus match {
              case 200 | 203 => {
                Success(true)
              }
              case 400 => Failure(new Exception("Bad Token Validation request to identity!")) //500 class
              case 401 | 403 => {
                if (doRetry) {
                  //Clear the cache, call this method again
                  validateToken(token, doRetry = false)
                } else {
                  Failure(new Exception("Admin user is not authorized to validate tokens"))
                }
              }
              case 404 => Success(false)
              case 503 => Failure(new Exception("Identity Service not avaialable to authenticate token")) //502
              case _ => Failure(new Exception("Unhandled response from Identity, unable to continue")) //502
            }
          }
          case Failure(x) => Failure(new Exception("Communication error with Identity to validate a token", x))
        }
      }
      case Failure(x) => Failure(new Exception("Unable to acquire admin token", x))
    }
  }

  def requestAdminToken: Try[String] = {
    //authenticate, or get the admin token
    val identityEndpoint = configuration.getIdentityService.getUri

    import play.api.libs.json._
    val adminUsername = configuration.getIdentityService.getUsername
    val adminPassword = configuration.getIdentityService.getPassword

    val authenticationPayload = Json.obj(
      "auth" -> Json.obj(
        "passwordCredentials" -> Json.obj(
          "username" -> adminUsername,
          "password" -> adminPassword
        )
      )
    )

    import scala.collection.JavaConversions._
    val akkaResponse = Try(akkaServiceClient.post("v2AdminTokenAuthentication",
      identityEndpoint,
      Map.empty[String, String],
      Json.stringify(authenticationPayload),
      MediaType.APPLICATION_JSON_TYPE
    ))

    akkaResponse match {
      case Success(x) => {
        val jsonResponse = Source.fromInputStream(x.getData).getLines().mkString("")
        val json = Json.parse(jsonResponse)
        Try(Success((json \ "access" \ "token" \ "id").as[String])) match {
          case Success(s) => s
          case Failure(f) => Failure(new Exception("Token not found in identity response", f))
        }
      }
      case Failure(x) => Failure(new Exception("Failure communicating with identity", x))
    }
  }

  override def configurationUpdated(configurationObject: KeystoneV2Config): Unit = {
    def fixMyDefaults(stupidConfig: KeystoneV2Config): KeystoneV2Config = {
      // LOLJAXB  	(╯°□°）╯︵ ┻━┻
      //This relies on the Default Settings plugin and the fluent_api plugin added to the Jaxb code generation plugin
      // I'm sorry
      if (stupidConfig.getCacheSettings == null) {
        stupidConfig.withCacheSettings(new CacheSettingsType().withTimeouts(new CacheTimeoutsType()))
      } else if (stupidConfig.getCacheSettings.getTimeouts == null) {
        stupidConfig.getCacheSettings.withTimeouts(new CacheTimeoutsType())
        stupidConfig
      } else {
        stupidConfig
      }
    }

    configuration = fixMyDefaults(configurationObject)
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}

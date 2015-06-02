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

import java.io.InputStream
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import javax.ws.rs.core.MediaType

import com.fasterxml.jackson.core.JsonProcessingException
import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.{PowerApiHeader, ServiceClientResponse, CommonHttpHeader}
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
  val ADMIN_TOKEN_KEY = "KEYSTONE-V2-ADMIN-TOKEN" //NOTE when using the self-validating we probably won't cache anything?

  var configurationFile: String = DEFAULT_CONFIG
  var configuration: KeystoneV2Config = _
  var initialized = false

  val datastore = datastoreService.getDefaultDatastore

  //Which happens to be the local datastore

  trait IdentityExceptions

  case class IdentityAdminTokenException(message: String, cause: Throwable = null) extends Exception(message, cause) with IdentityExceptions

  case class AdminTokenUnauthorizedException(message: String, cause: Throwable = null) extends Exception(message, cause) with IdentityExceptions

  case class IdentityValidationException(message: String, cause: Throwable = null) extends Exception(message, cause) with IdentityExceptions

  case class IdentityCommuncationException(message: String, cause: Throwable = null) extends Exception(message, cause) with IdentityExceptions

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

  sealed trait KeystoneV2Result

  case class Pass(headersToAdd: Map[String, String]) extends KeystoneV2Result

  case class Reject(status: Int, message: Option[String] = None, failure: Option[Throwable] = None) extends KeystoneV2Result


  /**
   * Get the user's endpoints, and validate that the configured restrictions match
   * @param result
   * @return
   */
  def performAuthorization(result: KeystoneV2Result): KeystoneV2Result = {
    result match {
      case x: Pass => ???
      //Get the endpoints, and see if they've got their endpoint configured
      case a: Reject => a
    }
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain): Unit = {
    val request = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
    val response = MutableHttpServletResponse.wrap(servletRequest.asInstanceOf[HttpServletRequest], servletResponse.asInstanceOf[HttpServletResponse])

    val authTokenValue = Option(request.getHeader(CommonHttpHeader.AUTH_TOKEN))
    //Pull in the status codes, because I'm using them a bunch
    import HttpServletResponse._

    //Get the authenticating token!
    val result: KeystoneV2Result = authTokenValue.map { authToken =>
      getAdminToken match {
        case Success(authenticatingToken) =>
          validateToken(authenticatingToken, authToken).recoverWith {
            //Recover if the exception is an AdminTokenUnauthorizedException
            //This way we can specify however we want to what we want to do to retry.
            //Also it only retries ONCE! No loops or anything. Fails gloriously
            case unauth: AdminTokenUnauthorizedException =>
              //Clear the cache, call this method again
              datastore.remove(ADMIN_TOKEN_KEY)
              getAdminToken match {
                case Success(newAdminToken) =>
                  validateToken(newAdminToken, authToken)
                case Failure(x) => Failure(IdentityAdminTokenException("Unable to reaquire admin token", x))
              }
          } match {
            case Success(InvalidToken) => {
              Reject(SC_FORBIDDEN)
            }
            case Success(validToken: ValidToken) => {
              val groupsHeader = Map(PowerApiHeader.GROUPS.toString -> validToken.groups.mkString(","))
              Pass(groupsHeader)
            }
            case Failure(x: IdentityAdminTokenException) => {
              Reject(SC_INTERNAL_SERVER_ERROR, failure = Some(x))
            }
            case Failure(x: IdentityCommuncationException) => {
              Reject(SC_BAD_GATEWAY, failure = Some(x))
            }
            case Failure(x) =>
              //TODO: this isn't yet complete
              Reject(SC_INTERNAL_SERVER_ERROR, failure = Some(x))
          }
        case Failure(x) =>
          Reject(SC_BAD_GATEWAY, Some("Unable to acquire admin token"), Some(x))
      }
    } getOrElse {
      Reject(SC_FORBIDDEN, Some("Token did not validate"))
    }

    //TODO: working on this
    if (false) {
      val result2 = performAuthorization(result)
    }

    result match {
      case rejection: Reject =>
        val message: Option[String] = rejection match {
          case Reject(_, Some(x), _) => Some(x)
          case Reject(code, None, Some(failure)) => {
            logger.debug(s"Rejecting with status $code", failure)
            Some(failure.getMessage)
          }
          case _ => None
        }
        message.map { m =>
          logger.debug(s"Rejection message: $m")
          response.sendError(rejection.status, m)
        } getOrElse {
          response.sendError(rejection.status)
        }

      case p: Pass =>
        //If the token validation passed and we're configured to do more things, we can do additional authorizations


        //Modify the request to add stuff
        p.headersToAdd.foreach { case (k, v) =>
          request.addHeader(k, v)
        }
        chain.doFilter(request, response)
    }
  }

  //Token Validation stuffs
  sealed trait TokenValidationResult

  case class ValidToken(groups: Seq[String]) extends TokenValidationResult

  case object InvalidToken extends TokenValidationResult

  final def validateToken(authenticatingToken: String, token: String): Try[TokenValidationResult] = {
    /**
     * Extract the user's information from the validate token response
     * @param inputStream the validate token response!
     * @return a success or failure of ValidToken information
     */
    def extractUserInformation(inputStream: InputStream): Try[ValidToken] = {
      import play.api.libs.json.Reads._
      import play.api.libs.json._

      val input: String = Source.fromInputStream(inputStream).getLines mkString ""
      try {
        val json = Json.parse(input)
        //Have to convert it to a vector, because List isn't serializeable in 2.10
        val roleNames: Seq[String] = (json \ "access" \ "user" \ "roles" \\ "name").map(_.as[String]).toVector
        val validToken = ValidToken(roleNames)
        datastore.put(token, validToken, configuration.getCacheSettings.getTimeouts.getToken, TimeUnit.SECONDS)
        Success(validToken)
      } catch {
        case oops@(_: JsResultException | _: JsonProcessingException) =>
          Failure(new IdentityCommuncationException("Unable to parse JSON from identity validate token response", oops))
      }
    }

    //TODO: pass in the configuration as well, because state change
    val identityEndpoint = configuration.getIdentityService.getUri

    import scala.collection.JavaConverters._
    Try(akkaServiceClient.get(token,
      identityEndpoint,
      Map(CommonHttpHeader.AUTH_TOKEN.toString -> authenticatingToken).asJava)
    ) match {
      case Success(serviceClientResponse) => {
        //DEAL WITH IT
        //Parse the response for validating a token?
        logger.debug(s"SERVICE CLIENT RESPONSE: ${serviceClientResponse.getStatus}")
        logger.debug(s"Admin Token: $authenticatingToken")
        serviceClientResponse.getStatus match {
          //TODO: magic numbers?
          case 200 | 203 =>
            //Extract the groups from the JSON and stick it in the ValidToken result
            extractUserInformation(serviceClientResponse.getData)
          case 400 => Failure(IdentityValidationException("Bad Token Validation request to identity!"))
          case 401 | 403 => Failure(AdminTokenUnauthorizedException("Unable to validate token, authenticating token unauthorized"))
          case 404 => {
            datastore.put(token, InvalidToken, configuration.getCacheSettings.getTimeouts.getToken, TimeUnit.SECONDS)
            Success(InvalidToken)
          }
          case 503 => Failure(IdentityValidationException("Identity Service not available to authenticate token"))
          case _ => Failure(IdentityCommuncationException("Unhandled response from Identity, unable to continue"))
        }
      }
      case Failure(x) => Failure(IdentityCommuncationException("Unable to successfully validate token with Identity", x))
    }
  }


  /**
   * Check the cache, or call to identity to get the admin token
   * @return Returns a Successful token, or a Failure
   */
  def getAdminToken: Try[String] = {
    //Check the cache first, then try the request
    Option(datastore.get(ADMIN_TOKEN_KEY)).map { value =>
      Success(value.asInstanceOf[String])
    } getOrElse {
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
            case Success(s) =>
              datastore.put(ADMIN_TOKEN_KEY, s.get, configuration.getCacheSettings.getTimeouts.getToken, TimeUnit.SECONDS)
              s
            case Failure(f) => Failure(IdentityCommuncationException("Token not found in identity response during Admin Authentication", f))
          }
        }
        case Failure(x) => Failure(IdentityCommuncationException("Failure communicating with identity during Admin Authentication", x))
      }
    }
  }

  //TODO: put the endpoint related stuff into an object we can call
  case class Endpoint(region: String, name: String, endpointType: String, publicURL: String)

  @tailrec
  final def requestEndpointsForToken(forToken: String, doRetry: Boolean = true): Try[List[Endpoint]] = {
    val identityEndpoint = configuration.getIdentityService.getUri

    import scala.collection.JavaConverters._

    val adminToken = ""
    Try(akkaServiceClient.get(s"${forToken}Endpoints",
      s"$identityEndpoint/tokens/$forToken/endpoints",
      Map(CommonHttpHeader.AUTH_TOKEN.toString -> adminToken).asJava)) match {
      case Success(serviceClientResponse) =>
        serviceClientResponse.getStatus match {
          case 200 | 203 => ??? //TODO: extract endpoints and return
          case 401 | 403 =>
            //TODO: use the recoverWith logic here also SRSLY
            if (doRetry) {
              requestEndpointsForToken(forToken, doRetry = false)
            } else {
              Failure(IdentityAdminTokenException(s"Admin user is not authorized to get endpoints for token $forToken"))
            }
        }
      case Failure(x) => Failure(x)
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
